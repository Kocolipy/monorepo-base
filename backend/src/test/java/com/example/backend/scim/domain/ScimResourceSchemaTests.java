package com.example.backend.scim.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.backend.scim.domain.ScimAttribute.Mutability;
import com.example.backend.scim.domain.ScimAttribute.Returned;
import com.example.backend.scim.domain.ScimAttribute.Uniqueness;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The attribute definition, pinned by independent expectations.
 *
 * <p>Every expected value here is written out by hand from this directory's profile rather than
 * read off the definition, so a fact that changed in the definition — and therefore everywhere
 * that derives from it at once — still fails a test.
 */
class ScimResourceSchemaTests {

    private static final ScimResourceSchema USER = ScimResourceSchema.of(ScimResourceType.USER);

    private static final ScimResourceSchema GROUP = ScimResourceSchema.of(ScimResourceType.GROUP);

    @Test
    void each_schema_is_of_its_own_type() {
        assertThat(USER.type()).isEqualTo(ScimResourceType.USER);
        assertThat(GROUP.type()).isEqualTo(ScimResourceType.GROUP);
    }

    @Test
    void the_user_schema_declares_its_attributes_in_rfc_order() {
        assertThat(USER.attributes()).extracting(ScimAttribute::name).containsExactly(
                "userName", "name", "displayName", "preferredLanguage", "locale", "timezone",
                "active", "password", "emails", "groups");
    }

    @Test
    void the_group_schema_declares_a_name_and_a_membership() {
        assertThat(GROUP.attributes()).extracting(ScimAttribute::name)
                .containsExactly("displayName", "members");
    }

    /** The common attributes belong to every resource and to neither schema's declared list. */
    @ParameterizedTest
    @EnumSource(ScimResourceType.class)
    void the_common_attributes_are_every_resources_and_no_schemas(ScimResourceType type) {
        ScimResourceSchema schema = ScimResourceSchema.of(type);

        assertThat(ScimResourceSchema.COMMON).extracting(ScimAttribute::name)
                .containsExactly("schemas", "id", "externalId", "meta");
        assertThat(schema.attributes()).extracting(ScimAttribute::name)
                .doesNotContain("schemas", "id", "externalId", "meta");
        assertThat(schema.allAttributes()).containsSequence(ScimResourceSchema.COMMON);
        assertThat(schema.allAttributes()).containsSubsequence(schema.attributes());
        assertThat(schema.allAttributes())
                .hasSize(ScimResourceSchema.COMMON.size() + schema.attributes().size());
    }

    @Test
    void names_are_the_common_and_declared_attributes() {
        assertThat(USER.names()).containsExactlyInAnyOrder(
                "schemas", "id", "externalId", "meta", "userName", "name", "displayName",
                "preferredLanguage", "locale", "timezone", "active", "password", "emails",
                "groups");
        assertThat(GROUP.names()).containsExactlyInAnyOrder(
                "schemas", "id", "externalId", "meta", "displayName", "members");
    }

    /** A credential is writable: write-only is not read-only. */
    @Test
    void the_writable_names_include_the_write_only_password_and_the_callers_alias() {
        assertThat(USER.writableNames()).containsExactlyInAnyOrder(
                "externalId", "userName", "name", "displayName", "preferredLanguage", "locale",
                "timezone", "active", "password", "emails");
        assertThat(GROUP.writableNames())
                .containsExactlyInAnyOrder("externalId", "displayName", "members");
    }

    @Test
    void the_read_only_names_are_the_common_ones_and_on_a_user_the_groups_view() {
        assertThat(USER.readOnlyNames()).containsExactlyInAnyOrder("schemas", "id", "meta", "groups");
        assertThat(GROUP.readOnlyNames()).containsExactlyInAnyOrder("schemas", "id", "meta");
    }

    @ParameterizedTest
    @EnumSource(ScimResourceType.class)
    void schemas_and_id_are_the_always_returned_attributes(ScimResourceType type) {
        assertThat(ScimResourceSchema.of(type).alwaysReturnedNames())
                .containsExactlyInAnyOrder("schemas", "id");
    }

    @ParameterizedTest
    @ValueSource(strings = {"userName", "USERNAME", "username", "externalid", "META"})
    void an_attribute_is_found_by_its_name_case_insensitively(String name) {
        assertThat(USER.find(name)).isPresent();
        assertThat(USER.find(name).orElseThrow().name()).isEqualToIgnoringCase(name);
    }

