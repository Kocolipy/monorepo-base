package com.example.backend.scim.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The claim that discovery matches the implementation, as an assertion.
 *
 * <p>The schema document and the set of attributes a write accepts are derived from one
 * list, so these tests are what makes that derivation load-bearing: advertising an
 * attribute nothing stores, or accepting one nothing advertises, fails here.
 *
 * <p>Both vocabularies are held to that standard. {@link ScimGroupAttributes} is a second
 * list read by the same three consumers, so the Group document and the Group write's
 * accepted set are compared the same way the User's are — a Group attribute advertised and
 * not implemented would otherwise be exactly the defect this class exists to catch,
 * unnoticed because it is on the newer of the two resources.
 */
class ScimUserAttributesTests {

    private static final String BASE_URI = ScimTestUris.BASE_URI;

    @Test
    void the_schema_document_advertises_exactly_the_declared_attributes() {
        assertThat(advertisedNames()).containsExactlyInAnyOrderElementsOf(
                ScimUserAttributes.SCHEMA_ATTRIBUTES.stream()
                        .map(ScimUserAttributes.Attribute::name)
                        .collect(Collectors.toSet()));
    }

    /**
     * Every attribute a write accepts is advertised, and every advertised WRITABLE one is
     * accepted. The two common attributes a resource body carries — {@code schemas} and
     * {@code externalId} — are writable without being declared by a resource schema, which
     * is RFC 7643 §3.1, so they are named here rather than derived.
     *
     * <p>"Writable" is read out of the document's own {@code mutability}, not out of a list
     * written here: {@code groups} is advertised and is NOT accepted on a write, and the
     * declaration that says so is the same one a connector reads. Deriving the expectation
     * from a second hand-written list would let the document and the accepted set disagree
     * with nothing failing.
     */
    @Test
    void the_accepted_write_attributes_are_the_advertised_writable_ones() {
        Set<String> accepted = ScimUserAttributes.writableNames();

        assertThat(accepted).containsAll(Set.of("schemas", "externalId"));
        assertThat(accepted).containsExactlyInAnyOrderElementsOf(
                withCommonWritables(advertisedWritableNames(ScimUserAttributes.schemaDocument(BASE_URI))));
    }

