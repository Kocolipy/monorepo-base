package com.example.backend.scim.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The claim that discovery matches the implementation, as an assertion.
 *
 * <p>The schema document and the set of attributes a write accepts are derived from one
 * list, so these tests are what makes that derivation load-bearing: advertising an
 * attribute nothing stores, or accepting one nothing advertises, fails here.
 */
class ScimUserAttributesTests {

    @Test
    void the_schema_document_advertises_exactly_the_declared_attributes() {
        assertThat(advertisedNames()).containsExactlyInAnyOrderElementsOf(
                ScimUserAttributes.SCHEMA_ATTRIBUTES.stream()
                        .map(ScimUserAttributes.Attribute::name)
                        .collect(Collectors.toSet()));
    }

    /**
     * Every attribute a write accepts is advertised, and every advertised writable one is
     * accepted. The two common attributes a resource body carries — {@code schemas} and
     * {@code externalId} — are writable without being declared by a resource schema, which
     * is RFC 7643 §3.1, so they are named here rather than derived.
     */
    @Test
    void the_accepted_write_attributes_are_the_advertised_ones() {
        Set<String> accepted = ScimUserAttributes.writableNames();

        assertThat(accepted).containsAll(Set.of("schemas", "externalId"));
        assertThat(accepted).containsExactlyInAnyOrderElementsOf(
                java.util.stream.Stream.concat(
                                advertisedNames().stream(), Set.of("schemas", "externalId").stream())
                        .collect(Collectors.toSet()));
    }

    /**
     * {@code password} is declared write-only and never returned, which is the declaration
     * that makes {@code attributes=password} a silent omission rather than a special case.
     */
    @Test
    void the_password_is_declared_write_only_and_never_returned() {
        Map<String, Object> password = advertised("password");

        assertThat(password).containsEntry("mutability", "writeOnly");
        assertThat(password).containsEntry("returned", "never");
        assertThat(ScimUserAttributes.isNeverReturned("password")).isTrue();
        assertThat(ScimUserAttributes.isNeverReturned("userName")).isFalse();
    }

    @Test
    void user_name_is_required_case_insensitive_and_server_unique() {
        Map<String, Object> userName = advertised("userName");

        assertThat(userName).containsEntry("required", true);
        assertThat(userName).containsEntry("caseExact", false);
        assertThat(userName).containsEntry("uniqueness", "server");
    }

    /**
     * An unimplemented core attribute is absent from the document. A service advertising
     * {@code phoneNumbers} and dropping them is worse than one that says it has none.
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "nickName", "profileUrl", "title", "userType", "phoneNumbers", "ims", "photos",
        "addresses", "entitlements", "roles", "x509Certificates", "groups",
    })
    void an_unimplemented_core_attribute_is_not_advertised(String attribute) {
        assertThat(advertisedNames()).doesNotContain(attribute);
    }

    /** The common attributes are not schema attributes, and must not be advertised as any. */
    @ParameterizedTest
    @ValueSource(strings = {"id", "externalId", "meta", "schemas"})
    void a_common_attribute_is_not_declared_by_the_resource_schema(String attribute) {
        assertThat(advertisedNames()).doesNotContain(attribute);
        assertThat(ScimUserAttributes.projectableNames()).contains(attribute);
    }

    /** {@code schemas} and {@code id} are the attributes a projection may never remove. */
    @Test
    void the_always_returned_attributes_are_schemas_and_id() {
        assertThat(ScimUserAttributes.alwaysReturned()).containsExactlyInAnyOrder("schemas", "id");
    }

    @Test
    void the_document_declares_itself_as_a_schema_at_its_own_location() {
        Map<String, Object> document = ScimUserAttributes.schemaDocument();

        assertThat(document).containsEntry("schemas", List.of(ScimSchemas.SCHEMA));
        assertThat(document).containsEntry("id", ScimSchemas.USER);
        assertThat(document.get("meta")).isEqualTo(Map.of(
                "resourceType", "Schema",
                "location", "/scim/v2/Schemas/" + ScimSchemas.USER));
    }

