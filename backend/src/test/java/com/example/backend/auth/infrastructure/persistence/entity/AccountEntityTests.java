package com.example.backend.auth.infrastructure.persistence.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.auth.domain.AccountRole;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * The constructor stores every argument it is given.
 *
 * <p>Worth its own test because the integration path cannot see this: a saved
 * entity is read back by Hibernate through the no-arg constructor and field
 * access, so a constructor that silently dropped a field would still round-trip
 * through the database correctly. What it would break is the object the adapter
 * maps back to a domain {@code Account} <em>without</em> re-reading — the return
 * value of a save — and nothing asserted that.
 *
 * <p>{@code enabled} is the field that made this visible: it is a nullable
 * {@code Boolean} here, so dropping its assignment yields null rather than a
 * failure, and null unboxes into an account that is neither enabled nor
 * disabled.
 */
class AccountEntityTests {

    @Test
    void theConstructorStoresEveryFieldItIsGiven() {
        UUID id = UUID.randomUUID();
        Instant lockedAt = Instant.parse("2026-09-24T07:00:00Z");
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

        AccountEntity entity = new AccountEntity(
                id, "ada", "hash", AccountRole.ADMIN, 4, lockedAt, false, createdAt);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getUsername()).isEqualTo("ada");
        assertThat(entity.getPasswordHash()).isEqualTo("hash");
        assertThat(entity.getRole()).isEqualTo(AccountRole.ADMIN);
        assertThat(entity.getFailedLoginAttempts()).isEqualTo(4);
        assertThat(entity.getLockedAt()).isEqualTo(lockedAt);
        assertThat(entity.getEnabled()).isFalse();
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
    }

    /** An enabled account is stored as enabled, not merely as "not disabled". */
    @Test
    void anEnabledAccountIsStoredAsEnabled() {
        AccountEntity entity = new AccountEntity(
                UUID.randomUUID(), "grace", null, AccountRole.USER, 0, null, true, null);

        assertThat(entity.getEnabled()).isTrue();
    }
}
