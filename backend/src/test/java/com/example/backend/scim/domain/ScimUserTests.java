package com.example.backend.scim.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The directory's own User record: its invariants, and the credential it may not have. */
class ScimUserTests {

    private static final UUID ID = UUID.fromString("8a5c1f4e-0000-4000-8000-000000000001");
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private static ScimUserProfile profile() {
        return new ScimUserProfile(
                "bjensen", ScimName.NONE, null, null, null, null, true, List.of());
    }

    @Test
    void a_created_user_starts_at_version_one_with_one_timestamp_for_both_fields() {
        ScimUser user = ScimUser.created(ID, profile(), "hash", NOW);

        assertThat(user.version()).isEqualTo(ScimUser.INITIAL_VERSION);
        assertThat(user.version()).isEqualTo(1L);
        assertThat(user.createdAt()).isEqualTo(NOW);
        assertThat(user.lastModifiedAt()).isEqualTo(NOW);
        assertThat(user.id()).isEqualTo(ID);
    }

    @Test
    void a_user_without_a_stable_id_is_refused() {
        assertThatThrownBy(() -> new ScimUser(null, profile(), "hash", 1L, NOW, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stable id");
    }

    @Test
    void a_user_without_a_profile_is_refused() {
        assertThatThrownBy(() -> new ScimUser(ID, null, "hash", 1L, NOW, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("has a profile");
    }

    @Test
    void a_version_below_one_is_refused_because_every_live_resource_has_an_etag() {
        assertThatThrownBy(() -> new ScimUser(ID, profile(), "hash", 0L, NOW, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("starts at 1");
        assertThatThrownBy(() -> new ScimUser(ID, profile(), "hash", -1L, NOW, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("starts at 1");
    }

    @Test
    void version_one_is_accepted_as_the_first_live_version() {
        assertThat(new ScimUser(ID, profile(), "hash", 1L, NOW, NOW).version()).isEqualTo(1L);
    }

    /**
     * The credentialless User is a supported state, not an incomplete one: SCIM allows a
     * create with no {@code password}, and such a User exists and can be read while being
     * unable to authenticate. {@code hasPassword} is how that is answered without the hash
     * leaving the slice.
     */
    @Test
    void a_user_with_a_hash_has_a_password() {
        assertThat(ScimUser.created(ID, profile(), "hash", NOW).hasPassword()).isTrue();
    }

    @Test
    void a_credentialless_user_has_no_password() {
        assertThat(ScimUser.created(ID, profile(), null, NOW).hasPassword()).isFalse();
    }

    @Test
    void the_uniqueness_form_of_the_user_name_comes_from_the_profile() {
        ScimUser user = ScimUser.created(ID, profile(), null, NOW);

        assertThat(user.normalizedUserName()).isEqualTo(profile().normalizedUserName());
        assertThat(user.normalizedUserName()).isEqualTo(NormalizedUserName.of("BJensen"));
    }
}
