package com.example.backend.scim.controller;

import com.example.backend.scim.application.ScimUserListing;
import com.example.backend.scim.application.ScimUserResource;
import com.example.backend.scim.domain.ScimEmail;
import com.example.backend.scim.domain.ScimName;
import com.example.backend.scim.domain.ScimResourceType;
import com.example.backend.scim.domain.ScimUserProfile;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders a User projection as the canonical SCIM document, and a page of them as a
 * {@code ListResponse}.
 *
 * <p>An ordered map rather than a bound DTO, for three reasons the JSON contract makes
 * concrete: SCIM omits an unassigned attribute entirely rather than rendering it null,
 * so presence has to be decided per attribute; the attribute ORDER is part of what a
 * reader of the wire format recognises; and attribute projection then filters the same
 * structure, so there is one representation to reason about rather than a DTO plus a
 * filtered copy of it.
 *
 * <p>The renderer receives {@link ScimUserResource}, which has no credential component.
 * "The password never appears in a response" is therefore not enforced here — there is
 * nothing here to enforce it against.
 */
final class ScimUserRenderer {

    /**
     * {@code meta.created} and {@code meta.lastModified} in UTC with a {@code Z} offset,
     * which is what RFC 7643's {@code xsd:dateTime} convention leads a connector to
     * expect. The service's LOG timestamps use a different zone on purpose; these are
     * different fields with different consumers and are not reconciled.
     */
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ISO_INSTANT;

    private ScimUserRenderer() {
    }

    /**
     * The resource as SCIM renders it.
     *
     * @param baseUri absolute URI of this service's SCIM base, with no trailing slash,
     *                so {@code meta.location} is absolute as RFC 7643 expects
     */
    static Map<String, Object> render(ScimUserResource user, String baseUri) {
        ScimUserProfile profile = user.profile();
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("schemas", List.of(ScimSchemas.USER));
        document.put("id", user.id().toString());
        putIfPresent(document, "externalId", user.externalId());
        document.put("userName", profile.userName());
        if (!profile.name().isUnassigned()) {
            document.put("name", renderName(profile.name()));
        }
        putIfPresent(document, "displayName", profile.displayName());
        putIfPresent(document, "preferredLanguage", profile.preferredLanguage());
        putIfPresent(document, "locale", profile.locale());
        putIfPresent(document, "timezone", profile.timezone());
        document.put("active", profile.active());
        if (!profile.emails().isEmpty()) {
            document.put("emails", renderEmails(profile.emails()));
        }
        document.put("meta", renderMeta(user, baseUri));
        return document;
    }

    /**
     * The strong entity tag for a resource version.
     *
     * <p>Strong — no {@code W/} prefix — because SCIM conditional writes compare
     * validators for exact equality, and a weak validator means "semantically
     * equivalent", which is not a claim this service makes or needs. The same string goes
     * in the {@code ETag} header and in {@code meta.version}, quotes included, so a
     * client can compare the two without unwrapping either.
     */
    static String etag(long version) {
        return "\"" + version + "\"";
    }

    /** The location of one User, absolute. */
    static String location(String baseUri, ScimUserResource user) {
        return baseUri + "/Users/" + user.id();
    }

    /**
     * A page as SCIM's {@code ListResponse}.
     *
     * <p>{@code itemsPerPage} is the size of the page RETURNED and not the size asked
     * for: RFC 7644 defines it as the number of resources in this response, so a last
     * page of three reports three. {@code Resources} is omitted entirely when empty, as
     * the RFC's own examples show, rather than rendered as an empty array.
     */
    static Map<String, Object> renderList(
            ScimUserListing listing, String baseUri, ScimAttributeProjection projection) {
        List<Map<String, Object>> resources = new ArrayList<>(listing.resources().size());
        for (ScimUserResource user : listing.resources()) {
            resources.add(projection.apply(render(user, baseUri)));
        }
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("schemas", List.of(ScimSchemas.LIST_RESPONSE));
        document.put("totalResults", listing.totalResults());
        document.put("startIndex", listing.page().startIndex());
        document.put("itemsPerPage", resources.size());
        if (!resources.isEmpty()) {
            document.put("Resources", List.copyOf(resources));
        }
        return document;
    }

    private static Map<String, Object> renderName(ScimName name) {
        Map<String, Object> rendered = new LinkedHashMap<>();
        putIfPresent(rendered, "formatted", name.formatted());
        putIfPresent(rendered, "familyName", name.familyName());
        putIfPresent(rendered, "givenName", name.givenName());
        putIfPresent(rendered, "middleName", name.middleName());
        putIfPresent(rendered, "honorificPrefix", name.honorificPrefix());
        putIfPresent(rendered, "honorificSuffix", name.honorificSuffix());
        return rendered;
    }

    private static List<Map<String, Object>> renderEmails(List<ScimEmail> emails) {
        List<Map<String, Object>> rendered = new ArrayList<>(emails.size());
        for (ScimEmail email : emails) {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("value", email.value());
            putIfPresent(value, "type", email.type());
            // Rendered only when true: `primary` means "this is the preferred value", and
            // RFC 7643 treats its absence as false, so emitting false on every other
            // value is noise a client has to ignore.
            if (email.primary()) {
                value.put("primary", true);
            }
            rendered.add(value);
        }
        return List.copyOf(rendered);
    }

    private static Map<String, Object> renderMeta(ScimUserResource user, String baseUri) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("resourceType", ScimResourceType.USER.resourceTypeName());
        meta.put("created", TIMESTAMP.format(user.createdAt()));
        meta.put("lastModified", TIMESTAMP.format(user.lastModifiedAt()));
        meta.put("location", location(baseUri, user));
        meta.put("version", etag(user.version()));
        return meta;
    }

    /** SCIM omits an unassigned attribute; it does not render it as null. */
    private static void putIfPresent(Map<String, Object> document, String name, String value) {
        if (value != null) {
            document.put(name, value);
        }
    }
}
