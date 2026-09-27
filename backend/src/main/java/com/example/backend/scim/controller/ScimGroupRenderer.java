package com.example.backend.scim.controller;

import com.example.backend.scim.application.ScimGroupListing;
import com.example.backend.scim.application.ScimGroupResource;
import com.example.backend.scim.domain.ScimGroupMember;
import com.example.backend.scim.domain.ScimResourceType;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders a Group projection as the canonical SCIM document, and a page of them as a
 * {@code ListResponse}.
 *
 * <p>An ordered map rather than a bound DTO, for the reasons {@link ScimUserRenderer} gives: SCIM
 * omits an unassigned attribute rather than rendering it null, the attribute ORDER is part of what
 * a reader of the wire format recognises, and attribute projection then filters the same structure.
 *
 * <p>The renderer receives {@link ScimGroupResource}, which has no field the reservation marker
 * could occupy. So "a client cannot read which resources are protected" is a property of the shape
 * rather than of this class's care — there is nothing here to leave out.
 */
final class ScimGroupRenderer {

    /**
     * {@code meta.created} and {@code meta.lastModified} in UTC with a {@code Z} offset, which is
     * what RFC 7643's {@code xsd:dateTime} convention leads a connector to expect.
     */
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ISO_INSTANT;

    /**
     * Every member is a User, which is the whole of this directory's membership model — so
     * {@code members.type} is a constant rather than a stored value. RFC 7643 allows {@code Group}
     * here; nothing in this directory can produce it, because the membership row's foreign key
     * points at the User table.
     */
    private static final String MEMBER_TYPE = "User";

    private ScimGroupRenderer() {
    }

    /**
     * The resource as SCIM renders it.
     *
     * @param baseUri absolute URI of this service's SCIM base, with no trailing slash, so
     *                {@code meta.location} and every member's {@code $ref} are absolute as RFC 7643
     *                expects
     */
    static Map<String, Object> render(ScimGroupResource group, String baseUri) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("schemas", List.of(ScimSchemas.GROUP));
        document.put("id", group.id().toString());
        if (group.externalId() != null) {
            document.put("externalId", group.externalId());
        }
        document.put("displayName", group.displayName());
        // Omitted entirely when empty rather than rendered as an empty array, as SCIM omits any
        // unassigned attribute. An empty Group is legitimate and renders without `members`.
        if (!group.members().isEmpty()) {
            document.put("members", renderMembers(group.members(), baseUri));
        }
        document.put("meta", renderMeta(group, baseUri));
        return document;
    }

    /** The strong entity tag for a resource version, spelled as {@link ScimUserRenderer} spells it. */
    static String etag(long version) {
        return "\"" + version + "\"";
    }

    /** The location of one Group, absolute. */
    static String location(String baseUri, ScimGroupResource group) {
        return baseUri + "/Groups/" + group.id();
    }

    /**
     * A page as SCIM's {@code ListResponse}.
     *
     * <p>{@code itemsPerPage} is the size of the page RETURNED and not the size asked for, per RFC
     * 7644, and {@code Resources} is omitted when empty rather than rendered as an empty array.
     */
    static Map<String, Object> renderList(
            ScimGroupListing listing, String baseUri, ScimAttributeProjection projection) {
        List<Map<String, Object>> resources = new ArrayList<>(listing.resources().size());
        for (ScimGroupResource group : listing.resources()) {
            resources.add(projection.apply(render(group, baseUri)));
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

    /**
     * The membership, each entry carrying the referenced User's id, its label, a resolvable
     * reference to it, and its type.
     *
     * <p>{@code display} is omitted when the referenced User has neither a {@code displayName} nor
     * — impossibly — a {@code userName}, rather than rendered null. The {@code $ref} is built from
     * the id here rather than stored, because it is a URI of THIS deployment: storing it would make
     * a rendered reference wrong the moment the service moved.
     */
    private static List<Map<String, Object>> renderMembers(
            List<ScimGroupMember> members, String baseUri) {
        List<Map<String, Object>> rendered = new ArrayList<>(members.size());
        for (ScimGroupMember member : members) {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("value", member.userId().toString());
            if (member.display() != null) {
                value.put("display", member.display());
            }
            value.put("$ref", baseUri + "/Users/" + member.userId());
            value.put("type", MEMBER_TYPE);
            rendered.add(value);
        }
        return List.copyOf(rendered);
    }

    private static Map<String, Object> renderMeta(ScimGroupResource group, String baseUri) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("resourceType", ScimResourceType.GROUP.resourceTypeName());
        meta.put("created", TIMESTAMP.format(group.createdAt()));
        meta.put("lastModified", TIMESTAMP.format(group.lastModifiedAt()));
        meta.put("location", location(baseUri, group));
        meta.put("version", etag(group.version()));
        return meta;
    }
}
