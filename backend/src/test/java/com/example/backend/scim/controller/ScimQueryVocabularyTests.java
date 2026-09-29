package com.example.backend.scim.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.scim.domain.ScimFilterPath;
import com.example.backend.scim.domain.ScimQueryVocabulary;
import com.example.backend.scim.domain.ScimQueryVocabulary.Attribute;
import com.example.backend.scim.domain.ScimQueryVocabulary.Kind;
import com.example.backend.scim.domain.ScimResourceType;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * What a filter compares by is what {@code /Schemas} advertises.
 *
 * <p>A connector builds its filters from discovery — whether {@code displayName} is case-exact,
 * whether {@code active} is a boolean — so the query vocabulary and the schema documents must be
 * the same claim. They live in different layers (the domain cannot read a web adapter's schema
 * list), so this test is what makes them one: every advertised attribute is queryable with the
 * advertised type, case sensitivity and cardinality, and nothing is queryable that is not
 * advertised, apart from the RFC 7643 §3.1 common attributes every resource has.
 */
class ScimQueryVocabularyTests {

    /** Queryable although no schema document declares them: RFC 7643 §3.1 common attributes. */
    private static final Set<ScimFilterPath> COMMON = Set.of(
            ScimFilterPath.ID, ScimFilterPath.EXTERNAL_ID, ScimFilterPath.META,
            ScimFilterPath.META_RESOURCE_TYPE, ScimFilterPath.META_CREATED,
            ScimFilterPath.META_LAST_MODIFIED, ScimFilterPath.META_LOCATION,
            ScimFilterPath.META_VERSION);

    @ParameterizedTest
    @EnumSource(ScimResourceType.class)
    void every_advertised_attribute_is_queryable_as_advertised_and_nothing_else_is(
            ScimResourceType type) {
        List<ScimUserAttributes.Attribute> advertised = type == ScimResourceType.USER
                ? ScimUserAttributes.SCHEMA_ATTRIBUTES
                : ScimGroupAttributes.SCHEMA_ATTRIBUTES;
        ScimQueryVocabulary vocabulary = ScimQueryVocabulary.of(type);
        Set<ScimFilterPath> covered = new HashSet<>(COMMON);

        for (ScimUserAttributes.Attribute attribute : advertised) {
            ScimFilterPath path = ScimFilterPath.of(attribute.name(), null).orElseThrow();
            if (path == ScimFilterPath.PASSWORD) {
                assertThat(vocabulary.find(path)).as("the password is advertised, never queryable")
                        .isEmpty();
                continue;
            }
            assertAgrees(vocabulary.find(path), attribute, false);
            covered.add(path);
            for (ScimUserAttributes.Attribute sub : attribute.subAttributes()) {
                ScimFilterPath subPath = path.subAttribute(sub.name()).orElseThrow();
                assertAgrees(vocabulary.find(subPath), sub, attribute.multiValued());
                covered.add(subPath);
            }
        }
        assertThat(vocabulary.paths()).containsExactlyInAnyOrderElementsOf(covered);
    }

    private static void assertAgrees(
            Optional<Attribute> queryable, ScimUserAttributes.Attribute advertised, boolean inMulti) {
        assertThat(queryable).as(advertised.name()).isPresent();
        Attribute attribute = queryable.get();
        assertThat(kind(attribute.kind())).as(advertised.name()).isEqualTo(advertised.type());
        assertThat(attribute.multiValued()).as(advertised.name())
                .isEqualTo(advertised.multiValued() || inMulti);
        if (attribute.isTextual()) {
            assertThat(attribute.caseExact()).as(advertised.name() + " caseExact")
                    .isEqualTo(advertised.caseExact());
        }
    }

    private static String kind(Kind kind) {
        return Map.of(
                Kind.STRING, "string", Kind.BOOLEAN, "boolean", Kind.DATE_TIME, "dateTime",
                Kind.REFERENCE, "reference", Kind.COMPLEX, "complex").get(kind);
    }

    /** A path both types have compares the same way on both, so a base search has one meaning. */
    @Test
    void a_path_both_types_have_is_the_same_kind_on_both() {
        ScimQueryVocabulary users = ScimQueryVocabulary.of(ScimResourceType.USER);
        ScimQueryVocabulary groups = ScimQueryVocabulary.of(ScimResourceType.GROUP);
        Set<ScimFilterPath> shared = new HashSet<>(users.paths());
        shared.retainAll(groups.paths());

        assertThat(shared).containsExactlyInAnyOrderElementsOf(
                union(COMMON, Set.of(ScimFilterPath.DISPLAY_NAME)));
        for (ScimFilterPath path : shared) {
            assertThat(users.find(path).orElseThrow().kind()).as(path.canonical())
                    .isEqualTo(groups.find(path).orElseThrow().kind());
        }
    }

    @Test
    void the_common_attributes_have_their_rfc_types_and_the_ids_are_case_exact() {
        for (ScimResourceType type : ScimResourceType.values()) {
            ScimQueryVocabulary vocabulary = ScimQueryVocabulary.of(type);
            assertThat(vocabulary.find(ScimFilterPath.ID).orElseThrow().caseExact()).isTrue();
            assertThat(vocabulary.find(ScimFilterPath.EXTERNAL_ID).orElseThrow().caseExact()).isTrue();
            assertThat(vocabulary.find(ScimFilterPath.META).orElseThrow().isComplex()).isTrue();
            assertThat(vocabulary.find(ScimFilterPath.META_CREATED).orElseThrow().kind())
                    .isEqualTo(Kind.DATE_TIME);
            assertThat(vocabulary.find(ScimFilterPath.META_LOCATION).orElseThrow().isTextual())
                    .isTrue();
        }
    }

    /** The schema URI a filter path is qualified with is the one the adapter renders. */
    @Test
    void each_types_schema_uri_is_the_rendered_schema() {
        assertThat(ScimResourceType.USER.schemaUri()).isEqualTo(ScimSchemas.USER);
        assertThat(ScimResourceType.GROUP.schemaUri()).isEqualTo(ScimSchemas.GROUP);
    }

    private static Set<ScimFilterPath> union(Set<ScimFilterPath> a, Set<ScimFilterPath> b) {
        Set<ScimFilterPath> union = new HashSet<>(a);
        union.addAll(b);
        return union;
    }
}
