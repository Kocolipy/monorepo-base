package com.example.backend.scim.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The Group attributes this service implements, and the one place that says so.
 *
 * <p>The same three consumers read this list as read {@link ScimUserAttributes}, and for the same
 * reason: {@code /Schemas} renders it, the request reader accepts exactly the writable names and
 * refuses anything else, and attribute projection validates requested paths against it. A test
 * asserts the advertised set and the accepted set are the same set, so adding an attribute to the
 * document without implementing it fails the build.
 *
 * <p>Deliberately short. RFC 7643's Group schema defines {@code displayName} and {@code members},
 * and that is the whole of it — a Group in this directory is a name and a membership, because what
 * a Group is FOR here is conferring authority, and nothing else about it changes who may do what.
 *
 * <p>{@code members} is declared {@code readWrite} at the top level with {@code readOnly}
 * sub-attributes other than {@code value}. That is RFC 7643 §4.2's own shape and it is what makes
 * "{@code display} is ignored on write" an advertised fact rather than an undocumented behaviour of
 * this implementation: a connector reading the schema is told the label is not something it sets.
 */
final class ScimGroupAttributes {

    private static final String READ_WRITE = "readWrite";

    private static final String READ_ONLY = "readOnly";

    private static final String DEFAULT_RETURNED = "default";

    /**
     * Read-only attributes a write IGNORES rather than refuses, per RFC 7644 §3.5.2, so a client
     * that round-trips a resource it read can PUT it back unchanged.
     */
    static final Set<String> IGNORED_ON_WRITE = Set.of("id", "meta");

    /** The attributes the core Group schema document advertises, in RFC 7643's order. */
    static final List<ScimUserAttributes.Attribute> SCHEMA_ATTRIBUTES = List.of(
            ScimUserAttributes.Attribute
                    .singular("displayName", "string", READ_WRITE, DEFAULT_RETURNED)
                    .asRequired()
                    .caseInsensitive()
                    // Not what RFC 7643 says — the RFC leaves `displayName` non-unique. This
                    // directory makes it server-unique because a Group's membership confers
                    // authority, and two Groups an administrator reads as the same name is how
                    // membership of the wrong one gets granted. Advertised, so a connector is told
                    // rather than discovering it through a 409.
                    .unique("server"),
            ScimUserAttributes.Attribute.multiValued(
                    "members", READ_WRITE, DEFAULT_RETURNED, List.of(
                            // The only sub-attribute a write supplies: the member's resource id.
                            ScimUserAttributes.Attribute
                                    .singular("value", "string", READ_WRITE, DEFAULT_RETURNED),
                            // Both read-only, and both derived from the referenced User rather
                            // than stored, which is why a submitted value is ignored: there is no
                            // column it could be written to.
                            ScimUserAttributes.Attribute
                                    .singular("display", "string", READ_ONLY, DEFAULT_RETURNED),
                            ScimUserAttributes.Attribute
                                    .singular("$ref", "reference", READ_ONLY, DEFAULT_RETURNED),
                            ScimUserAttributes.Attribute
                                    .singular("type", "string", READ_ONLY, DEFAULT_RETURNED))));

    private ScimGroupAttributes() {
    }

    /**
     * Every attribute name a write may assert: the writable schema attributes plus
     * {@code externalId}, and {@code schemas}, which every resource body declares.
     */
    static Set<String> writableNames() {
        Set<String> writable = SCHEMA_ATTRIBUTES.stream()
                .filter(attribute -> !READ_ONLY.equals(attribute.mutability()))
                .map(ScimUserAttributes.Attribute::name)
                .collect(Collectors.toSet());
        return Set.copyOf(union(writable, Set.of("schemas", "externalId")));
    }

    /** Every top-level attribute name projection may name: the schema's, plus the common ones. */
    static Set<String> projectableNames() {
        Set<String> declared = SCHEMA_ATTRIBUTES.stream()
                .map(ScimUserAttributes.Attribute::name)
                .collect(Collectors.toSet());
        return Set.copyOf(union(declared, ScimUserAttributes.COMMON_ATTRIBUTES));
    }

    /** The core Group schema, as {@code /Schemas} renders it. */
    static Map<String, Object> schemaDocument() {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("schemas", List.of(ScimSchemas.SCHEMA));
        document.put("id", ScimSchemas.GROUP);
        document.put("name", "Group");
        document.put("description", "SCIM core Group, as implemented by this service.");
        document.put(
                "attributes",
                SCHEMA_ATTRIBUTES.stream()
                        .map(ScimUserAttributes.Attribute::render)
                        .toList());
        document.put("meta", Map.of(
                "resourceType", "Schema",
                "location", ScimSchemas.BASE_PATH + "/Schemas/" + ScimSchemas.GROUP));
        return document;
    }

    private static Set<String> union(Set<String> first, Set<String> second) {
        return Stream.concat(first.stream(), second.stream()).collect(Collectors.toSet());
    }
}
