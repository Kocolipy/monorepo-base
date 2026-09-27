package com.example.backend.scim.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * The marker that makes a resource one the deployment reserves for its own recovery.
 *
 * <p>The stored values are pinned by literal, not derived from {@link Enum#name()}, because the
 * database's own CHECK constraint lists exactly these two strings: a Java rename that changed the
 * column's contents would make every existing reserved row unrecognisable, which is a silent loss
 * of both the write protection and the lockout exemption.
 */
class ReservedResourceNameTests {

    @Test
    void the_stored_values_are_the_two_strings_the_schema_constrains_the_column_to() {
        assertThat(ReservedResourceName.BOOTSTRAP_ADMIN.storedValue()).isEqualTo("bootstrap-admin");
        assertThat(ReservedResourceName.ADMIN_GROUP.storedValue()).isEqualTo("admin-group");
    }

    @Test
    void a_null_column_is_an_ordinary_unreserved_resource() {
        assertThat(ReservedResourceName.ofStoredValue(null)).isEmpty();
    }

    @Test
    void a_stored_value_round_trips_to_its_reservation() {
        for (ReservedResourceName reserved : ReservedResourceName.values()) {
            assertThat(ReservedResourceName.ofStoredValue(reserved.storedValue()))
                    .contains(reserved);
        }
    }

    /**
     * An unrecognised non-null value must NOT read as "not protected". The column is constrained to
     * two strings, so a third one means the schema and this enum have diverged — and treating it as
     * unreserved would silently open the recovery resources to every SCIM write.
     */
    @Test
    void an_unrecognised_reservation_throws_rather_than_reading_as_unprotected() {
        assertThatThrownBy(() -> ReservedResourceName.ofStoredValue("something-else"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not know");
    }

    @Test
    void the_lookup_is_exact_rather_than_case_insensitive() {
        assertThatThrownBy(() -> ReservedResourceName.ofStoredValue("BOOTSTRAP-ADMIN"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void a_reserved_user_is_protected_from_writes_and_exempt_from_lockout() {
        ScimUser bootstrapAdmin = reserved(ReservedResourceName.BOOTSTRAP_ADMIN);

        assertThat(bootstrapAdmin.isProtectedFromWrites()).isTrue();
        assertThat(bootstrapAdmin.isExemptFromLockout()).isTrue();
    }

    /**
     * The two rules are distinct even though one marker decides both today. A resource reserved
     * under a future third name would be protected from writes without being exempt from lockout,
     * and collapsing the two would grant it an exemption nobody asked for.
     */
    @Test
    void a_reservation_other_than_the_bootstrap_admin_confers_no_lockout_exemption() {
        ScimUser reservedOtherwise = reserved(ReservedResourceName.ADMIN_GROUP);

        assertThat(reservedOtherwise.isProtectedFromWrites()).isTrue();
        assertThat(reservedOtherwise.isExemptFromLockout()).isFalse();
    }

    @Test
    void an_ordinary_user_is_neither_protected_nor_exempt() {
        ScimUser ordinary = reserved(null);

        assertThat(ordinary.isProtectedFromWrites()).isFalse();
        assertThat(ordinary.isExemptFromLockout()).isFalse();
    }

    private static ScimUser reserved(ReservedResourceName reservedName) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new ScimUser(
                UUID.randomUUID(),
                new ScimUserProfile("admin", null, null, null, null, null, true, null),
                ScimLoginState.of("hash"),
                reservedName,
                ScimUser.INITIAL_VERSION,
                now,
                now);
    }
}