    @Test
    void an_attribute_is_found_only_on_the_type_that_has_it() {
        assertThat(USER.find("members")).isEmpty();
        assertThat(GROUP.find("groups")).isEmpty();
        assertThat(GROUP.find("password")).isEmpty();
        assertThat(USER.find("nickName")).isEmpty();
        assertThat(GROUP.find("id")).isPresent();
    }

    // --- representative attributes, characteristic by characteristic -------------------------

    @Test
    void user_name_is_a_required_case_insensitive_server_unique_string() {
        assertThat(USER.find("userName").orElseThrow()).isEqualTo(new ScimAttribute(
                "userName", ScimAttributeType.STRING, false, true, false, Mutability.READ_WRITE,
                Returned.DEFAULT, Uniqueness.SERVER, List.of(), List.of(), List.of()));
    }

    @Test
    void active_is_a_single_valued_writable_boolean() {
        assertThat(USER.find("active").orElseThrow()).isEqualTo(new ScimAttribute(
                "active", ScimAttributeType.BOOLEAN, false, false, false, Mutability.READ_WRITE,
                Returned.DEFAULT, Uniqueness.NONE, List.of(), List.of(), List.of()));
    }

    @Test
    void the_password_is_a_case_exact_write_only_string_never_returned() {
        ScimAttribute password = USER.find("password").orElseThrow();

        assertThat(password).isEqualTo(new ScimAttribute(
                "password", ScimAttributeType.STRING, false, false, true, Mutability.WRITE_ONLY,
                Returned.NEVER, Uniqueness.NONE, List.of(), List.of(), List.of()));
        assertThat(password.isWritable()).isTrue();
        assertThat(password.isReturned()).isFalse();
    }

    @Test
    void name_is_a_single_valued_complex_attribute_of_six_strings() {
        ScimAttribute name = USER.find("name").orElseThrow();

        assertThat(name.type()).isEqualTo(ScimAttributeType.COMPLEX);
        assertThat(name.multiValued()).isFalse();
        assertThat(name.subAttributes()).extracting(ScimAttribute::name).containsExactly(
                "formatted", "familyName", "givenName", "middleName", "honorificPrefix",
                "honorificSuffix");
        assertThat(name.subAttributes()).allSatisfy(sub -> {
            assertThat(sub.type()).isEqualTo(ScimAttributeType.STRING);
            assertThat(sub.caseExact()).isFalse();
            assertThat(sub.mutability()).isEqualTo(Mutability.READ_WRITE);
        });
    }

    @Test
    void emails_are_multi_valued_with_a_canonical_type_and_a_boolean_primary() {
        ScimAttribute emails = USER.find("emails").orElseThrow();

        assertThat(emails.type()).isEqualTo(ScimAttributeType.COMPLEX);
        assertThat(emails.multiValued()).isTrue();
        assertThat(emails.mutability()).isEqualTo(Mutability.READ_WRITE);
        assertThat(emails.subAttribute("type").orElseThrow().canonicalValues())
                .containsExactly("work", "home", "other");
        assertThat(emails.subAttribute("primary").orElseThrow().type())
                .isEqualTo(ScimAttributeType.BOOLEAN);
        assertThat(emails.subAttribute("value").orElseThrow().caseExact()).isFalse();
    }

    @Test
    void the_groups_view_is_wholly_read_only_and_its_ids_are_case_exact() {
        ScimAttribute groups = USER.find("groups").orElseThrow();

        assertThat(groups.multiValued()).isTrue();
        assertThat(groups.isWritable()).isFalse();
        assertThat(groups.subAttributes()).allSatisfy(sub -> assertThat(sub.isWritable()).isFalse());
        assertThat(groups.subAttribute("value").orElseThrow().caseExact()).isTrue();
        assertThat(groups.subAttribute("$ref").orElseThrow()).isEqualTo(new ScimAttribute(
                "$ref", ScimAttributeType.REFERENCE, false, false, true, Mutability.READ_ONLY,
                Returned.DEFAULT, Uniqueness.NONE, List.of(), List.of(), List.of("Group")));
        assertThat(groups.subAttribute("type").orElseThrow().canonicalValues())
                .containsExactly("direct");
        assertThat(groups.subAttribute("display").orElseThrow().caseExact()).isFalse();
    }

