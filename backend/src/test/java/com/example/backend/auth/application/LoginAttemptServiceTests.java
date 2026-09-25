package com.example.backend.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.audit.RecordingAuditTrail;
import com.example.backend.audit.RecordingAuditTrail.Recorded;
import com.example.backend.audit.domain.AuditOperation;
import com.example.backend.audit.domain.AuditRefusalReason;
import com.example.backend.auth.InMemoryAccountRepository;
import com.example.backend.auth.InMemoryAccountSessions;
import com.example.backend.auth.MutableClock;
import com.example.backend.auth.PendingCommit;
import com.example.backend.auth.domain.Account;
import com.example.backend.auth.domain.AccountRole;
import com.example.backend.auth.domain.BootstrapAdmin;
import com.example.backend.auth.domain.LockoutPolicy;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginAttemptServiceTests {

    private static final Instant NOW = Instant.parse("2026-09-24T07:00:00Z");

    /** Far past any window the former expiring lockout could have had. */
    private static final Duration A_LONG_TIME = Duration.ofDays(3650);

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final InMemoryAccountSessions sessions = new InMemoryAccountSessions();
    private final PendingCommit transaction = new PendingCommit();
    private final MutableClock clock = new MutableClock(NOW);
    private final RecordingAuditTrail audit = new RecordingAuditTrail();

    private LoginAttemptService attempts;

    @BeforeEach
    void setUp() {
        attempts = new LoginAttemptService(
                accounts,
                sessions,
                transaction,
                new LockoutPolicy(3),
                new BootstrapAdmin("recovery-admin"),
                audit,
                clock);
        accounts.save(new Account("ada", "hash", AccountRole.USER));
    }

    @Test
    void aRefusedAttemptIsCountedAgainstTheAccount() {
        attempts.recordFailure("ada", AuditRefusalReason.BAD_CREDENTIALS);

        assertThat(accounts.require("ada").failedLoginAttempts()).isEqualTo(1);
        assertThat(accounts.require("ada").isLocked()).isFalse();
    }

    @Test
    void theThirdConsecutiveRefusalLocksTheAccount() {
        failTimes(3);

        Account locked = accounts.require("ada");
        assertThat(locked.isLocked()).isTrue();
        assertThat(locked.lockedAt()).isEqualTo(NOW);
    }

    @Test
    void anAcceptedLoginResetsTheFailureCount() {
        failTimes(2);

        attempts.recordSuccess("ada");

        assertThat(accounts.require("ada").failedLoginAttempts()).isZero();
        assertThat(accounts.require("ada").lockedAt()).isNull();
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

    /**
     * No amount of elapsed time is a lift. The clock is moved a decade rather than
     * a few minutes so the assertion could not pass against a merely long window.
     */
    @Test
    void noPassageOfTimeEndsTheLockout() {
        failTimes(3);

        clock.advanceBy(A_LONG_TIME);
        attempts.recordFailure("ada", AuditRefusalReason.ACCOUNT_LOCKED);

        Account stillLocked = accounts.require("ada");
        assertThat(stillLocked.isLocked()).isTrue();
        assertThat(stillLocked.lockedAt()).isEqualTo(NOW);
        assertThat(stillLocked.failedLoginAttempts()).isEqualTo(3);
    }

    // The sessions the lock takes away

    /**
     * A lock that left live sessions alone would close the front door while the
     * account kept acting through a session it already held.
     */
    @Test
    void imposingTheLockoutRevokesTheAccountsSessionsAfterTheCommit() {
        sessions.open(accounts.require("ada").id(), "session-1");

        failTimes(3);

        assertThat(transaction.pending()).isEqualTo(1);
        assertThat(sessions.revocations()).isEmpty();

        transaction.commit();

        assertThat(sessions.revocations()).containsExactly(accounts.require("ada").id());
        assertThat(sessions.sessionsOf(accounts.require("ada").id())).isEmpty();
    }

    /** A rolled-back transaction wrote no lock, so it must revoke nothing. */
    @Test
    void aRolledBackFailureRevokesNothing() {
        sessions.open(accounts.require("ada").id(), "session-1");

        failTimes(3);
        transaction.rollback();

        assertThat(sessions.revocations()).isEmpty();
        assertThat(sessions.sessionsOf(accounts.require("ada").id()))
                .containsExactly("session-1");
    }

    @Test
    void aFailureBelowTheLimitRevokesNothing() {
        failTimes(2);
        transaction.commit();

        assertThat(sessions.revocations()).isEmpty();
    }

    /** Only the transition into the lock revokes; a refusal after it does not. */
    @Test
    void anAttemptAgainstAnAlreadyLockedAccountRevokesNothingFurther() {
        failTimes(3);
        transaction.commit();

        attempts.recordFailure("ada", AuditRefusalReason.ACCOUNT_LOCKED);
        transaction.commit();

        assertThat(sessions.revocations())
                .containsExactly(accounts.require("ada").id());
    }

    // The Bootstrap Admin, which is counted and audited but never locked

    @Test
    void theBootstrapAdminIsNeverLockedHoweverLongItsFailureRunGrows() {
        accounts.save(new Account("recovery-admin", "hash", AccountRole.ADMIN));

        for (int attempt = 0; attempt < 10; attempt++) {
            attempts.recordFailure("recovery-admin", AuditRefusalReason.BAD_CREDENTIALS);
        }

        Account recovery = accounts.require("recovery-admin");
        assertThat(recovery.isLocked()).isFalse();
        assertThat(recovery.lockedAt()).isNull();
        assertThat(recovery.failedLoginAttempts()).isEqualTo(10);
    }

    @Test
    void everyBootstrapAdminFailureIsAuditedAgainstItsStableId() {
        Account recovery = accounts.save(
                new Account("recovery-admin", "hash", AccountRole.ADMIN));

        for (int attempt = 0; attempt < 10; attempt++) {
            attempts.recordFailure("recovery-admin", AuditRefusalReason.BAD_CREDENTIALS);
        }

        assertThat(audit.of(AuditOperation.LOGIN_FAILURE)).hasSize(10)
                .allSatisfy(event -> assertThat(event.subjectId()).isEqualTo(recovery.id()));
        assertThat(audit.of(AuditOperation.LOCKOUT_SET)).isEmpty();
    }

    @Test
    void theBootstrapAdminKeepsItsSessionsThroughAFailureRun() {
        Account recovery = accounts.save(
                new Account("recovery-admin", "hash", AccountRole.ADMIN));
        sessions.open(recovery.id(), "recovery-session");

        for (int attempt = 0; attempt < 10; attempt++) {
            attempts.recordFailure("recovery-admin", AuditRefusalReason.BAD_CREDENTIALS);
        }
        transaction.commit();

        assertThat(sessions.revocations()).isEmpty();
        assertThat(sessions.sessionsOf(recovery.id())).containsExactly("recovery-session");
    }

    /** The exemption is this one account's, not every administrator's. */
    @Test
    void anotherAdministratorStillLocks() {
        accounts.save(new Account("ordinary-admin", "hash", AccountRole.ADMIN));

        for (int attempt = 0; attempt < 3; attempt++) {
            attempts.recordFailure("ordinary-admin", AuditRefusalReason.BAD_CREDENTIALS);
        }

        assertThat(accounts.require("ordinary-admin").isLocked()).isTrue();
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
        failTimes(3);
        // Refused by the lockout now, which neither deepens it nor re-imposes it.
        attempts.recordFailure("ada", AuditRefusalReason.ACCOUNT_LOCKED);

        assertThat(audit.of(AuditOperation.LOCKOUT_SET)).containsExactly(new Recorded(
                AuditOperation.LOCKOUT_SET, null, accounts.require("ada").id(), null));
        assertThat(audit.of(AuditOperation.LOGIN_FAILURE)).hasSize(4);
        assertThat(audit.of(AuditOperation.LOCKOUT_LIFT)).isEmpty();
    }

    /**
     * Nothing on this path can record a lift. There is no unrequested lift to
     * record: the only one is an administrator's Unlock, which happens in
     * {@link AccountAdministrationService} and names its actor.
     */
    @Test
    void noLoginAttemptEverRecordsALockoutLift() {
        failTimes(3);
        clock.advanceBy(A_LONG_TIME);

        attempts.recordFailure("ada", AuditRefusalReason.ACCOUNT_LOCKED);
        attempts.recordSuccess("ada");

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

    private void failTimes(int times) {
        for (int attempt = 0; attempt < times; attempt++) {
            attempts.recordFailure("ada", AuditRefusalReason.BAD_CREDENTIALS);
        }
    }
}
