package com.example.backend.scim.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** A Group as the directory holds it: a name, a de-duplicated direct-User membership, a version. */
class ScimGroupTests {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private static final UUID ALICE = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID BOB = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Test
    void a_null_id_is_refused() {
        assertThatThrownBy(() -> ScimGroup.created(null, "Admins", List.of(), NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("has a stable id");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void a_blank_display_name_is_refused(String blank) {
        assertThatThrownBy(() -> ScimGroup.created(ID, blank, List.of(), NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("has a displayName");
    }

    @Test
    void a_version_below_one_is_refused_because_no_client_could_hold_that_etag() {
        assertThatThrownBy(() -> new ScimGroup(ID, "Admins", List.of(), null, 0, NOW, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("starts at 1");
    }

    @Test
    void a_created_group_starts_at_version_one_with_both_timestamps_equal() {
        ScimGroup group = ScimGroup.created(ID, "Admins", List.of(), NOW);

        assertThat(group.version()).isEqualTo(ScimUser.INITIAL_VERSION);
        assertThat(group.createdAt()).isEqualTo(NOW);
        assertThat(group.lastModifiedAt()).isEqualTo(NOW);
    }

    /**
     * No factory produces a reserved Group, and that is the guarantee rather than a gap: a
     * provisioning path that could reserve one could claim the Admin group's authority.
     */
    @Test
    void a_created_group_is_unreserved_and_so_not_protected() {
        ScimGroup group = ScimGroup.created(ID, "Engineering", List.of(), NOW);

        assertThat(group.reservedName()).isNull();
        assertThat(group.isProtectedFromWrites()).isFalse();
    }

    @Test
    void a_reserved_group_is_protected_from_writes() {
        ScimGroup group = new ScimGroup(
                ID, "Admins", List.of(), ReservedResourceName.ADMIN_GROUP, 1, NOW, NOW);

        assertThat(group.isProtectedFromWrites()).isTrue();
    }

    @Test
    void null_members_become_an_empty_list_rather_than_staying_null() {
        assertThat(ScimGroup.created(ID, "Admins", null, NOW).members()).isEmpty();
    }

    /**
     * Keyed on the referenced id alone, so two entries naming the same User collapse even when
     * their read-only labels differ — which a connector may send, and which is not stored.
     */
    @Test
    void repeated_members_collapse_to_the_first_mention_whatever_label_they_carry() {
        ScimGroup group = ScimGroup.created(ID, "Admins", List.of(
                new ScimGroupMember(ALICE, "Alice"),
                new ScimGroupMember(ALICE, "a different label"),
                new ScimGroupMember(BOB, "Bob")), NOW);

        assertThat(group.members()).containsExactly(
                new ScimGroupMember(ALICE, "Alice"), new ScimGroupMember(BOB, "Bob"));
    }

    @Test
    void membership_is_reported_by_the_referenced_user_id() {
        ScimGroup group = ScimGroup.created(
                ID, "Admins", List.of(ScimGroupMember.reference(ALICE)), NOW);

        assertThat(group.hasMember(ALICE)).isTrue();
        assertThat(group.hasMember(BOB)).isFalse();
    }

    @Test
    void the_uniqueness_form_is_the_normalized_display_name() {
        assertThat(ScimGroup.created(ID, "Admins", List.of(), NOW).normalizedDisplayName())
                .isEqualTo(NormalizedDisplayName.of("admins"));
    }

    @Test
    void a_replacement_states_the_new_name_and_membership_and_moves_last_modified() {
        Instant later = NOW.plusSeconds(60);

        ScimGroup replaced = ScimGroup.created(ID, "Admins", List.of(), NOW)
                .replacedWith("Administrators", List.of(ScimGroupMember.reference(ALICE)), later);

        assertThat(replaced.displayName()).isEqualTo("Administrators");
        assertThat(replaced.hasMember(ALICE)).isTrue();
        assertThat(replaced.lastModifiedAt()).isEqualTo(later);
        assertThat(replaced.createdAt()).isEqualTo(NOW);
    }

    /**
     * The version is NOT advanced by the transition. A membership change advances other resources'
     * versions too, and an increment computed here could be lost by a concurrent one, so the port
     * owns it and returns the stored result. A test that expected version + 1 here would be
     * asserting the bug.
     */
    @Test
    void a_replacement_leaves_the_version_for_the_port_to_advance() {
        ScimGroup stored = new ScimGroup(ID, "Admins", List.of(), null, 7, NOW, NOW);

        assertThat(stored.replacedWith("Administrators", List.of(), NOW).version()).isEqualTo(7);
    }

    @Test
    void a_replacement_cannot_reserve_or_unreserve_a_group() {
        ScimGroup reserved = new ScimGroup(
                ID, "Admins", List.of(), ReservedResourceName.ADMIN_GROUP, 1, NOW, NOW);

        assertThat(reserved.replacedWith("Admins", List.of(), NOW).reservedName())
                .isEqualTo(ReservedResourceName.ADMIN_GROUP);
    }

    @Test
    void a_replacement_de_duplicates_its_membership_like_a_create_does() {
        ScimGroup replaced = ScimGroup.created(ID, "Admins", List.of(), NOW)
                .replacedWith("Admins", List.of(
                        ScimGroupMember.reference(ALICE),
                        ScimGroupMember.reference(ALICE)), NOW);

        assertThat(replaced.members()).hasSize(1);
    }

    @Test
    void a_membership_reference_names_a_user_and_carries_no_label() {
        ScimGroupMember member = ScimGroupMember.reference(ALICE);

        assertThat(member.userId()).isEqualTo(ALICE);
        assertThat(member.display()).isNull();
    }

    @Test
    void a_membership_with_no_user_is_refused() {
        assertThatThrownBy(() -> ScimGroupMember.reference(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("names a User");
    }
}