    @Test
    void a_group_display_name_is_required_and_server_unique() {
        assertThat(GROUP.find("displayName").orElseThrow()).isEqualTo(new ScimAttribute(
                "displayName", ScimAttributeType.STRING, false, true, false,
                Mutability.READ_WRITE, Returned.DEFAULT, Uniqueness.SERVER,
                List.of(), List.of(), List.of()));
    }

    @Test
    void a_user_display_name_is_neither_required_nor_unique() {
        ScimAttribute displayName = USER.find("displayName").orElseThrow();

        assertThat(displayName.required()).isFalse();
        assertThat(displayName.uniqueness()).isEqualTo(Uniqueness.NONE);
    }

    @Test
    void members_are_written_by_value_alone_and_reference_users() {
        ScimAttribute members = GROUP.find("members").orElseThrow();

        assertThat(members.multiValued()).isTrue();
        assertThat(members.isWritable()).isTrue();
        assertThat(members.subAttributes()).extracting(ScimAttribute::name)
                .containsExactly("value", "display", "$ref", "type");
        assertThat(members.subAttributes()).extracting(ScimAttribute::isWritable)
                .containsExactly(true, false, false, false);
        assertThat(members.subAttribute("value").orElseThrow().caseExact()).isTrue();
        assertThat(members.subAttribute("$ref").orElseThrow().referenceTypes())
                .containsExactly("User");
        assertThat(members.subAttribute("type").orElseThrow().canonicalValues())
                .containsExactly("User");
    }

    @Test
    void the_common_identifiers_are_case_exact_and_only_external_id_is_writable() {
        ScimAttribute schemas = USER.find("schemas").orElseThrow();
        ScimAttribute id = USER.find("id").orElseThrow();
        ScimAttribute externalId = USER.find("externalId").orElseThrow();
        ScimAttribute meta = USER.find("meta").orElseThrow();

        assertThat(schemas).isEqualTo(new ScimAttribute(
                "schemas", ScimAttributeType.REFERENCE, true, true, true, Mutability.READ_ONLY,
                Returned.ALWAYS, Uniqueness.NONE, List.of(), List.of(), List.of()));
        assertThat(id).isEqualTo(new ScimAttribute(
                "id", ScimAttributeType.STRING, false, false, true, Mutability.READ_ONLY,
                Returned.ALWAYS, Uniqueness.NONE, List.of(), List.of(), List.of()));
        assertThat(externalId).isEqualTo(new ScimAttribute(
                "externalId", ScimAttributeType.STRING, false, false, true,
                Mutability.READ_WRITE, Returned.DEFAULT, Uniqueness.NONE,
                List.of(), List.of(), List.of()));
        assertThat(meta.type()).isEqualTo(ScimAttributeType.COMPLEX);
        assertThat(meta.caseExact()).isTrue();
        assertThat(meta.isWritable()).isFalse();
        assertThat(meta.returned()).isEqualTo(Returned.DEFAULT);
        assertThat(meta.subAttributes()).extracting(ScimAttribute::name).containsExactly(
                "resourceType", "created", "lastModified", "location", "version");
        assertThat(meta.subAttributes()).extracting(ScimAttribute::type).containsExactly(
                ScimAttributeType.STRING, ScimAttributeType.DATE_TIME,
                ScimAttributeType.DATE_TIME, ScimAttributeType.REFERENCE,
                ScimAttributeType.STRING);
        assertThat(meta.subAttributes()).allSatisfy(sub -> {
            assertThat(sub.caseExact()).isTrue();
            assertThat(sub.isWritable()).isFalse();
        });
    }

    // --- the record itself --------------------------------------------------------------------

    @Test
    void a_sub_attribute_is_found_case_insensitively_and_an_unknown_one_is_not() {
        ScimAttribute emails = USER.find("emails").orElseThrow();

        assertThat(emails.subAttribute("PRIMARY").orElseThrow().name()).isEqualTo("primary");
        assertThat(emails.subAttribute("label")).isEmpty();
        assertThat(USER.find("active").orElseThrow().subAttribute("value")).isEmpty();
    }

