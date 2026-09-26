package com.example.backend.scim.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The User attributes this service implements, and the one place that says so.
 *
 * <p>Three consumers read this list and they must agree, which is the reason it is a
 * list rather than three hand-written documents:
 *
 * <ul>
 *   <li>{@code /Schemas} renders it, so discovery advertises exactly what exists;
 *   <li>the request reader accepts exactly the writable names and refuses anything
 *       else, so an attribute this service cannot store is rejected rather than
 *       silently dropped;
 *   <li>attribute projection validates requested paths against it, so
 *       {@code attributes=nickName} is a refusal rather than an empty resource.
 * </ul>
 *
 * <p>A test asserts the advertised set and the accepted set are the same set. That is
 * what makes "discovery matches the implementation" checkable rather than reviewed:
 * adding an attribute to the schema document without implementing it, or the reverse,
 * fails the build.
 *
 * <p><strong>Unsupported core attributes are absent on purpose.</strong> RFC 7643's
 * User schema defines {@code nickName}, {@code title}, {@code phoneNumbers},
 * {@code addresses} and more; none is stored here, so none is advertised, and asserting
 * one as a write target is refused. The Enterprise User extension is likewise absent —
 * a service that advertised it and ignored it would be worse than one that says it
 * does not have it.
 */
final class ScimUserAttributes {

    /** Mutability values, as RFC 7643 §7 names them. */
    private static final String READ_WRITE = "readWrite";

    private static final String READ_ONLY = "readOnly";

    private static final String WRITE_ONLY = "writeOnly";

    /** Returned values, as RFC 7643 §7 names them. */
    private static final String DEFAULT_RETURNED = "default";

    private static final String NEVER_RETURNED = "never";

    private static final String ALWAYS_RETURNED = "always";

    /**
     * The common attributes RFC 7643 §3.1 defines for every resource, which is why they
     * are NOT in the schema document: a schema lists the attributes it adds.
     *
     * <p>They are listed here because projection has to accept them —
     * {@code attributes=id,userName} is an ordinary request — and because
     * {@code externalId} is writable even though no resource schema declares it.
     */
    static final Set<String> COMMON_ATTRIBUTES =
            Set.of("schemas", "id", "externalId", "meta");

    /**
     * Read-only attributes a write IGNORES rather than refuses, per RFC 7644 §3.5.2:
     * a client that round-trips a resource it read must be able to PUT it back.
     *
     * <p>{@code groups} is here and absent from the schema document at the same time,
     * and the combination is deliberate: the reverse membership view is not computed
     * until Groups exist, so advertising it would be a claim this service cannot honour
     * — while refusing it would break the round-trip of a resource a later release WILL
     * render. Tolerating it now costs nothing, because ignoring it is what the RFC asks
     * for either way.
     */
    static final Set<String> IGNORED_ON_WRITE = Set.of("id", "meta", "groups");

    /**
     * The attributes the core User schema document advertises, in RFC 7643's order.
     *
     * <p>{@code password} is here with {@code mutability=writeOnly} and
     * {@code returned=never}, which is how the RFC says a credential is declared. That
     * declaration is the whole reason {@code attributes=password} needs no special case:
     * an attribute that is never returned is absent from every rendering, so asking for
     * it yields a resource without it rather than an error.
     */
    static final List<Attribute> SCHEMA_ATTRIBUTES = List.of(
            Attribute.singular("userName", "string", READ_WRITE, DEFAULT_RETURNED)
                    .asRequired()
                    .caseInsensitive()
                    .unique("server"),
            Attribute.complex("name", READ_WRITE, DEFAULT_RETURNED, List.of(
                    Attribute.singular("formatted", "string", READ_WRITE, DEFAULT_RETURNED),
                    Attribute.singular("familyName", "string", READ_WRITE, DEFAULT_RETURNED),
                    Attribute.singular("givenName", "string", READ_WRITE, DEFAULT_RETURNED),
                    Attribute.singular("middleName", "string", READ_WRITE, DEFAULT_RETURNED),
                    Attribute.singular(
                            "honorificPrefix", "string", READ_WRITE, DEFAULT_RETURNED),
                    Attribute.singular(
                            "honorificSuffix", "string", READ_WRITE, DEFAULT_RETURNED))),
            Attribute.singular("displayName", "string", READ_WRITE, DEFAULT_RETURNED),
            Attribute.singular("preferredLanguage", "string", READ_WRITE, DEFAULT_RETURNED),
            Attribute.singular("locale", "string", READ_WRITE, DEFAULT_RETURNED),
            Attribute.singular("timezone", "string", READ_WRITE, DEFAULT_RETURNED),
            Attribute.singular("active", "boolean", READ_WRITE, DEFAULT_RETURNED),
            Attribute.singular("password", "string", WRITE_ONLY, NEVER_RETURNED),
            Attribute.multiValued("emails", READ_WRITE, DEFAULT_RETURNED, List.of(
                    Attribute.singular("value", "string", READ_WRITE, DEFAULT_RETURNED),
                    Attribute.singular("type", "string", READ_WRITE, DEFAULT_RETURNED),
                    Attribute.singular("primary", "boolean", READ_WRITE, DEFAULT_RETURNED))));