    /**
     * The rendered schema document, in full, field for field.
     *
     * <p>This is the one assertion the other tests in this class cannot replace. They check
     * WHICH attributes are advertised; this checks what is said ABOUT each of them —
     * {@code type}, {@code multiValued}, {@code required}, {@code caseExact},
     * {@code mutability}, {@code returned}, {@code uniqueness} and the sub-attribute lists.
     * A connector reads exactly these values to decide whether it may write an attribute,
     * whether to match on it case-sensitively, and whether it is single- or multi-valued, so
     * a wrong flag here is a wrong integration everywhere — and it is invisible to a test
     * that only compares names.
     *
     * <p>Whole-document equality rather than a field-by-field walk, deliberately: an
     * attribute gaining a key nobody asserted is the failure mode this catches. The list is
     * compared in order, because RFC 7643 §4.1 gives the core User attributes an order and
     * a client may render them in it.
     */
    @Test
    void the_rendered_schema_document_is_exactly_this() {
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("schemas", List.of(ScimSchemas.SCHEMA));
        expected.put("id", ScimSchemas.USER);
        expected.put("name", "User");
        expected.put("description", "SCIM core User, as implemented by this service.");
        expected.put("attributes", List.of(
                attribute("userName", "string", false, true, false, READ_WRITE, DEFAULT, "server", List.of()),
                attribute("name", "complex", false, false, true, READ_WRITE, DEFAULT, "none", List.of(
                        writable("formatted", "string"),
                        writable("familyName", "string"),
                        writable("givenName", "string"),
                        writable("middleName", "string"),
                        writable("honorificPrefix", "string"),
                        writable("honorificSuffix", "string"))),
                writable("displayName", "string"),
                writable("preferredLanguage", "string"),
                writable("locale", "string"),
                writable("timezone", "string"),
                writable("active", "boolean"),
                attribute("password", "string", false, false, true, "writeOnly", "never", "none", List.of()),
                attribute("emails", "complex", true, false, true, READ_WRITE, DEFAULT, "none", List.of(
                        writable("value", "string"),
                        writable("type", "string"),
                        writable("primary", "boolean")))));
        expected.put("meta", Map.of(
                "resourceType", "Schema",
                "location", "/scim/v2/Schemas/" + ScimSchemas.USER));

        assertThat(ScimUserAttributes.schemaDocument()).isEqualTo(expected);
    }

    private static final String READ_WRITE = "readWrite";

    private static final String DEFAULT = "default";

    /** An ordinary single-valued, optional, case-exact, writable attribute. */
    private static Map<String, Object> writable(String name, String type) {
        return attribute(name, type, false, false, true, READ_WRITE, DEFAULT, "none", List.of());
    }

    private static Map<String, Object> attribute(
            String name,
            String type,
            boolean multiValued,
            boolean required,
            boolean caseExact,
            String mutability,
            String returned,
            String uniqueness,
            List<Map<String, Object>> subAttributes) {
        Map<String, Object> rendered = new LinkedHashMap<>();
        rendered.put("name", name);
        rendered.put("type", type);
        if (!subAttributes.isEmpty()) {
            rendered.put("subAttributes", subAttributes);
        }
        rendered.put("multiValued", multiValued);
        rendered.put("required", required);
        rendered.put("caseExact", caseExact);
        rendered.put("mutability", mutability);
        rendered.put("returned", returned);
        rendered.put("uniqueness", uniqueness);
        return rendered;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> advertisedNames() {
        List<Map<String, Object>> attributes =
                (List<Map<String, Object>>) ScimUserAttributes.schemaDocument().get("attributes");
        return attributes.stream()
                .map(attribute -> (String) attribute.get("name"))
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> advertised(String name) {
        List<Map<String, Object>> attributes =
                (List<Map<String, Object>>) ScimUserAttributes.schemaDocument().get("attributes");
        return attributes.stream()
                .filter(attribute -> name.equals(attribute.get("name")))
                .findFirst()
                .orElseThrow();
    }
}
