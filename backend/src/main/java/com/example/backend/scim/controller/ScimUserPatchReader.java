package com.example.backend.scim.controller;

import com.example.backend.scim.domain.ScimEmail;
import com.example.backend.scim.domain.ScimEmailFilter;
import com.example.backend.scim.domain.ScimEmailPart;
import com.example.backend.scim.domain.ScimName;
import com.example.backend.scim.domain.ScimRequestLimits;
import com.example.backend.scim.domain.ScimUserPatchOperation;
import com.example.backend.scim.domain.ScimUserPatchOperation.EmailUpdate;
import com.example.backend.scim.domain.ScimUserPatchOperation.NamePart;
import com.example.backend.scim.domain.ScimUserPatchOperation.TextAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads a SCIM User {@code PatchOp} body into the operations it means.
 *
 * <p>This is where every refusal that depends only on the REQUEST is made, so the operations that
 * reach the use case are all well-formed and only the refusals that depend on the stored User
 * remain for it ({@code noTarget}, removing a required value):
 *
 * <ul>
 *   <li>a body that is not a {@code PatchOp}, or has no operations — {@code invalidSyntax};
 *   <li>a {@code path} that does not parse, names an attribute this service does not implement,
 *       or puts a filter or a sub-attribute where the attribute has none — {@code invalidPath};
 *   <li>a value filter using anything but {@code eq} comparisons joined by {@code and} —
 *       {@code invalidFilter}, because the full filter grammar is not implemented yet and an
 *       approximated filter would select values the client did not ask for;
 *   <li>a path naming a read-only attribute ({@code id}, {@code meta}, {@code groups}), or
 *       {@code externalId}, which is fixed at creation — {@code mutability};
 *   <li>a {@code remove} with no path — {@code noTarget}, which RFC 7644 §3.5.2.2 names for it;
 *   <li>a value of the wrong JSON type for its target — {@code invalidValue}.
 * </ul>
 *
 * <p>Paths are matched case-insensitively and may carry the core User schema URN as a prefix, both
 * of which RFC 7644 §3.10 and §3.5.2 allow. No message produced here echoes a submitted value; a
 * path is echoed only after {@link ScimUserRequestReader#sanitized}.
 */
final class ScimUserPatchReader {

    private static final String SCHEMA_PREFIX = ScimSchemas.USER.toLowerCase(Locale.ROOT) + ":";

    /** {@code attr}, {@code attr.sub}, {@code attr[filter]}, {@code attr[filter].sub}. */
    private static final Pattern PATH = Pattern.compile(
            "([A-Za-z][A-Za-z0-9_$-]*)(?:\\[(.+)])?(?:\\.([A-Za-z][A-Za-z0-9_$-]*))?");

    /** One {@code sub op value} comparison inside a value filter. */
    private static final Pattern COMPARISON =
            Pattern.compile("\\s*([A-Za-z][A-Za-z0-9_$-]*)\\s+([A-Za-z]+)\\s+(.+?)\\s*");

    private static final Set<String> READ_ONLY = Set.of("id", "meta", "groups", "schemas");

    /**
     * Fixed at creation, as a Group's is: a PATCH naming it is refused as {@code mutability} rather
     * than applied, and a path-less value carrying it — a resource sent back as read — ignores it.
     */
    private static final String EXTERNAL_ID = "externalid";

    private static final Map<String, TextAttribute> TEXT = Map.of(
            "username", TextAttribute.USER_NAME,
            "displayname", TextAttribute.DISPLAY_NAME,
            "preferredlanguage", TextAttribute.PREFERRED_LANGUAGE,
            "locale", TextAttribute.LOCALE,
            "timezone", TextAttribute.TIMEZONE);

    private static final Map<String, NamePart> NAME_PARTS = Map.of(
            "formatted", NamePart.FORMATTED,
            "familyname", NamePart.FAMILY_NAME,
            "givenname", NamePart.GIVEN_NAME,
            "middlename", NamePart.MIDDLE_NAME,
            "honorificprefix", NamePart.HONORIFIC_PREFIX,
            "honorificsuffix", NamePart.HONORIFIC_SUFFIX);

    private static final Map<String, ScimEmailPart> EMAIL_PARTS = Map.of(
            "value", ScimEmailPart.VALUE,
            "type", ScimEmailPart.TYPE,
            "primary", ScimEmailPart.PRIMARY);

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private enum Op { ADD, REMOVE, REPLACE }

    /** A parsed path: the attribute, and the filter and sub-attribute when it has them. */
    private record Path(String attribute, String filter, String subAttribute) {
    }

    private ScimUserPatchReader() {
    }

    /**
     * The operations this body describes, in order.
     *
     * @throws ScimErrorException {@code 400} for every request-only refusal listed on the class
     */
    static List<ScimUserPatchOperation> readPatch(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw ScimErrorException.invalidSyntax("The request body must be a SCIM PatchOp.");
        }
        JsonNode schemas = body.get("schemas");
        if (schemas == null || !schemas.isArray() || schemas.size() != 1
                || !ScimSchemas.PATCH_OP.equals(schemas.get(0).asText())) {
            throw ScimErrorException.invalidSyntax(
                    "A PATCH body must declare exactly the schema " + ScimSchemas.PATCH_OP);
        }
        JsonNode operations = body.get("Operations");
        if (operations == null || !operations.isArray() || operations.isEmpty()) {
            throw ScimErrorException.invalidSyntax(
                    "A PatchOp must carry a non-empty Operations array.");
        }
        if (operations.size() > ScimRequestLimits.MAX_PATCH_OPERATIONS) {
            throw ScimErrorException.invalidValue("A PatchOp may carry at most "
                    + ScimRequestLimits.MAX_PATCH_OPERATIONS + " operations.");
        }
        List<ScimUserPatchOperation> read = new ArrayList<>();
        for (JsonNode operation : operations) {
            if (!operation.isObject()) {
                throw ScimErrorException.invalidSyntax("Each PATCH operation must be an object.");
            }
            read.addAll(readOperation(operation));
        }
        return List.copyOf(read);
    }

    private static List<ScimUserPatchOperation> readOperation(JsonNode operation) {
        Op op = op(operation.get("op"));
        JsonNode pathNode = operation.get("path");
        JsonNode value = operation.get("value");
        if (pathNode == null || pathNode.isNull()) {
            return pathless(op, value);
        }
        if (!pathNode.isString()) {
            throw ScimErrorException.invalidPath("path must be a string.");
        }
        return List.of(target(op, parse(pathNode.stringValue()), value));
    }

    private static Op op(JsonNode op) {
        if (op == null || !op.isString()) {
            throw ScimErrorException.invalidSyntax("Each PATCH operation requires an op.");
        }
        return switch (op.stringValue().toLowerCase(Locale.ROOT)) {
            case "add" -> Op.ADD;
            case "remove" -> Op.REMOVE;
            case "replace" -> Op.REPLACE;
            default -> throw ScimErrorException.invalidSyntax(
                    "op must be add, remove or replace.");
        };
    }

    /**
     * An operation with no path: its value is an object of attributes, each applied as if it had
     * been the operation's path. {@code remove} needs a path, so a path-less one is
     * {@code noTarget}. Read-only attributes in the value are ignored, as they are in a PUT body,
     * so a client may send back what it read.
     */
    private static List<ScimUserPatchOperation> pathless(Op op, JsonNode value) {
        if (op == Op.REMOVE) {
            throw ScimErrorException.noTarget("A remove operation requires a path.");
        }
        if (value == null || !value.isObject()) {
            throw ScimErrorException.invalidValue(
                    "An operation without a path requires an object value.");
        }
        List<ScimUserPatchOperation> read = new ArrayList<>();
        for (String attribute : value.propertyNames()) {
            String lower = attribute.toLowerCase(Locale.ROOT);
            if (READ_ONLY.contains(lower) || EXTERNAL_ID.equals(lower)) {
                continue;
            }
            if (!PATH.matcher(attribute).matches() || attribute.contains(".")
                    || attribute.contains("[")) {
                throw ScimErrorException.invalidPath(
                        "Not an attribute name: " + ScimUserRequestReader.sanitized(attribute));
            }
            read.add(target(op, new Path(attribute.toLowerCase(Locale.ROOT), null, null),
                    value.get(attribute)));
        }
        return read;
    }

    private static Path parse(String rawPath) {
        String path = rawPath.strip();
        if (path.toLowerCase(Locale.ROOT).startsWith(SCHEMA_PREFIX)) {
            path = path.substring(SCHEMA_PREFIX.length());
        }
        Matcher matcher = PATH.matcher(path);
        if (!matcher.matches()) {
            throw ScimErrorException.invalidPath(
                    "Not a valid attribute path: " + ScimUserRequestReader.sanitized(rawPath));
        }
        String sub = matcher.group(3);
        return new Path(
                matcher.group(1).toLowerCase(Locale.ROOT),
                matcher.group(2),
                sub == null ? null : sub.toLowerCase(Locale.ROOT));
    }

    private static ScimUserPatchOperation target(Op op, Path path, JsonNode value) {
        String attribute = path.attribute();
        if (READ_ONLY.contains(attribute)) {
            throw ScimErrorException.mutability(attribute + " is read-only.");
        }
        if (EXTERNAL_ID.equals(attribute)) {
            throw ScimErrorException.mutability(
                    "externalId is set when the User is created and is not changed afterwards.");
        }
        if (path.filter() != null && !attribute.equals("emails")) {
            throw ScimErrorException.invalidPath("Only emails accepts a value filter.");
        }
        if (path.subAttribute() != null && !attribute.equals("name")
                && !attribute.equals("emails")) {
            throw ScimErrorException.invalidPath(attribute + " has no sub-attributes.");
        }
        TextAttribute text = TEXT.get(attribute);
        if (text != null) {
            return op == Op.REMOVE
                    ? new ScimUserPatchOperation.RemoveText(text)
                    : new ScimUserPatchOperation.SetText(text, stringValue(value, attribute));
        }
        return switch (attribute) {
            case "active" -> op == Op.REMOVE
                    ? new ScimUserPatchOperation.RemoveActive()
                    : new ScimUserPatchOperation.SetActive(booleanValue(value, "active"));
            case "password" -> op == Op.REMOVE
                    ? new ScimUserPatchOperation.RemovePassword()
                    : new ScimUserPatchOperation.SetPassword(stringValue(value, "password"));
            case "name" -> name(op, path.subAttribute(), value);
            case "emails" -> emails(op, path, value);
            default -> throw ScimErrorException.invalidPath(
                    "This service does not implement the User attribute: "
                            + ScimUserRequestReader.sanitized(attribute));
        };
    }

    private static ScimUserPatchOperation name(Op op, String sub, JsonNode value) {
        if (sub == null) {
            if (op == Op.REMOVE) {
                return new ScimUserPatchOperation.RemoveName();
            }
            if (value == null || !value.isObject()) {
                throw ScimErrorException.invalidValue("name must be a complex value.");
            }
            return new ScimUserPatchOperation.MergeName(ScimUserRequestReader.readName(value));
        }
        NamePart part = NAME_PARTS.get(sub);
        if (part == null) {
            throw ScimErrorException.invalidPath("name has no such sub-attribute.");
        }
        if (op == Op.REMOVE) {
            return new ScimUserPatchOperation.RemoveNamePart(part);
        }
        String given = stringValue(value, "name." + sub);
        return new ScimUserPatchOperation.MergeName(new ScimName(
                part == NamePart.FORMATTED ? given : null,
                part == NamePart.FAMILY_NAME ? given : null,
                part == NamePart.GIVEN_NAME ? given : null,
                part == NamePart.MIDDLE_NAME ? given : null,
                part == NamePart.HONORIFIC_PREFIX ? given : null,
                part == NamePart.HONORIFIC_SUFFIX ? given : null));
    }

    private static ScimUserPatchOperation emails(Op op, Path path, JsonNode value) {
        ScimEmailFilter filter =
                path.filter() == null ? ScimEmailFilter.ALL : filter(path.filter());
        if (path.subAttribute() != null) {
            ScimEmailPart part = EMAIL_PARTS.get(path.subAttribute());
            if (part == null) {
                throw ScimErrorException.invalidPath("emails has no such sub-attribute.");
            }
            if (op == Op.REMOVE) {
                return new ScimUserPatchOperation.RemoveEmailPart(filter, part);
            }
            return new ScimUserPatchOperation.UpdateEmails(filter, switch (part) {
                case VALUE -> new EmailUpdate(stringValue(value, "emails.value"), null, null);
                case TYPE -> new EmailUpdate(null, stringValue(value, "emails.type"), null);
                case PRIMARY -> new EmailUpdate(null, null, booleanValue(value, "emails.primary"));
            });
        }
        if (op == Op.REMOVE) {
            return new ScimUserPatchOperation.RemoveEmails(filter);
        }
        if (path.filter() != null) {
            return new ScimUserPatchOperation.UpdateEmails(filter, emailUpdate(value));
        }
        if (value == null || !value.isArray()) {
            throw ScimErrorException.invalidValue("emails must be an array.");
        }
        List<ScimEmail> emails = ScimUserRequestReader.readEmails(value);
        return op == Op.ADD
                ? new ScimUserPatchOperation.AddEmails(emails)
                : new ScimUserPatchOperation.ReplaceEmails(emails);
    }

    /** The sub-attributes an object value writes onto filtered emails; unnamed ones are kept. */
    private static EmailUpdate emailUpdate(JsonNode value) {
        if (value == null || !value.isObject()) {
            throw ScimErrorException.invalidValue(
                    "A filtered emails operation requires an object value.");
        }
        for (String sub : value.propertyNames()) {
            if (!EMAIL_PARTS.containsKey(sub.toLowerCase(Locale.ROOT))) {
                throw ScimErrorException.invalidValue(
                        "This service does not implement the emails sub-attribute: "
                                + ScimUserRequestReader.sanitized(sub));
            }
        }
        JsonNode primary = value.get("primary");
        return new EmailUpdate(
                value.has("value") ? stringValue(value.get("value"), "emails.value") : null,
                value.has("type") ? stringValue(value.get("type"), "emails.type") : null,
                primary == null ? null : booleanValue(primary, "emails.primary"));
    }

    /**
     * A value filter: {@code eq} comparisons on email sub-attributes, joined by {@code and}.
     *
     * <p>Split on {@code and} only outside quoted strings, so a compared value containing the word
     * is not mistaken for a conjunction. Grouping is refused here; {@code or}, {@code not} and every
     * other operator are refused as {@code invalidFilter} by the comparison and literal parsing
     * below — an {@code or} leaves the text after it trailing the compared value, which is then not
     * one JSON literal — rather than approximated.
     */
    private static ScimEmailFilter filter(String text) {
        List<ScimEmailFilter.Condition> conditions = new ArrayList<>();
        for (String comparison : conjuncts(text)) {
            Matcher matcher = COMPARISON.matcher(comparison);
            if (!matcher.matches()) {
                throw ScimErrorException.invalidFilter(
                        "Only 'sub-attribute eq value' comparisons joined by 'and' are supported"
                                + " in an emails filter.");
            }
            ScimEmailPart part = EMAIL_PARTS.get(matcher.group(1).toLowerCase(Locale.ROOT));
            if (part == null) {
                throw ScimErrorException.invalidFilter("emails has no such sub-attribute.");
            }
            if (!matcher.group(2).equalsIgnoreCase("eq")) {
                throw ScimErrorException.invalidFilter(
                        "Only the eq operator is supported in an emails filter.");
            }
            conditions.add(new ScimEmailFilter.Condition(part, literal(part, matcher.group(3))));
        }
        return new ScimEmailFilter(conditions);
    }

    /** The filter's comparisons, split at each top-level {@code and}; refuses what is unsupported. */
    private static List<String> conjuncts(String text) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (quoted) {
                current.append(character);
                if (character == '\\' && index + 1 < text.length()) {
                    current.append(text.charAt(++index));
                } else if (character == '"') {
                    quoted = false;
                }
                continue;
            }
            if (character == '"') {
                quoted = true;
            } else if (character == '(' || character == ')' || character == '[') {
                throw ScimErrorException.invalidFilter(
                        "Grouping is not supported in an emails filter.");
            } else if (Character.isWhitespace(character) && keywordAt(text, index + 1, "and")) {
                parts.add(current.toString());
                current.setLength(0);
                index += "and".length();
                continue;
            }
            current.append(character);
        }
        if (quoted) {
            throw ScimErrorException.invalidPath("The emails filter has an unterminated string.");
        }
        parts.add(current.toString());
        return parts;
    }

    /** Whether {@code keyword} stands alone at this index, followed by whitespace. */
    private static boolean keywordAt(String text, int index, String keyword) {
        int end = index + keyword.length();
        return end < text.length()
                && text.regionMatches(true, index, keyword, 0, keyword.length())
                && Character.isWhitespace(text.charAt(end));
    }

    /** A comparison's value, which must be a JSON literal of the sub-attribute's type. */
    private static Object literal(ScimEmailPart part, String text) {
        JsonNode literal;
        try {
            literal = JSON.readTree(text);
        } catch (RuntimeException notJson) {
            throw ScimErrorException.invalidFilter(
                    "A compared value must be a JSON string, boolean or null.");
        }
        if (part == ScimEmailPart.PRIMARY) {
            if (!literal.isBoolean()) {
                throw ScimErrorException.invalidFilter("primary compares with true or false.");
            }
            return literal.booleanValue();
        }
        if (literal.isNull()) {
            return null;
        }
        if (!literal.isString()) {
            throw ScimErrorException.invalidFilter(
                    "value and type compare with a quoted string.");
        }
        return literal.stringValue();
    }

    private static String stringValue(JsonNode value, String attribute) {
        if (value == null || value.isNull()) {
            throw ScimErrorException.invalidValue("This operation requires a value for "
                    + attribute + ".");
        }
        return ScimUserRequestReader.string(value, attribute);
    }

    private static boolean booleanValue(JsonNode value, String attribute) {
        if (value == null || !value.isBoolean()) {
            throw ScimErrorException.invalidValue(attribute + " must be a boolean.");
        }
        return value.booleanValue();
    }
}