    /**
     * The reverse membership view is advertised, wholly read-only, and a write neither
     * accepts nor refuses it: RFC 7644 §3.5.2 has a read-only attribute in a body IGNORED, so
     * a client that round-trips a User it read can PUT it back.
     */
    @Test
    void the_reverse_groups_view_is_advertised_read_only_and_ignored_on_a_write() {
        Map<String, Object> groups = advertised("groups");

        assertThat(groups).containsEntry("mutability", "readOnly");
        assertThat(groups).containsEntry("multiValued", true);
        assertThat(groups).containsEntry("returned", "default");
        assertThat(subAttributeNames(groups))
                .containsExactly("value", "display", "$ref", "type");
        assertThat(subAttributes(groups))
                .allSatisfy(sub -> assertThat(sub).containsEntry("mutability", "readOnly"));
        assertThat(ScimUserAttributes.writableNames()).doesNotContain("groups");
        assertThat(ScimUserAttributes.IGNORED_ON_WRITE).contains("groups");
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
     *
     * <p>{@code groups} has left this list, and that is the inversion this ticket makes: the
     * reverse membership view is computed and rendered now, so it is advertised — as
     * {@code readOnly}, which the test above asserts.
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "nickName", "profileUrl", "title", "userType", "phoneNumbers", "ims", "photos",
        "addresses", "entitlements", "roles", "x509Certificates",
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

    /**
     * Projection names the declared attributes and the common ones, with no special case
     * left for {@code groups}: it is a declared schema attribute now, so it is projectable
     * because the derivation says so rather than because a list names it.
     */
    @Test
    void every_declared_and_common_attribute_is_projectable() {
        assertThat(ScimUserAttributes.projectableNames())
                .containsExactlyInAnyOrderElementsOf(union(
                        advertisedNames(), ScimUserAttributes.COMMON_ATTRIBUTES));
        assertThat(ScimUserAttributes.projectableNames()).contains("groups");
    }

    /** {@code schemas} and {@code id} are the attributes a projection may never remove. */
    @Test
    void the_always_returned_attributes_are_schemas_and_id() {
        assertThat(ScimUserAttributes.alwaysReturned()).containsExactlyInAnyOrder("schemas", "id");
    }

    @Test
    void the_document_declares_itself_as_a_schema_at_its_own_location() {
        Map<String, Object> document = ScimUserAttributes.schemaDocument(BASE_URI);

        assertThat(document).containsEntry("schemas", List.of(ScimSchemas.SCHEMA));
        assertThat(document).containsEntry("id", ScimSchemas.USER);
        assertThat(document.get("meta")).isEqualTo(Map.of(
                "resourceType", "Schema",
                "location", BASE_URI + "/Schemas/" + ScimSchemas.USER));
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
                attribute("name", "complex", false, false, false, READ_WRITE, DEFAULT, "none", List.of(
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
                attribute("password", "string", false, false, false, "writeOnly", "never", "none", List.of()),
                attribute("emails", "complex", true, false, false, READ_WRITE, DEFAULT, "none", List.of(
                        writable("value", "string"),
                        with(writable("type", "string"),
                                "canonicalValues", List.of("work", "home", "other")),
                        writable("primary", "boolean"))),
                attribute("groups", "complex", true, false, false, READ_ONLY, DEFAULT, "none", List.of(
                        with(readOnly("value", "string"), "caseExact", true),
                        readOnly("display", "string"),
                        with(with(readOnly("$ref", "reference"), "caseExact", true),
                                "referenceTypes", List.of("Group")),
                        with(readOnly("type", "string"), "canonicalValues", List.of("direct"))))));
        expected.put("meta", Map.of(
                "resourceType", "Schema",
                "location", BASE_URI + "/Schemas/" + ScimSchemas.USER));

        assertThat(ScimUserAttributes.schemaDocument(BASE_URI)).isEqualTo(expected);
    }

    // --- the Group vocabulary, held to the same standard ---------------------------------

    @Test
    void the_group_schema_document_advertises_exactly_the_declared_attributes() {
        assertThat(advertisedNames(ScimGroupAttributes.schemaDocument(BASE_URI)))
                .containsExactlyInAnyOrderElementsOf(ScimGroupAttributes.SCHEMA_ATTRIBUTES.stream()
                        .map(ScimUserAttributes.Attribute::name)
                        .collect(Collectors.toSet()));
    }

    @Test
    void the_accepted_group_write_attributes_are_the_advertised_writable_ones() {
        Set<String> accepted = ScimGroupAttributes.writableNames();

        assertThat(accepted).containsAll(Set.of("schemas", "externalId"));
        assertThat(accepted).containsExactlyInAnyOrderElementsOf(
                withCommonWritables(advertisedWritableNames(ScimGroupAttributes.schemaDocument(BASE_URI))));
    }

    @Test
    void a_common_group_attribute_is_not_declared_by_the_group_schema() {
        assertThat(advertisedNames(ScimGroupAttributes.schemaDocument(BASE_URI)))
                .doesNotContainAnyElementsOf(Set.of("id", "externalId", "meta", "schemas"));
        assertThat(ScimGroupAttributes.projectableNames())
                .containsAll(ScimUserAttributes.COMMON_ATTRIBUTES);
    }

    @Test
    void every_declared_and_common_group_attribute_is_projectable() {
        assertThat(ScimGroupAttributes.projectableNames())
                .containsExactlyInAnyOrderElementsOf(union(
                        advertisedNames(ScimGroupAttributes.schemaDocument(BASE_URI)),
                        ScimUserAttributes.COMMON_ATTRIBUTES));
    }

    /**
     * A Group's {@code displayName} is server-unique, which RFC 7643 does not require. It is
     * advertised so a connector is told rather than discovering it through a 409.
     */
    @Test
    void a_group_display_name_is_required_case_insensitive_and_server_unique() {
        Map<String, Object> displayName =
                advertised(ScimGroupAttributes.schemaDocument(BASE_URI), "displayName");

        assertThat(displayName).containsEntry("required", true);
        assertThat(displayName).containsEntry("caseExact", false);
        assertThat(displayName).containsEntry("uniqueness", "server");
    }

    /**
     * {@code members} is writable at the top level with {@code value} as its only writable
     * sub-attribute, which is the advertised form of "the label is derived, not something you
     * set" — the other end of the same relation the User's read-only {@code groups} view
     * renders.
     */
    @Test
    void group_membership_is_written_by_value_alone() {
        Map<String, Object> members = advertised(ScimGroupAttributes.schemaDocument(BASE_URI), "members");

        assertThat(members).containsEntry("mutability", "readWrite");
        assertThat(members).containsEntry("multiValued", true);
        assertThat(subAttributeNames(members)).containsExactly("value", "display", "$ref", "type");
        assertThat(subAttribute(members, "value")).containsEntry("mutability", "readWrite");
        assertThat(subAttribute(members, "display")).containsEntry("mutability", "readOnly");
        assertThat(subAttribute(members, "$ref")).containsEntry("mutability", "readOnly");
        assertThat(subAttribute(members, "type")).containsEntry("mutability", "readOnly");
    }

    /** The Group document, whole, for the reason the User document is asserted whole. */
    @Test
    void the_rendered_group_schema_document_is_exactly_this() {
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("schemas", List.of(ScimSchemas.SCHEMA));
        expected.put("id", ScimSchemas.GROUP);
        expected.put("name", "Group");
        expected.put("description", "SCIM core Group, as implemented by this service.");
        expected.put("attributes", List.of(
                attribute("displayName", "string", false, true, false, READ_WRITE, DEFAULT, "server",
                        List.of()),
                attribute("members", "complex", true, false, false, READ_WRITE, DEFAULT, "none", List.of(
                        with(writable("value", "string"), "caseExact", true),
                        readOnly("display", "string"),
                        with(with(readOnly("$ref", "reference"), "caseExact", true),
                                "referenceTypes", List.of("User")),
                        with(readOnly("type", "string"), "canonicalValues", List.of("User"))))));
        expected.put("meta", Map.of(
                "resourceType", "Schema",
                "location", BASE_URI + "/Schemas/" + ScimSchemas.GROUP));

        assertThat(ScimGroupAttributes.schemaDocument(BASE_URI)).isEqualTo(expected);
    }

    @Test
    void the_group_document_declares_itself_as_a_schema_at_its_own_location() {
        Map<String, Object> document = ScimGroupAttributes.schemaDocument(BASE_URI);

        assertThat(document).containsEntry("schemas", List.of(ScimSchemas.SCHEMA));
        assertThat(document).containsEntry("id", ScimSchemas.GROUP);
        assertThat(document.get("meta")).isEqualTo(Map.of(
                "resourceType", "Schema",
                "location", BASE_URI + "/Schemas/" + ScimSchemas.GROUP));
    }

    private static final String READ_WRITE = "readWrite";

    private static final String READ_ONLY = "readOnly";

    private static final String DEFAULT = "default";

    /** An ordinary single-valued, optional, case-insensitive, writable attribute. */
    private static Map<String, Object> writable(String name, String type) {
        return attribute(name, type, false, false, false, READ_WRITE, DEFAULT, "none", List.of());
    }

    /** The same shape, declared read-only: a derived value a write may not set. */
    private static Map<String, Object> readOnly(String name, String type) {
        return attribute(name, type, false, false, false, READ_ONLY, DEFAULT, "none", List.of());
    }

    /** {@code rendered} with one characteristic set — replaced in place, or appended after the rest. */
    private static Map<String, Object> with(Map<String, Object> rendered, String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(rendered);
        copy.put(key, value);
        return copy;
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

    private static Set<String> advertisedNames() {
        return advertisedNames(ScimUserAttributes.schemaDocument(BASE_URI));
    }

    private static Set<String> advertisedNames(Map<String, Object> document) {
        return attributes(document).stream()
                .map(attribute -> (String) attribute.get("name"))
                .collect(Collectors.toSet());
    }

    /** The advertised attributes a client may write, read out of the document's mutability. */
    private static Set<String> advertisedWritableNames(Map<String, Object> document) {
        return attributes(document).stream()
                .filter(attribute -> !READ_ONLY.equals(attribute.get("mutability")))
                .map(attribute -> (String) attribute.get("name"))
                .collect(Collectors.toSet());
    }

    /** The two common attributes a write may assert though no resource schema declares them. */
    private static Set<String> withCommonWritables(Set<String> advertised) {
        return union(advertised, Set.of("schemas", "externalId"));
    }

    private static Set<String> union(Set<String> first, Set<String> second) {
        return Stream.concat(first.stream(), second.stream()).collect(Collectors.toSet());
    }

    private static Map<String, Object> advertised(String name) {
        return advertised(ScimUserAttributes.schemaDocument(BASE_URI), name);
    }

    private static Map<String, Object> advertised(Map<String, Object> document, String name) {
        return attributes(document).stream()
                .filter(attribute -> name.equals(attribute.get("name")))
                .findFirst()
                .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> attributes(Map<String, Object> document) {
        return (List<Map<String, Object>>) document.get("attributes");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> subAttributes(Map<String, Object> attribute) {
        return (List<Map<String, Object>>) attribute.get("subAttributes");
    }

    private static List<String> subAttributeNames(Map<String, Object> attribute) {
        return subAttributes(attribute).stream()
                .map(sub -> (String) sub.get("name"))
                .toList();
    }

    private static Map<String, Object> subAttribute(Map<String, Object> attribute, String name) {
        return subAttributes(attribute).stream()
                .filter(sub -> name.equals(sub.get("name")))
                .findFirst()
                .orElseThrow();
    }
}
