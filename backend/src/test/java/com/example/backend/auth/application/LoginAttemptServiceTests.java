package com.example.backend.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.audit.RecordingAuditTrail;
import com.example.backend.audit.RecordingAuditTrail.Recorded;
import com.example.backend.audit.domain.AuditLockoutLift;
import com.example.backend.audit.domain.AuditOperation;
import com.example.backend.audit.domain.AuditRefusalReason;
import com.example.backend.auth.InMemoryAccountRepository;
import com.example.backend.auth.MutableClock;
import com.example.backend.auth.domain.Account;
import com.example.backend.auth.domain.AccountRole;
import com.example.backend.auth.domain.LockoutPolicy;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginAttemptServiceTests {

    private static final Instant NOW = Instant.parse("2026-09-24T07:00:00Z");

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final MutableClock clock = new MutableClock(NOW);
    private final RecordingAuditTrail audit = new RecordingAuditTrail();

    private LoginAttemptService attempts;

    @BeforeEach
    void setUp() {
        attempts = new LoginAttemptService(
                accounts, new LockoutPolicy(3, Duration.ofMinutes(5)), audit, clock);
        accounts.save(new Account("ada", "hash", AccountRole.USER));
    }

    @Test
    void aRefusedAttemptIsCountedAgainstTheAccount() {
        attempts.recordFailure("ada", AuditRefusalReason.BAD_CREDENTIALS);

        assertThat(accounts.require("ada").failedLoginAttempts()).isEqualTo(1);
        assertThat(accounts.require("ada").isLocked(NOW)).isFalse();
    }

    @Test
    void theThirdConsecutiveRefusalLocksTheAccount() {
        attempts.recordFailure("ada", AuditRefusalReason.BAD_CREDENTIALS);
        attempts.recordFailure("ada", AuditRefusalReason.BAD_CREDENTIALS);
        attempts.recordFailure("ada", AuditRefusalReason.BAD_CREDENTIALS);

        Account locked = accounts.require("ada");
        assertThat(locked.isLocked(NOW)).isTrue();
        assertThat(locked.lockedUntil()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
    }

    @Test
    void anAcceptedLoginResetsTheFailureCount() {
        attempts.recordFailure("ada", AuditRefusalReason.BAD_CREDENTIALS);
        attempts.recordFailure("ada", AuditRefusalReason.BAD_CREDENTIALS);

        attempts.recordSuccess("ada");

        assertThat(accounts.require("ada").failedLoginAttempts()).isZero();
        assertThat(accounts.require("ada").lockedUntil()).isNull();
    }

    /**
     * Recording nothing for a name that does not exist is what keeps a refusal
     * uninformative: no row appears, so stored state cannot be used to enumerate
     * accounts.
     */
    @Test
    void aRefusalForAnUnknownUsernameIsNotRecordedAnywhere() {
        attempts.recordFailure("nobody", AuditRefusalReason.BAD_CREDENTIALS);

        assertThat(accounts.findByUsername("nobody")).isEmpty();
        assertThat(accounts.require("ada").failedLoginAttempts()).isZero();
    }

    @Test
    void anAcceptedLoginForAnUnknownUsernameIsANoOp() {
        attempts.recordSuccess("nobody");

        assertThat(accounts.findByUsername("nobody")).isEmpty();
    }

    /**
     * An account with no failure run has nothing to clear, so the login must not
     * write to it — every accepted login would otherwise cost a pointless update.
     */
    @Test
    void anAcceptedLoginOnAnUntouchedAccountWritesNothing() {
        int savesBefore = accounts.saves();

        attempts.recordSuccess("ada");

        assertThat(accounts.saves()).isEqualTo(savesBefore);
    }

    @Test
    void anAcceptedLoginAfterAFailureDoesWrite() {
        attempts.recordFailure("ada", AuditRefusalReason.BAD_CREDENTIALS);
        int savesBefore = accounts.saves();

        attempts.recordSuccess("ada");

        assertThat(accounts.saves()).isEqualTo(savesBefore + 1);
    }

    @Test
    void theClockDecidesWhenTheLockoutIsOver() {
        attempts.recordFailure("ada", AuditRefusalReason.BAD_CREDENTIALS);
        attempts.recordFailure("ada", AuditRefusalReason.BAD_CREDENTIALS);
        attempts.recordFailure("ada", AuditRefusalReason.BAD_CREDENTIALS);
        assertThat(accounts.require("ada").isLocked(clock.instant())).isTrue();

        clock.advanceBy(Duration.ofMinutes(5));

        assertThat(accounts.require("ada").isLocked(clock.instant())).isFalse();
    }

    // What the trail is told, which is the other half of counting an attempt

    @Test
    void aRefusedAttemptIsRecordedAgainstTheAccountsStableIdWithItsReason() {
        attempts.recordFailure("ada", AuditRefusalReason.BAD_CREDENTIALS);

        assertThat(audit.recorded()).containsExactly(new Recorded(
                AuditOperation.LOGIN_FAILURE,
                null,
                accounts.require("ada").id(),
                AuditRefusalReason.BAD_CREDENTIALS.name()));
    }

    /**
     * No account carries the name, so there is no subject — and the submitted value
     * is not recorded in its place. The reason the caller supplied is replaced too:
     * whether the name exists is settled here, by looking, not guessed from an
     * exception type the authentication library deliberately makes ambiguous.
     */
    @Test
    void aRefusalForAnUnknownUsernameIsRecordedWithNoSubjectAndItsOwnReason() {
        attempts.recordFailure("nobody", AuditRefusalReason.BAD_CREDENTIALS);

        assertThat(audit.recorded()).containsExactly(new Recorded(
                AuditOperation.LOGIN_FAILURE,
                null,
                null,
                AuditRefusalReason.UNKNOWN_ACCOUNT.name()));
    }

    @Test
    void reachingTheLimitRecordsTheLockoutOnceBesideEachRefusal() {
        attempts.recordFailure("ada", AuditRefusalReason.BAD_CREDENTIALS);
        attempts.recordFailure("ada", AuditRefusalReason.BAD_CREDENTIALS);
        attempts.recordFailure("ada", AuditRefusalReason.BAD_CREDENTIALS);
        // Refused by the lockout now, which neither extends it nor re-imposes it.
        attempts.recordFailure("ada", AuditRefusalReason.ACCOUNT_LOCKED);

        assertThat(audit.of(AuditOperation.LOCKOUT_SET)).containsExactly(new Recorded(
                AuditOperation.LOCKOUT_SET, null, accounts.require("ada").id(), null));
        assertThat(audit.of(AuditOperation.LOGIN_FAILURE)).hasSize(4);
        assertThat(audit.of(AuditOperation.LOCKOUT_LIFT)).isEmpty();
    }

    @Test
    void anAcceptedLoginIsRecordedAgainstTheAccountsStableId() {
        attempts.recordSuccess("ada");

        assertThat(audit.recorded()).containsExactly(new Recorded(
                AuditOperation.LOGIN_SUCCESS,
                accounts.require("ada").id(),
                accounts.require("ada").id(),
                null));
    }

    @Test
    void anAcceptedLoginForAnUnknownUsernameRecordsNothing() {
        attempts.recordSuccess("nobody");

        assertThat(audit.recorded()).isEmpty();
    }

    /**
     * The expiry is recorded at the next attempt against the account, and once —
     * after which the row no longer holds the evidence a lockout existed, so there
     * is nothing left to record it from.
     */
    @Test
    void aLockoutThatRanOutIsRecordedOnceAtTheNextRefusal() {
        attempts.recordFailure("ada", AuditRefusalReason.BAD_CREDENTIALS);
        attempts.recordFailure("ada", AuditRefusalReason.BAD_CREDENTIALS);
        attempts.recordFailure("ada", AuditRefusalReason.BAD_CREDENTIALS);
        clock.advanceBy(Duration.ofMinutes(6));
        audit.reset();

        attempts.recordFailure("ada", AuditRefusalReason.BAD_CREDENTIALS);

        assertThat(audit.of(AuditOperation.LOCKOUT_LIFT)).containsExactly(new Recorded(
                AuditOperation.LOCKOUT_LIFT,
                null,
                accounts.require("ada").id(),
                AuditLockoutLift.EXPIRY.name()));

        audit.reset();
        attempts.recordFailure("ada", AuditRefusalReason.BAD_CREDENTIALS);
        assertThat(audit.of(AuditOperation.LOCKOUT_LIFT)).isEmpty();
    }

    @Test
    void aLockoutThatRanOutIsRecordedAtAnAcceptedLoginToo() {
        attempts.recordFailure("ada", AuditRefusalReason.BAD_CREDENTIALS);
        attempts.recordFailure("ada", AuditRefusalReason.BAD_CREDENTIALS);
        attempts.recordFailure("ada", AuditRefusalReason.BAD_CREDENTIALS);
        clock.advanceBy(Duration.ofMinutes(6));
        audit.reset();

        attempts.recordSuccess("ada");

        assertThat(audit.of(AuditOperation.LOCKOUT_LIFT)).hasSize(1);
        assertThat(audit.of(AuditOperation.LOGIN_SUCCESS)).hasSize(1);
    }

    /**
     * An account that never locked out has no expiry to report, so an accepted
     * login records the success alone.
     */
    @Test
    void anAccountThatNeverLockedOutReportsNoExpiry() {
        attempts.recordFailure("ada", AuditRefusalReason.BAD_CREDENTIALS);
        audit.reset();

        attempts.recordSuccess("ada");

        assertThat(audit.of(AuditOperation.LOCKOUT_LIFT)).isEmpty();
        assertThat(audit.of(AuditOperation.LOGIN_SUCCESS)).hasSize(1);
    }
}
