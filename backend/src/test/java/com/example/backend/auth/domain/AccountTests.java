package com.example.backend.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AccountTests {

    private static final Instant NOW = Instant.parse("2026-09-24T07:00:00Z");

    /** Far enough past any window the old, expiring lockout ever had. */
    private static final Duration A_LONG_TIME = Duration.ofDays(3650);

    private static final LockoutPolicy POLICY = new LockoutPolicy(3);

    private static final Account ACCOUNT =
            new Account("ada", "hash", AccountRole.USER);

    @Test
    void aNewAccountHasNoFailuresAndNoLockout() {
        assertThat(ACCOUNT.failedLoginAttempts()).isZero();
        assertThat(ACCOUNT.lockedAt()).isNull();
        assertThat(ACCOUNT.isLocked()).isFalse();
    }

    @Test
    void aFailureBelowTheLimitIsCountedWithoutLocking() {
        Account afterOne = ACCOUNT.withFailureRecorded(POLICY, NOW);
        Account afterTwo = afterOne.withFailureRecorded(POLICY, NOW);

        assertThat(afterOne.failedLoginAttempts()).isEqualTo(1);
        assertThat(afterTwo.failedLoginAttempts()).isEqualTo(2);
        assertThat(afterTwo.lockedAt()).isNull();
        assertThat(afterTwo.isLocked()).isFalse();
    }

    @Test
    void reachingTheLimitLocksTheAccountAsOfThatMoment() {
        Account locked = failTimes(ACCOUNT, 3, NOW);

        assertThat(locked.failedLoginAttempts()).isEqualTo(3);
        assertThat(locked.lockedAt()).isEqualTo(NOW);
        assertThat(locked.isLocked()).isTrue();
    }

    /** The lock is already in force, so attempts made against it change nothing. */
    @Test
    void aFailureDuringTheLockoutNeitherCountsNorDeepensIt() {
        Account locked = failTimes(ACCOUNT, 3, NOW);

        Account afterAnotherTry = locked.withFailureRecorded(
                POLICY, NOW.plus(Duration.ofMinutes(1)));

        assertThat(afterAnotherTry).isSameAs(locked);
    }

    /**
     * The heart of the change: there is no instant at which the lock has lifted.
     * A test that only advanced a clock a little could pass against an expiring
     * lockout with a long window, so the advance here is a decade.
     */
    @Test
    void theLockoutIsStillInForceHoweverMuchTimeHasPassed() {
        Account locked = failTimes(ACCOUNT, 3, NOW);

        assertThat(locked.isLocked()).isTrue();
        assertThat(locked.withFailureRecorded(POLICY, NOW.plus(A_LONG_TIME)))
                .isSameAs(locked);
        assertThat(locked.isLocked()).isTrue();
    }

    /**
     * A locked account has nothing but Unlock ahead of it, so a later failure
     * cannot start a fresh run — the whole "expired lockout gets a new run"
     * branch is gone with the expiry it depended on.
     */
    @Test
    void noFailureAfterALockoutEverStartsANewRun() {
        Account locked = failTimes(ACCOUNT, 3, NOW);

        Account afterOneMore = locked.withFailureRecorded(POLICY, NOW.plus(A_LONG_TIME));

        assertThat(afterOneMore.failedLoginAttempts()).isEqualTo(3);
        assertThat(afterOneMore.lockedAt()).isEqualTo(NOW);
    }

    @Test
    void clearingTheLockoutIsWhatEndsItAndTheFailureRunWithIt() {
        Account locked = failTimes(ACCOUNT, 3, NOW);

        Account unlocked = locked.withLockoutCleared();

        assertThat(unlocked.isLocked()).isFalse();
        assertThat(unlocked.lockedAt()).isNull();
        assertThat(unlocked.failedLoginAttempts()).isZero();
    }

    /** Unlocking leaves the credentials alone, so the same password still logs in. */
    @Test
    void clearingTheLockoutKeepsTheCredentialsAndRole() {
        Account unlocked = failTimes(ACCOUNT, 3, NOW).withLockoutCleared();

        assertThat(unlocked.username()).isEqualTo("ada");
        assertThat(unlocked.passwordHash()).isEqualTo("hash");
        assertThat(unlocked.role()).isEqualTo(AccountRole.USER);
    }

    @Test
    void aSuccessfulLoginClearsTheFailureRun() {
        Account afterTwoFailures = failTimes(ACCOUNT, 2, NOW);

        Account afterLogin = afterTwoFailures.withSuccessfulLogin();

        assertThat(afterLogin.failedLoginAttempts()).isZero();
        assertThat(afterLogin.lockedAt()).isNull();
    }

    /** Identity signals "nothing to write", which the caller uses to skip a save. */
    @Test
    void aSuccessfulLoginOnAnUntouchedAccountReturnsItUnchanged() {
        assertThat(ACCOUNT.withSuccessfulLogin()).isSameAs(ACCOUNT);
    }

    /**
     * The guard has to test both halves: an account carrying a lock instant but no
     * counted failures still has something to clear, so it must not be mistaken
     * for one that was never touched.
     */
    @Test
    void clearingALockoutWorksEvenWhenNoFailuresAreCounted() {
        Account oddlyLocked = new Account("ada", "hash", AccountRole.USER, 0, NOW);

        Account unlocked = oddlyLocked.withLockoutCleared();

        assertThat(unlocked.lockedAt()).isNull();
        assertThat(unlocked.isLocked()).isFalse();
    }

    // Counted but never locked — the Bootstrap Admin's transition

    @Test
    void aCountedFailureLengthensTheRunWithoutEverLocking() {
        Account account = ACCOUNT;
        for (int attempt = 0; attempt < 10; attempt++) {
            account = account.withFailureCounted();
        }

        assertThat(account.failedLoginAttempts()).isEqualTo(10);
        assertThat(account.lockedAt()).isNull();
        assertThat(account.isLocked()).isFalse();
    }

    @Test
    void aCountedFailureCarriesTheCredentialsAndProfileThrough() {
        Account seeded = new Account("ada", "hash", AccountRole.ADMIN, 0, null, false, NOW);

        Account afterFailure = seeded.withFailureCounted();

        assertThat(afterFailure.username()).isEqualTo("ada");
        assertThat(afterFailure.passwordHash()).isEqualTo("hash");
        assertThat(afterFailure.role()).isEqualTo(AccountRole.ADMIN);
        assertThat(afterFailure.enabled()).isFalse();
        assertThat(afterFailure.createdAt()).isEqualTo(NOW);
    }

    /** An accepted login clears the run this transition built up, as for anyone else. */
    @Test
    void aSuccessfulLoginClearsARunOfCountedFailures() {
        Account afterFailures = ACCOUNT.withFailureCounted().withFailureCounted();

        assertThat(afterFailures.withSuccessfulLogin().failedLoginAttempts()).isZero();
    }

    @Test
    void aPolicyMustAllowAtLeastOneAttempt() {
        assertThatThrownBy(() -> new LockoutPolicy(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A lockout policy needs at least one attempt");
        assertThatThrownBy(() -> new LockoutPolicy(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A lockout policy needs at least one attempt");
    }

    /**
     * One attempt is the strictest policy the rule accepts, and it is the boundary:
     * a single failure locks the account immediately.
     */
    @Test
    void aPolicyOfOneAttemptLocksOnTheFirstFailure() {
        LockoutPolicy strict = new LockoutPolicy(1);

        Account afterOne = ACCOUNT.withFailureRecorded(strict, NOW);

        assertThat(afterOne.isLocked()).isTrue();
        assertThat(afterOne.lockedAt()).isEqualTo(NOW);
    }

    private static Account failTimes(Account account, int times, Instant now) {
        Account current = account;
        for (int attempt = 0; attempt < times; attempt++) {
            current = current.withFailureRecorded(POLICY, now);
        }
        return current;
    }

    @Test
    void aNewAccountIsEnabledAndCarriesTheProfileItWasGiven() {
        Account seeded = new Account("ada", "hash", AccountRole.USER, 0, null, true, NOW);

        assertThat(seeded.createdAt()).isEqualTo(NOW);
        assertThat(seeded.enabled()).isTrue();
    }

    @Test
    void backfillingFillsACreationTimestampTheAccountDoesNotHave() {
        Account backfilled = ACCOUNT.withCreatedAtBackfilled(NOW);

        assertThat(backfilled.createdAt()).isEqualTo(NOW);
    }

    /**
     * The identity of the result is what lets seeding skip a write on an account
     * that needs nothing, so "already recorded" has to return the same instance
     * rather than an equal copy.
     */
    @Test
    void backfillingARecordedTimestampChangesNothingAndAllocatesNothing() {
        Account complete = new Account("ada", "hash", AccountRole.USER, 0, null, true, NOW);

        assertThat(complete.withCreatedAtBackfilled(NOW.plusSeconds(60))).isSameAs(complete);
    }

    /**
     * A backfill is administrative bookkeeping on a row that predates the
     * column. It must not disturb the login history, which the login path owns
     * and may have written a moment earlier.
     */
    @Test
    void backfillingPreservesTheFailureRunAndLockout() {
        Account locked = failTimes(ACCOUNT, 3, NOW);

        Account backfilled = locked.withCreatedAtBackfilled(NOW);

        assertThat(backfilled.failedLoginAttempts()).isEqualTo(3);
        assertThat(backfilled.lockedAt()).isEqualTo(locked.lockedAt());
        assertThat(backfilled.isLocked()).isTrue();
    }

    /** The two refusal mechanisms are independent; neither implies the other. */
    @Test
    void aDisabledAccountIsNotLockedAndALockedAccountIsNotDisabled() {
        Account disabled = new Account("ada", "hash", AccountRole.USER, 0, null, false, NOW);
        Account locked = failTimes(ACCOUNT, 3, NOW);

        assertThat(disabled.isLocked()).isFalse();
        assertThat(locked.enabled()).isTrue();
    }

    /** Enabling and disabling leave the lock exactly as it stands. */
    @Test
    void changingTheAdministrativeStandingLeavesTheLockoutInForce() {
        Account locked = failTimes(ACCOUNT, 3, NOW);

        Account disabled = locked.withEnabled(false);

        assertThat(disabled.isLocked()).isTrue();
        assertThat(disabled.lockedAt()).isEqualTo(NOW);
        assertThat(disabled.withEnabled(true).isLocked()).isTrue();
    }

    @Test
    void theLoginTransitionsCarryTheProfileThrough() {
        Account seeded = new Account("ada", "hash", AccountRole.USER, 0, null, false, NOW);

        Account afterFailure = seeded.withFailureRecorded(POLICY, NOW);
        Account afterSuccess = afterFailure.withSuccessfulLogin();

        assertThat(afterFailure.createdAt()).isEqualTo(NOW);
        assertThat(afterFailure.enabled()).isFalse();
        assertThat(afterSuccess.createdAt()).isEqualTo(NOW);
        assertThat(afterSuccess.enabled()).isFalse();
    }

    /**
     * A credentialless account — no password ever set — is a real, constructible
     * state, not an invariant violation: an administrator may create or reset an
     * account before issuing it a password. It behaves like any other account for
     * every transition that has nothing to do with the hash.
     */
    @Test
    void anAccountMayCarryNoPasswordHash() {
        Account credentialless = new Account("nopass", null, AccountRole.USER);

        assertThat(credentialless.passwordHash()).isNull();
        assertThat(credentialless.isLocked()).isFalse();

        Account afterFailure = credentialless.withFailureRecorded(POLICY, NOW);

        assertThat(afterFailure.passwordHash()).isNull();
        assertThat(afterFailure.failedLoginAttempts()).isEqualTo(1);
    }
}