    private ScimUserAttributes() {
    }

    /**
     * Every attribute name a write may assert: the writable schema attributes plus
     * {@code externalId}, and {@code schemas}, which every resource body declares.
     */
    static Set<String> writableNames() {
        Set<String> writable = SCHEMA_ATTRIBUTES.stream()
                .filter(attribute -> !READ_ONLY.equals(attribute.mutability()))
                .map(Attribute::name)
                .collect(Collectors.toSet());
        return Set.copyOf(union(writable, Set.of("schemas", "externalId")));
    }

    /**
     * Every top-level attribute name projection may name: the schema's attributes and
     * the common ones, plus {@code groups} — which is tolerated on write and will be
     * rendered once it is computed.
     */
    static Set<String> projectableNames() {
        Set<String> declared = SCHEMA_ATTRIBUTES.stream()
                .map(Attribute::name)
                .collect(Collectors.toSet());
        return Set.copyOf(union(union(declared, COMMON_ATTRIBUTES), Set.of("groups")));
    }

    /** The core User schema, as {@code /Schemas} renders it. */
    static Map<String, Object> schemaDocument() {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("schemas", List.of(ScimSchemas.SCHEMA));
        document.put("id", ScimSchemas.USER);
        document.put("name", "User");
        document.put("description", "SCIM core User, as implemented by this service.");
        document.put(
                "attributes",
                SCHEMA_ATTRIBUTES.stream().map(Attribute::render).toList());
        document.put("meta", Map.of(
                "resourceType", "Schema",
                "location", ScimSchemas.BASE_PATH + "/Schemas/" + ScimSchemas.USER));
        return document;
    }

    private static Set<String> union(Set<String> first, Set<String> second) {
        return java.util.stream.Stream.concat(first.stream(), second.stream())
                .collect(Collectors.toSet());
    }

    /**
     * One attribute definition, as RFC 7643 §7 describes one.
     *
     * <p>A record with named factories rather than a builder, because the combinations
     * that occur are few and each factory names one of them: a singular value, a complex
     * value, a multi-valued complex value. A builder would also permit
     * {@code multiValued} with no sub-attributes, which nothing here needs.
     */
    record Attribute(
            String name,
            String type,
            boolean multiValued,
            boolean required,
            boolean caseExact,
            String mutability,
            String returned,
            String uniqueness,
            List<Attribute> subAttributes) {

        static Attribute singular(
                String name, String type, String mutability, String returned) {
            return new Attribute(
                    name, type, false, false, true, mutability, returned, "none", List.of());
        }

        static Attribute complex(
                String name, String mutability, String returned, List<Attribute> sub) {
            return new Attribute(
                    name, "complex", false, false, true, mutability, returned, "none", sub);
        }

        static Attribute multiValued(
                String name, String mutability, String returned, List<Attribute> sub) {
            return new Attribute(
                    name, "complex", true, false, true, mutability, returned, "none", sub);
        }

        Attribute asRequired() {
            return new Attribute(
                    name, type, multiValued, true, caseExact, mutability, returned,
                    uniqueness, subAttributes);
        }

        Attribute caseInsensitive() {
            return new Attribute(
                    name, type, multiValued, required, false, mutability, returned,
                    uniqueness, subAttributes);
        }

        Attribute unique(String scope) {
            return new Attribute(
                    name, type, multiValued, required, caseExact, mutability, returned,
                    scope, subAttributes);
        }

        /** The attribute as a schema document renders it. */
        Map<String, Object> render() {
            Map<String, Object> rendered = new LinkedHashMap<>();
            rendered.put("name", name);
            rendered.put("type", type);
            if (!subAttributes.isEmpty()) {
                rendered.put(
                        "subAttributes", subAttributes.stream().map(Attribute::render).toList());
            }
            rendered.put("multiValued", multiValued);
            rendered.put("required", required);
            rendered.put("caseExact", caseExact);
            rendered.put("mutability", mutability);
            rendered.put("returned", returned);
            rendered.put("uniqueness", uniqueness);
            return rendered;
        }
    }

    /** Whether an attribute is one a rendering may ever include. */
    static boolean isNeverReturned(String attributeName) {
        return SCHEMA_ATTRIBUTES.stream()
                .anyMatch(attribute -> attribute.name().equals(attributeName)
                        && NEVER_RETURNED.equals(attribute.returned()));
    }

    /** Attributes a projection may not remove, because the RFC returns them always. */
    static Set<String> alwaysReturned() {
        Set<String> always = SCHEMA_ATTRIBUTES.stream()
                .filter(attribute -> ALWAYS_RETURNED.equals(attribute.returned()))
                .map(Attribute::name)
                .collect(Collectors.toSet());
        // `schemas` and `id` are common attributes with returned=always in RFC 7643
        // §3.1; they are not in the list above because a schema document does not
        // declare them. `meta` is returned=default and so may be projected away — the
        // version it carries is also in the ETag header, so a client that excluded it
        // has not lost the ability to make a conditional write.
        return Set.copyOf(union(always, Set.of("schemas", "id")));
    }
}
