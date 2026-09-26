package com.example.backend.scim.controller;

import com.example.backend.scim.application.NewScimUser;
import com.example.backend.scim.domain.ScimEmail;
import com.example.backend.scim.domain.ScimName;
import com.example.backend.scim.domain.ScimUserProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/**
 * Reads a SCIM User write body, refusing what this service does not implement and
 * ignoring what the RFC says to ignore.
 *
 * <p>Hand-read from a {@link JsonNode} rather than bound to a DTO, because SCIM's rules
 * for a write body are not the rules a binder applies. Three of them matter here and
 * none is expressible as a field declaration:
 *
 * <ul>
 *   <li>a read-only attribute in the body is <strong>ignored</strong> (RFC 7644 §3.5.2),
 *       so a client may PUT back a resource it read;
 *   <li>an attribute this service does not implement is <strong>refused</strong>, not
 *       dropped — a silently ignored {@code title} is a client believing it stored one;
 *   <li>a value of the wrong JSON type is a {@code 400 invalidValue} naming the
 *       attribute, not a binder's message naming a Java field.
 * </ul>
 *
 * <p>Binding would give the middle rule the opposite default: Boot disables
 * {@code FAIL_ON_UNKNOWN_PROPERTIES}, so an unsupported attribute would be dropped, and
 * re-enabling it for one DTO is not something an annotation on that DTO can do.
 *
 * <p>No message produced here contains a submitted VALUE. Attribute paths are echoed
 * because the caller needs to know which attribute was wrong, and an attribute name is
 * this service's own vocabulary; a value could be a password.
 */
final class ScimUserRequestReader {

    /** Attributes accepted on a write, from the one list that says what is implemented. */
    private static final Set<String> WRITABLE = ScimUserAttributes.writableNames();

    private ScimUserRequestReader() {
    }

    /**
     * The create command this body describes.
     *
     * @throws ScimErrorException {@code 400} for a body that is not an object, declares
     *                            the wrong schema, omits {@code userName}, asserts an
     *                            unimplemented attribute, or carries a value of the wrong
     *                            type
     */
    static NewScimUser readCreate(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw ScimErrorException.invalidSyntax("The request body must be a SCIM resource.");
        }
        requireCoreUserSchema(body);
        rejectUnsupportedAttributes(body);

