package com.example.backend.scim.controller;

import com.example.backend.scim.controller.ScimPatchPathGrammar.Path;
import com.example.backend.scim.domain.ScimAttribute;
import com.example.backend.scim.domain.ScimEmail;
import com.example.backend.scim.domain.ScimEmailFilter;
import com.example.backend.scim.domain.ScimEmailPart;
import com.example.backend.scim.domain.ScimName;
import com.example.backend.scim.domain.ScimRequestLimits;
import com.example.backend.scim.domain.ScimResourceSchema;
import com.example.backend.scim.domain.ScimResourceType;
import com.example.backend.scim.domain.ScimUserPatchOperation;
import com.example.backend.scim.domain.ScimUserPatchOperation.EmailUpdate;
import com.example.backend.scim.domain.ScimUserPatchOperation.NamePart;
import com.example.backend.scim.domain.ScimUserPatchOperation.TextAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import tools.jackson.databind.JsonNode;

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
 *   <li>a path naming a read-only attribute ({@code id}, {@code meta}, {@code groups}) —
 *       {@code mutability};
 *   <li>a {@code remove} with no path — {@code noTarget}, which RFC 7644 §3.5.2.2 names for it;
 *   <li>a value of the wrong JSON type for its target — {@code invalidValue}.
 * </ul>
 *
 * <p>Paths are matched case-insensitively and may carry the core User schema URN as a prefix, both
 * of which RFC 7644 §3.10 and §3.5.2 allow. The text of a path and of its value filter is parsed
 * by {@link ScimPatchPathGrammar}, whose refusals pass through unchanged; this reader owns the
 * {@code PatchOp} envelope and decides which edit a parsed path becomes. No message produced here
 * echoes a submitted value; a path is echoed only after {@link ScimUserRequestReader#sanitized}.
 */
final class ScimUserPatchReader {

    /**
     * Which attributes exist, which are read-only, and which are complex or multi-valued: the User
     * schema's facts, which classify a path before this reader decides what its operation does.
     * Nothing below restates them; the maps that follow say only which edit a supported attribute
     * becomes.
     */
    private static final ScimResourceSchema SCHEMA = ScimResourceSchema.of(ScimResourceType.USER);

    /** The {@code name} attribute, whose sub-attributes a sub-path names. */
    private static final ScimAttribute NAME = SCHEMA.find("name").orElseThrow();

    /**
     * The calling connector's alias: read-write, as the schema declares it. {@code add} and
     * {@code replace} set it and {@code remove} clears it; the use case writes it under the calling
     * connector alone, so no PATCH can reach another connector's alias.
     */
    private static final String EXTERNAL_ID = "externalId";

    /** The single-valued strings, each with the edit that sets or clears it. */
    private static final Map<String, TextAttribute> TEXT = Map.of(
            "userName", TextAttribute.USER_NAME,
            "displayName", TextAttribute.DISPLAY_NAME,
            "preferredLanguage", TextAttribute.PREFERRED_LANGUAGE,
            "locale", TextAttribute.LOCALE,
            "timezone", TextAttribute.TIMEZONE);

    /** {@code name}'s sub-attributes, by canonical name, as the part each edits. */
    private static final Map<String, NamePart> NAME_PARTS = Map.of(
            "formatted", NamePart.FORMATTED,
            "familyName", NamePart.FAMILY_NAME,
            "givenName", NamePart.GIVEN_NAME,
            "middleName", NamePart.MIDDLE_NAME,
            "honorificPrefix", NamePart.HONORIFIC_PREFIX,
            "honorificSuffix", NamePart.HONORIFIC_SUFFIX);

    private enum Op { ADD, REMOVE, REPLACE }

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
        return List.of(target(op, ScimPatchPathGrammar.path(pathNode.stringValue()), value));
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
            if (isReadOnly(SCHEMA.find(attribute))) {
                continue;
            }
            if (!ScimPatchPathGrammar.isAttributeName(attribute)) {
                throw ScimErrorException.invalidPath(
                        "Not an attribute name: " + ScimUserRequestReader.sanitized(attribute));
            }
            read.add(target(op, new Path(attribute, null, null), value.get(attribute)));
        }
        return read;
    }

    /**
     * The operation a path names, after the schema has classified it.
     *
     * <p>The order of the refusals is the order a client can act on: a read-only attribute is
     * {@code mutability} whatever else the path says about it; a value filter on an attribute that
     * is not multi-valued, or a sub-path on one with no sub-attributes, is {@code invalidPath}; and
     * only then is an attribute this schema does not have refused as unimplemented.
     */
    private static ScimUserPatchOperation target(Op op, Path path, JsonNode value) {
        String attribute = path.attribute();
        Optional<ScimAttribute> declared = SCHEMA.find(attribute);
        if (isReadOnly(declared)) {
            throw ScimErrorException.mutability(declared.get().name() + " is read-only.");
        }
        if (path.filter() != null && !declared.map(ScimAttribute::multiValued).orElse(false)) {
            throw ScimErrorException.invalidPath(
                    "Only a multi-valued attribute accepts a value filter.");
        }
        if (path.subAttribute() != null
                && declared.map(found -> found.subAttributes().isEmpty()).orElse(true)) {
            throw ScimErrorException.invalidPath(attribute + " has no sub-attributes.");
        }
        String name = declared
                .map(ScimAttribute::name)
                .orElseThrow(() -> ScimErrorException.invalidPath(
                        "This service does not implement the User attribute: "
                                + ScimUserRequestReader.sanitized(attribute)));
        if (EXTERNAL_ID.equals(name)) {
            return op == Op.REMOVE
                    ? new ScimUserPatchOperation.RemoveExternalId()
                    : new ScimUserPatchOperation.SetExternalId(stringValue(value, EXTERNAL_ID));
        }
        TextAttribute text = TEXT.get(name);
        if (text != null) {
            return op == Op.REMOVE
                    ? new ScimUserPatchOperation.RemoveText(text)
                    : new ScimUserPatchOperation.SetText(text, stringValue(value, name));
        }
        return switch (name) {
            // A remove is refused rather than read as "back to the create default". RFC 7644
            // §3.5.2.2 makes a removed attribute unassigned, and this service has no unassigned
            // `active` to store; reading it as `true` would let a remove reactivate a deactivated
            // User, which only an explicit false-to-true replace may do.
            case "active" -> {
                if (op == Op.REMOVE) {
                    throw ScimErrorException.mutability(
                            "active cannot be removed; replace it with true or false.");
                }
                yield new ScimUserPatchOperation.SetActive(booleanValue(value, "active"));
            }
            case "password" -> op == Op.REMOVE
                    ? new ScimUserPatchOperation.RemovePassword()
                    : new ScimUserPatchOperation.SetPassword(stringValue(value, "password"));
            case "name" -> name(op, path.subAttribute(), value);
            case "emails" -> emails(op, path, value);
            // Every writable User attribute has an arm above, so a writable attribute reaching here
            // is one the schema gained without this reader learning to edit it.
            default -> throw new IllegalStateException("No PATCH edit for the User attribute " + name);
        };
    }

    /** Whether the schema declares this attribute read-only, which no write can change. */
    private static boolean isReadOnly(Optional<ScimAttribute> declared) {
        return declared.map(attribute -> !attribute.isWritable()).orElse(false);
    }

    /** {@code name}'s sub-attribute as the part an edit targets, or empty when it has none such. */
    private static Optional<NamePart> namePart(String sub) {
        return NAME.subAttribute(sub).map(declared -> NAME_PARTS.get(declared.name()));
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
        NamePart part = namePart(sub).orElseThrow(
                () -> ScimErrorException.invalidPath("name has no such sub-attribute."));
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
        ScimEmailFilter filter = path.filter() == null
                ? ScimEmailFilter.ALL
                : ScimPatchPathGrammar.emailFilter(path.filter());
        if (path.subAttribute() != null) {
            ScimEmailPart part = ScimPatchPathGrammar.emailPart(path.subAttribute()).orElseThrow(
                    () -> ScimErrorException.invalidPath("emails has no such sub-attribute."));
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
            if (ScimPatchPathGrammar.emailPart(sub).isEmpty()) {
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
