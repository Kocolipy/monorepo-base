package com.example.backend.scim.infrastructure.persistence.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The equality contract of the two JPA composite keys in this package.
 *
 * <p>These exist because Hibernate keys its persistence context by them: two reads of the same
 * membership must resolve to one managed instance, and a key whose {@code equals} ignored one of
 * its two columns would collapse distinct rows onto each other — a membership of the wrong Group,
 * or an {@code externalId} alias attributed to the wrong connector. Neither failure is visible
 * from an integration test, because the wrong answer is still a well-formed one.
 *
 * <p>Tested rather than justified for exactly that reason. Both classes previously had every
 * {@code equals} branch unreached by any test: nothing in the application calls them in Java, so
 * only a direct test can reach them, and "the framework calls it" is not evidence that it does
 * the right thing when it does.
 *
 * <p>Each nested class walks all four branches — same instance, wrong type, one field differing,
 * the other field differing — because a two-field {@code equals} has a distinct way to be wrong
 * for each field, and an assertion on only one of them passes against a key that ignores the
 * other.
 */
class CompositeKeyContractTests {

    private static final UUID FIRST = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    private static final UUID SECOND = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

    private static final UUID THIRD = UUID.fromString("00000000-0000-0000-0000-0000000000c3");

    @Nested
    class GroupMembership {

        @Test
        void a_key_equals_itself_and_an_identical_key() {
            ScimGroupMemberId key = new ScimGroupMemberId(FIRST, SECOND);

            assertThat(key).isEqualTo(key).isEqualTo(new ScimGroupMemberId(FIRST, SECOND));
            assertThat(key.hashCode()).isEqualTo(new ScimGroupMemberId(FIRST, SECOND).hashCode());
        }

        /** The same User in a different Group is a different membership. */
        @Test
        void a_differing_group_makes_a_different_key() {
            assertThat(new ScimGroupMemberId(FIRST, SECOND))
                    .isNotEqualTo(new ScimGroupMemberId(THIRD, SECOND));
            // Not a requirement of the equals/hashCode contract, but of the key's use: Hibernate's
            // persistence context keys entities by it, and a constant hash degrades every lookup
            // across a large membership to a linear scan.
            assertThat(new ScimGroupMemberId(FIRST, SECOND).hashCode())
                    .isNotEqualTo(new ScimGroupMemberId(THIRD, SECOND).hashCode());
        }

        /** A different User in the same Group is a different membership. */
        @Test
        void a_differing_user_makes_a_different_key() {
            assertThat(new ScimGroupMemberId(FIRST, SECOND))
                    .isNotEqualTo(new ScimGroupMemberId(FIRST, THIRD));
        }

        @Test
        void a_value_of_another_type_is_not_equal() {
            assertThat(new ScimGroupMemberId(FIRST, SECOND))
                    .isNotEqualTo(new ScimExternalIdEntity.Key(FIRST, SECOND))
                    .isNotEqualTo(null)
                    .isNotEqualTo("not a key");
        }

        /**
         * The property Hibernate actually relies on: equal keys collide in a hash container, so
         * two reads of one membership resolve to one entry rather than two.
         */
        @Test
        void equal_keys_are_one_entry_in_a_hash_container() {
            Map<ScimGroupMemberId, String> byKey = new java.util.HashMap<>();
            byKey.put(new ScimGroupMemberId(FIRST, SECOND), "first");
            byKey.put(new ScimGroupMemberId(FIRST, SECOND), "second");
            byKey.put(new ScimGroupMemberId(FIRST, THIRD), "other");

            assertThat(byKey).hasSize(2);
            assertThat(byKey.get(new ScimGroupMemberId(FIRST, SECOND))).isEqualTo("second");
        }
    }

    @Nested
    class ExternalIdAlias {

        @Test
        void a_key_equals_itself_and_an_identical_key() {
            ScimExternalIdEntity.Key key = new ScimExternalIdEntity.Key(FIRST, SECOND);

            assertThat(key).isEqualTo(key).isEqualTo(new ScimExternalIdEntity.Key(FIRST, SECOND));
            assertThat(key.hashCode())
                    .isEqualTo(new ScimExternalIdEntity.Key(FIRST, SECOND).hashCode());
        }

        /**
         * The alias is connector-scoped, so the connector half of the key is load-bearing: two
         * connectors may hold an alias for the same resource and those are separate rows.
         */
        @Test
        void a_differing_connector_makes_a_different_key() {
            assertThat(new ScimExternalIdEntity.Key(FIRST, SECOND))
                    .isNotEqualTo(new ScimExternalIdEntity.Key(THIRD, SECOND));
        }

        @Test
        void a_differing_resource_makes_a_different_key() {
            assertThat(new ScimExternalIdEntity.Key(FIRST, SECOND))
                    .isNotEqualTo(new ScimExternalIdEntity.Key(FIRST, THIRD));
        }

        @Test
        void a_value_of_another_type_is_not_equal() {
            assertThat(new ScimExternalIdEntity.Key(FIRST, SECOND))
                    .isNotEqualTo(new ScimGroupMemberId(FIRST, SECOND))
                    .isNotEqualTo(null)
                    .isNotEqualTo("not a key");
        }

        @Test
        void equal_keys_are_one_entry_in_a_hash_container() {
            Map<ScimExternalIdEntity.Key, String> byKey = new java.util.HashMap<>();
            byKey.put(new ScimExternalIdEntity.Key(FIRST, SECOND), "first");
            byKey.put(new ScimExternalIdEntity.Key(FIRST, SECOND), "second");

            assertThat(byKey).hasSize(1);
        }
    }
}
