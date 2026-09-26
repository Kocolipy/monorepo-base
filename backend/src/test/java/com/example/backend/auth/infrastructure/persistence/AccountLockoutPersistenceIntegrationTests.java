package com.example.backend.auth.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.ContainerTestConfiguration;
import com.example.backend.auth.domain.Account;
import com.example.backend.auth.domain.AccountRepository;
import com.example.backend.auth.domain.AccountRole;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * What the account repository actually writes to the lockout columns, read back out
 * of Postgres with SQL rather than through the mapping that wrote it.
 *
 * <p>The permanent lockout is stored state, not a computation: with no duration to
 * recompute from, a lock persists only because {@code locked_at} and the failure run
 * behind it reached the row and stayed there. That makes these two columns the whole
 * mechanism, and an adapter that dropped either argument on the way down would leave
 * every service-level lockout test passing while no account ever actually locked
 * across a restart.
 *
 * <p>Read with {@code JdbcTemplate} for the same reason the audit tests are: going
 * back out through {@code findByUsername} would let one mapping bug cancel another,
 * so what is asserted here is the bytes in the row.
 */
@SpringBootTest
@Import(ContainerTestConfiguration.class)
class AccountLockoutPersistenceIntegrationTests {

    private static final String ROW =
            "SELECT failed_login_attempts, locked_at, enabled, created_at"
            + " FROM accounts WHERE username = ?";

    @Autowired
    private AccountRepository accounts;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TransactionTemplate transaction;

    private Account saved;

    private Instant createdAt;

    /**
     * {@code updateLockout} is a modifying query, so it runs in the transaction its
     * production callers already hold rather than one this test invents.
     */
    private void updateLockout(int failedLoginAttempts, Instant lockedAt) {
        transaction.executeWithoutResult(status -> accounts.updateLockout(new Account(
                saved.id(),
                saved.username(),
                saved.passwordHash(),
                saved.role(),
                failedLoginAttempts,
                lockedAt,
                true,
                createdAt)));
    }

    @BeforeEach
    void seedAnAccount() {
        jdbc.update("DELETE FROM accounts WHERE username = ?", "lockout-persistence");
        createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        saved = accounts.save(new Account(
                UUID.randomUUID(),
                "lockout-persistence",
                "$argon2id$not-a-real-hash",
                AccountRole.USER,
                0,
                null,
                true,
                createdAt));
    }

    /**
     * The failure run reaches the row. This is the mutant-killing assertion for
     * {@code updateLockout} passing {@code account.failedLoginAttempts()}: an
     * adapter that sent the lock instant but not the count would still lock the
     * account, and every count assertion held in memory would pass.
     */
    @Test
    void updatingTheLockoutStoresTheFailureRunThatProducedIt() {
        Instant lockedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);

        updateLockout(5, lockedAt);

        Map<String, Object> row = jdbc.queryForMap(ROW, "lockout-persistence");
        assertThat(row.get("failed_login_attempts")).isEqualTo(5);
        assertThat(((java.sql.Timestamp) row.get("locked_at")).toInstant()).isEqualTo(lockedAt);
    }

    /**
     * A run short of the threshold stores the count with no lock instant beside it,
     * so the count is carried on its own rather than only as a by-product of
     * locking.
     */
    @Test
    void aFailureRunBelowTheThresholdIsStoredWithNoLockInstant() {
        updateLockout(3, null);

        Map<String, Object> row = jdbc.queryForMap(ROW, "lockout-persistence");
        assertThat(row.get("failed_login_attempts")).isEqualTo(3);
        assertThat(row.get("locked_at")).isNull();
    }

    /**
     * An Admin unlock writes the cleared state through: zero failures and no lock
     * instant. Without the count travelling, an unlocked account would come back
     * still carrying the run that locked it and re-lock on its next single mistake.
     */
    @Test
    void anUnlockStoresTheClearedRunAndNoLockInstant() {
        updateLockout(5, Instant.now().truncatedTo(ChronoUnit.MILLIS));

        updateLockout(0, null);

        Map<String, Object> row = jdbc.queryForMap(ROW, "lockout-persistence");
        assertThat(row.get("failed_login_attempts")).isEqualTo(0);
        assertThat(row.get("locked_at")).isNull();
    }

    /**
     * Saving stores the administrative standing and the creation timestamp the
     * caller supplied — the other two arguments on the same argument list the
     * lockout rename touched.
     */
    @Test
    void savingStoresTheStandingAndTheCreationTimestampItWasGiven() {
        Map<String, Object> row = jdbc.queryForMap(ROW, "lockout-persistence");

        assertThat(row.get("enabled")).isEqualTo(true);
        assertThat(((java.sql.Timestamp) row.get("created_at")).toInstant()).isEqualTo(createdAt);
    }

    /**
     * Lookup by stable id returns the mapped account. This is the id-keyed read the
     * session index and every administrative action depend on, and it had no test at
     * all: PIT could remove the repository call, the mapping, or the whole return
     * value and nothing failed.
     */
    @Test
    void findsAnAccountByItsStableId() {
        Optional<Account> found = accounts.findById(saved.id());

        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(saved.id());
        assertThat(found.get().username()).isEqualTo("lockout-persistence");
        assertThat(found.get().role()).isEqualTo(AccountRole.USER);
        assertThat(found.get().enabled()).isTrue();
    }

    /**
     * An id no row carries reads as empty rather than throwing or inventing an
     * account, so a caller holding a stale id learns the account is gone.
     */
    @Test
    void anIdNoRowCarriesReadsAsEmpty() {
        assertThat(accounts.findById(UUID.randomUUID())).isEmpty();
    }

    /**
     * Lookup by id reflects a lockout written through {@code updateLockout}, so the
     * mapping carries the lockout columns rather than only the identity ones.
     */
    @Test
    void lookupByIdReportsTheStoredLockout() {
        Instant lockedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);

        updateLockout(5, lockedAt);

        Account found = accounts.findById(saved.id()).orElseThrow();
        assertThat(found.failedLoginAttempts()).isEqualTo(5);
        assertThat(found.lockedAt()).isEqualTo(lockedAt);
        assertThat(found.isLocked()).isTrue();
    }
}