        ScimUserProfile profile = new ScimUserProfile(
                requiredString(body, "userName"),
                readName(body.get("name")),
                optionalString(body, "displayName"),
                optionalString(body, "preferredLanguage"),
                optionalString(body, "locale"),
                optionalString(body, "timezone"),
                // RFC 7643's create default. Stated here, at the one place a create is
                // read, rather than as a column default that would also apply to writes
                // this reader never saw.
                optionalBoolean(body, "active", true),
                readEmails(body.get("emails")));
        return new NewScimUser(
                profile, optionalString(body, "password"), optionalString(body, "externalId"));
    }

    /**
     * The body declares exactly this service's one User schema.
     *
     * <p>Exactly, not "contains": a body declaring an extension schema is asserting
     * attributes this service does not have, and accepting the declaration while ignoring
     * the attributes is how a connector comes to believe its extension is stored.
     */
    private static void requireCoreUserSchema(JsonNode body) {
        JsonNode schemas = body.get("schemas");
        if (schemas == null || !schemas.isArray() || schemas.isEmpty()) {
            throw ScimErrorException.invalidSyntax(
                    "A resource must declare its schemas as a non-empty array.");
        }
        if (schemas.size() != 1
                || !ScimSchemas.USER.equals(schemas.get(0).asText())) {
            throw ScimErrorException.invalidValue(
                    "This service implements one User schema only: " + ScimSchemas.USER);
        }
    }

    /**
     * Every attribute in the body is one this service writes, or one it is told to ignore.
     *
     * <p>Checked before anything is read, so a body mixing a valid create with one
     * unsupported attribute is refused whole rather than partly applied.
     */
    private static void rejectUnsupportedAttributes(JsonNode body) {
        for (String attribute : body.propertyNames()) {
            if (WRITABLE.contains(attribute) || ScimUserAttributes.IGNORED_ON_WRITE
                    .contains(attribute)) {
                continue;
            }
            throw ScimErrorException.invalidValue(
                    "This service does not implement the User attribute: "
                            + sanitized(attribute));
        }
    }

    private static ScimName readName(JsonNode name) {
        if (name == null || name.isNull()) {
            return ScimName.NONE;
        }
        if (!name.isObject()) {
            throw ScimErrorException.invalidValue("name must be a complex value.");
        }
        Set<String> declared = declaredSubAttributes("name");
        for (String sub : name.propertyNames()) {
            if (!declared.contains(sub)) {
                throw ScimErrorException.invalidValue(
                        "This service does not implement the name sub-attribute: "
                                + sanitized(sub));
            }
        }
        return new ScimName(
                optionalString(name, "formatted"),
                optionalString(name, "familyName"),
                optionalString(name, "givenName"),
                optionalString(name, "middleName"),
                optionalString(name, "honorificPrefix"),
                optionalString(name, "honorificSuffix"));
    }

    private static List<ScimEmail> readEmails(JsonNode emails) {
        if (emails == null || emails.isNull()) {
            return List.of();
        }
        if (!emails.isArray()) {
            throw ScimErrorException.invalidValue("emails must be an array.");
        }
        Set<String> declared = declaredSubAttributes("emails");
        List<ScimEmail> read = new ArrayList<>(emails.size());
        for (JsonNode email : emails) {
            if (!email.isObject()) {
                throw ScimErrorException.invalidValue("each emails value must be a complex value.");
            }
            for (String sub : email.propertyNames()) {
                if (!declared.contains(sub)) {
                    throw ScimErrorException.invalidValue(
                            "This service does not implement the emails sub-attribute: "
                                    + sanitized(sub));
                }
            }
            read.add(new ScimEmail(
                    requiredString(email, "value"),
                    optionalString(email, "type"),
                    optionalBoolean(email, "primary", false)));
        }
        return read;
    }

    private static Set<String> declaredSubAttributes(String attribute) {
        return ScimUserAttributes.SCHEMA_ATTRIBUTES.stream()
                .filter(declared -> declared.name().equals(attribute))
                .flatMap(declared -> declared.subAttributes().stream())
                .map(ScimUserAttributes.Attribute::name)
                .collect(java.util.stream.Collectors.toSet());
    }

    private static String requiredString(JsonNode parent, String attribute) {
        JsonNode value = parent.get(attribute);
        if (value == null || value.isNull()) {
            throw ScimErrorException.invalidSyntax("A required attribute is missing: " + attribute);
        }
        return string(value, attribute);
    }

    private static String optionalString(JsonNode parent, String attribute) {
        JsonNode value = parent.get(attribute);
        return value == null || value.isNull() ? null : string(value, attribute);
    }

    private static String string(JsonNode value, String attribute) {
        if (!value.isString()) {
            throw ScimErrorException.invalidValue(attribute + " must be a string.");
        }
        String text = value.stringValue();
        if (text.isBlank()) {
            throw ScimErrorException.invalidValue(attribute + " must not be blank.");
        }
        return text;
    }

    private static boolean optionalBoolean(
            JsonNode parent, String attribute, boolean whenAbsent) {
        JsonNode value = parent.get(attribute);
        if (value == null || value.isNull()) {
            return whenAbsent;
        }
        if (!value.isBoolean()) {
            throw ScimErrorException.invalidValue(attribute + " must be a boolean.");
        }
        return value.booleanValue();
    }

    /**
     * A client-supplied attribute NAME, reduced to something safe to put in a response and
     * in the log line an error may produce.
     *
     * <p>Control characters and line breaks are what a crafted attribute name would carry
     * to forge a log record (CWE-117), and the length cap keeps a megabyte-long key from
     * being echoed at all. The name is lower-cased so the echo cannot be mistaken for a
     * canonical spelling this service recognises.
     */
    private static String sanitized(String attribute) {
        String stripped = attribute.codePoints()
                .filter(codePoint -> !Character.isISOControl(codePoint))
                .limit(64)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint,
                        StringBuilder::append)
                .toString();
        return stripped.toLowerCase(Locale.ROOT);
    }
}