    @Test
    void the_record_holds_copies_of_the_lists_it_was_given() {
        List<ScimAttribute> sub = new ArrayList<>(List.of(
                ScimAttribute.singular("value", ScimAttributeType.STRING,
                        Mutability.READ_WRITE, Returned.DEFAULT)));
        List<String> canonical = new ArrayList<>(List.of("work"));
        List<String> references = new ArrayList<>(List.of("User"));
        ScimAttribute attribute = new ScimAttribute("x", ScimAttributeType.COMPLEX, true, false,
                false, Mutability.READ_WRITE, Returned.DEFAULT, Uniqueness.NONE,
                sub, canonical, references);

        sub.clear();
        canonical.clear();
        references.clear();

        assertThat(attribute.subAttributes()).hasSize(1);
        assertThat(attribute.canonicalValues()).containsExactly("work");
        assertThat(attribute.referenceTypes()).containsExactly("User");
        assertThatThrownBy(() -> attribute.subAttributes().add(attribute))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    /** Each modifier states one departure from the RFC 7643 §2.2 defaults and keeps the rest. */
    @Test
    void each_modifier_changes_exactly_one_characteristic() {
        ScimAttribute plain = ScimAttribute.singular(
                "x", ScimAttributeType.REFERENCE, Mutability.READ_ONLY, Returned.ALWAYS);
        ScimAttribute all = plain.asRequired().asCaseExact().unique(Uniqueness.SERVER)
                .canonical("a", "b").references("User");

        assertThat(plain).isEqualTo(new ScimAttribute("x", ScimAttributeType.REFERENCE, false,
                false, false, Mutability.READ_ONLY, Returned.ALWAYS, Uniqueness.NONE,
                List.of(), List.of(), List.of()));
        assertThat(all).isEqualTo(new ScimAttribute("x", ScimAttributeType.REFERENCE, false,
                true, true, Mutability.READ_ONLY, Returned.ALWAYS, Uniqueness.SERVER,
                List.of(), List.of("a", "b"), List.of("User")));
        ScimAttribute complex = ScimAttribute.complex(
                "c", Mutability.READ_WRITE, Returned.DEFAULT, List.of(plain));
        ScimAttribute multi = ScimAttribute.multiValuedComplex(
                "m", Mutability.READ_WRITE, Returned.NEVER, List.of(plain));
        ScimAttribute multiModified = multi.asRequired().asCaseExact().unique(Uniqueness.SERVER)
                .canonical("v").references("Group");
        assertThat(complex).isEqualTo(new ScimAttribute("c", ScimAttributeType.COMPLEX, false,
                false, false, Mutability.READ_WRITE, Returned.DEFAULT, Uniqueness.NONE,
                List.of(plain), List.of(), List.of()));
        assertThat(multi).isEqualTo(new ScimAttribute("m", ScimAttributeType.COMPLEX, true,
                false, false, Mutability.READ_WRITE, Returned.NEVER, Uniqueness.NONE,
                List.of(plain), List.of(), List.of()));
        assertThat(multiModified).isEqualTo(new ScimAttribute("m", ScimAttributeType.COMPLEX,
                true, true, true, Mutability.READ_WRITE, Returned.NEVER, Uniqueness.SERVER,
                List.of(plain), List.of("v"), List.of("Group")));
    }

    @Test
    void writable_means_not_read_only_and_returned_means_not_never() {
        assertThat(ScimAttribute.singular("a", ScimAttributeType.STRING,
                Mutability.READ_WRITE, Returned.DEFAULT).isWritable()).isTrue();
        assertThat(ScimAttribute.singular("a", ScimAttributeType.STRING,
                Mutability.WRITE_ONLY, Returned.NEVER).isWritable()).isTrue();
        assertThat(ScimAttribute.singular("a", ScimAttributeType.STRING,
                Mutability.READ_ONLY, Returned.ALWAYS).isWritable()).isFalse();
        assertThat(ScimAttribute.singular("a", ScimAttributeType.STRING,
                Mutability.READ_ONLY, Returned.ALWAYS).isReturned()).isTrue();
        assertThat(ScimAttribute.singular("a", ScimAttributeType.STRING,
                Mutability.READ_ONLY, Returned.DEFAULT).isReturned()).isTrue();
        assertThat(ScimAttribute.singular("a", ScimAttributeType.STRING,
                Mutability.WRITE_ONLY, Returned.NEVER).isReturned()).isFalse();
    }
}
