package com.example.backend.scim.controller;

import com.example.backend.scim.domain.ScimAttribute;
import com.example.backend.scim.domain.ScimEmailFilter;
import com.example.backend.scim.domain.ScimEmailPart;
import com.example.backend.scim.domain.ScimResourceSchema;
import com.example.backend.scim.domain.ScimResourceType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The grammar of a SCIM User PATCH {@code path}: text in, a parsed path or an email value filter
 * out — or a refusal.
 *
 * <p>A refusal is a {@link ScimErrorException} carrying the {@code scimType} and detail the
 * response uses, so a caller propagates it unchanged:
 *
 * <ul>
 *   <li>a path that does not parse, or a value filter with an unterminated string —
 *       {@code invalidPath};
 *   <li>a value filter using anything but {@code eq} comparisons on {@code emails}
 *       sub-attributes joined by {@code and} — {@code invalidFilter}, because the full filter
 *       grammar is not implemented yet and an approximated filter would select values the
 *       client did not ask for.
 * </ul>
 *
 * <p>This module knows the shape of the text and the {@code emails} sub-attribute names a filter
 * compares; what a parsed path then means for a User — read-only, unimplemented, which edit — is
 * {@link ScimUserPatchReader}'s business. No message produced here echoes a submitted value; a
 * path is echoed only after {@link ScimUserRequestReader#sanitized}.
 */
final class ScimPatchPathGrammar {

    private static final String SCHEMA_PREFIX = ScimSchemas.USER.toLowerCase(Locale.ROOT) + ":";

    /** One attribute or sub-attribute name (RFC 7643 §2.1 ATTRNAME), the grammar's only name rule. */
    private static final String NAME = "[A-Za-z][A-Za-z0-9_$-]*";

    /** {@code attr}, {@code attr.sub}, {@code attr[filter]}, {@code attr[filter].sub}. */
    private static final Pattern PATH =
            Pattern.compile("(" + NAME + ")(?:\\[(.+)])?(?:\\.(" + NAME + "))?");

    /**
     * A plain attribute name: {@link #PATH} with neither a filter nor a sub-attribute, which is
     * exactly {@link #PATH}'s first group, since both are built from {@link #NAME}.
     */
    private static final Pattern ATTRIBUTE_NAME = Pattern.compile(NAME);

    /** One {@code sub op value} comparison inside a value filter. */
    private static final Pattern COMPARISON =
            Pattern.compile("\\s*(" + NAME + ")\\s+([A-Za-z]+)\\s+(.+?)\\s*");

    /** The {@code emails} attribute, whose sub-attributes a value filter and a sub-path name. */
    private static final ScimAttribute EMAILS =
            ScimResourceSchema.of(ScimResourceType.USER).find("emails").orElseThrow();

    /** {@code emails}' sub-attributes, by canonical name, as the part each edits or selects. */
    private static final Map<String, ScimEmailPart> EMAIL_PARTS = Map.of(
            "value", ScimEmailPart.VALUE,
            "type", ScimEmailPart.TYPE,
            "primary", ScimEmailPart.PRIMARY);

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /**
     * A parsed path: the attribute and sub-attribute lower-cased, and the value filter's text as
     * written, or {@code null} for each part the path does not have.
     */
    record Path(String attribute, String filter, String subAttribute) {
    }

    private ScimPatchPathGrammar() {
    }

    /**
     * The path this text denotes. Surrounding whitespace and the core User schema URN prefix are
     * not part of it, and the prefix matches case-insensitively (RFC 7644 §3.10, §3.5.2).
     *
     * @throws ScimErrorException {@code invalidPath} when the text is not an attribute path
     */
    static Path path(String rawPath) {
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

    /** Whether this text is a plain attribute name, with no filter and no sub-attribute. */
    static boolean isAttributeName(String text) {
        return ATTRIBUTE_NAME.matcher(text).matches();
    }

    /** {@code emails}' sub-attribute this name denotes, case-insensitively, or empty for none. */
    static Optional<ScimEmailPart> emailPart(String sub) {
        return EMAILS.subAttribute(sub).map(declared -> EMAIL_PARTS.get(declared.name()));
    }

    /**
     * An {@code emails} value filter: {@code eq} comparisons on email sub-attributes, joined by
     * {@code and}.
     *
     * <p>Split on {@code and} only outside quoted strings, so a compared value containing the word
     * is not mistaken for a conjunction. Grouping is refused here; {@code or}, {@code not} and every
     * other operator are refused as {@code invalidFilter} by the comparison and literal parsing
     * below — an {@code or} leaves the text after it trailing the compared value, which is then not
     * one JSON literal — rather than approximated.
     *
     * @param text the filter between a path's brackets, as written
     * @throws ScimErrorException {@code invalidFilter} for an unsupported filter, and
     *     {@code invalidPath} for an unterminated string
     */
    static ScimEmailFilter emailFilter(String text) {
        List<ScimEmailFilter.Condition> conditions = new ArrayList<>();
        for (String comparison : conjuncts(text)) {
            Matcher matcher = COMPARISON.matcher(comparison);
            if (!matcher.matches()) {
                throw ScimErrorException.invalidFilter(
                        "Only 'sub-attribute eq value' comparisons joined by 'and' are supported"
                                + " in an emails filter.");
            }
            ScimEmailPart part = emailPart(matcher.group(1)).orElseThrow(
                    () -> ScimErrorException.invalidFilter("emails has no such sub-attribute."));
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
}
