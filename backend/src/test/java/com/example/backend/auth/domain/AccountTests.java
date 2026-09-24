package com.example.backend.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AccountTests {

    private static final Instant NOW = Instant.parse("2026-09-24T07:00:00Z");

    private static final LockoutPolicy POLICY = new LockoutPolicy(3, Duration.ofMinutes(5));

    private static final Account ACCOUNT =
            new Account("ada", "hash", AccountRole.USER);

    @Test
    void aNewAccountHasNoFailuresAndNoLockout() {
        assertThat(ACCOUNT.failedLoginAttempts()).isZero();
        assertThat(ACCOUNT.lockedUntil()).isNull();
        assertThat(ACCOUNT.isLocked(NOW)).isFalse();
    }

    @Test
    void aFailureBelowTheLimitIsCountedWithoutLocking() {
        Account afterOne = ACCOUNT.withFailureRecorded(POLICY, NOW);
        Account afterTwo = afterOne.withFailureRecorded(POLICY, NOW);

        assertThat(afterOne.failedLoginAttempts()).isEqualTo(1);
        assertThat(afterTwo.failedLoginAttempts()).isEqualTo(2);
        assertThat(afterTwo.lockedUntil()).isNull();
        assertThat(afterTwo.isLocked(NOW)).isFalse();
    }

    @Test
    void reachingTheLimitLocksTheAccountForThePolicyDuration() {
        Account locked = failTimes(ACCOUNT, 3, NOW);

        assertThat(locked.failedLoginAttempts()).isEqualTo(3);
        assertThat(locked.lockedUntil()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
        assertThat(locked.isLocked(NOW)).isTrue();
    }

    /** The penalty is a fixed window, so attempts made inside it change nothing. */
    @Test
    void aFailureDuringTheLockoutNeitherCountsNorExtendsIt() {
        Account locked = failTimes(ACCOUNT, 3, NOW);

        Account afterAnotherTry = locked.withFailureRecorded(
                POLICY, NOW.plus(Duration.ofMinutes(1)));

        assertThat(afterAnotherTry).isEqualTo(locked);
    }

    @Test
    void theLockoutIsOverOnceItsInstantHasPassed() {
        Account locked = failTimes(ACCOUNT, 3, NOW);

        assertThat(locked.isLocked(locked.lockedUntil().minusMillis(1))).isTrue();
        assertThat(locked.isLocked(locked.lockedUntil())).isFalse();
        assertThat(locked.isLocked(locked.lockedUntil().plusMillis(1))).isFalse();
    }

    /**
     * An account whose lockout has expired gets a fresh run of attempts, rather
     * than being re-locked by the next single mistake on a count left at the limit.
     */
    @Test
    void aFailureAfterAnExpiredLockoutStartsANewRun() {
        Account locked = failTimes(ACCOUNT, 3, NOW);
        Instant afterExpiry = locked.lockedUntil().plusSeconds(1);

        Account afterOneMore = locked.withFailureRecorded(POLICY, afterExpiry);

        assertThat(afterOneMore.failedLoginAttempts()).isEqualTo(1);
        assertThat(afterOneMore.lockedUntil()).isNull();
        assertThat(afterOneMore.isLocked(afterExpiry)).isFalse();
    }

    @Test
    void aSuccessfulLoginClearsTheFailureRunAndAnyExpiredLockout() {
        Account locked = failTimes(ACCOUNT, 3, NOW);

        Account afterLogin = locked.withSuccessfulLogin();

        assertThat(afterLogin.failedLoginAttempts()).isZero();
        assertThat(afterLogin.lockedUntil()).isNull();
    }

    /** Identity signals "nothing to write", which the caller uses to skip a save. */
    @Test
    void aSuccessfulLoginOnAnUntouchedAccountReturnsItUnchanged() {
        assertThat(ACCOUNT.withSuccessfulLogin()).isSameAs(ACCOUNT);
    }

    /**
     * The guard has to test both halves: an account carrying a lockout instant but
     * no counted failures still has something to clear, so it must not be mistaken
     * for one that was never touched.
     */
    @Test
    void aSuccessfulLoginClearsALockoutEvenWhenNoFailuresAreCounted() {
        Account oddlyLocked = new Account(
                "ada", "hash", AccountRole.USER, 0, NOW.plus(Duration.ofMinutes(5)));

        Account afterLogin = oddlyLocked.withSuccessfulLogin();

        assertThat(afterLogin.lockedUntil()).isNull();
        assertThat(afterLogin.isLocked(NOW)).isFalse();
    }

    @Test
    void aSuccessfulLoginKeepsTheCredentialsAndRole() {
        Account afterLogin = failTimes(ACCOUNT, 1, NOW).withSuccessfulLogin();

        assertThat(afterLogin.username()).isEqualTo("ada");
        assertThat(afterLogin.passwordHash()).isEqualTo("hash");
        assertThat(afterLogin.role()).isEqualTo(AccountRole.USER);
    }

    @Test
    void aPolicyMustAllowAtLeastOneAttemptAndLockForAPositiveTime() {
        assertThatThrownBy(() -> new LockoutPolicy(0, Duration.ofMinutes(5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A lockout policy needs at least one attempt");
        assertThatThrownBy(() -> new LockoutPolicy(3, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A lockout needs a positive duration");
        assertThatThrownBy(() -> new LockoutPolicy(3, Duration.ofMinutes(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A lockout needs a positive duration");
        assertThatThrownBy(() -> new LockoutPolicy(3, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A lockout needs a positive duration");
    }

    /**
     * One attempt is the strictest policy the rule accepts, and it is the boundary:
     * a single failure locks the account immediately.
     */
    @Test
    void aPolicyOfOneAttemptLocksOnTheFirstFailure() {
        LockoutPolicy strict = new LockoutPolicy(1, Duration.ofMinutes(5));

        Account afterOne = ACCOUNT.withFailureRecorded(strict, NOW);

        assertThat(afterOne.isLocked(NOW)).isTrue();
        assertThat(afterOne.lockedUntil()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
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
        Account seeded = new Account(
                "ada", "hash", AccountRole.USER, 0, null, "ada@example.com", true, NOW);

        assertThat(seeded.email()).isEqualTo("ada@example.com");
        assertThat(seeded.createdAt()).isEqualTo(NOW);
        assertThat(seeded.enabled()).isTrue();
    }

    @Test
    void backfillingFillsAProfileTheAccountDoesNotHave() {
        Account backfilled = ACCOUNT.withProfileBackfilled("ada@example.com", NOW);

        assertThat(backfilled.email()).isEqualTo("ada@example.com");
        assertThat(backfilled.createdAt()).isEqualTo(NOW);
    }

    /**
     * The identity of the result is what lets seeding skip a write on an account
     * that needs nothing, so "already complete" has to return the same instance
     * rather than an equal copy.
     */
    @Test
    void backfillingACompleteProfileChangesNothingAndAllocatesNothing() {
        Account complete = new Account(
                "ada", "hash", AccountRole.USER, 0, null, "ada@example.com", true, NOW);

        assertThat(complete.withProfileBackfilled("other@example.com", NOW.plusSeconds(60)))
                .isSameAs(complete);
    }

    @Test
    void backfillingFillsOnlyTheHalfOfTheProfileThatIsMissing() {
        Account halfRecorded = new Account(
                "ada", "hash", AccountRole.USER, 0, null, "kept@example.com", true, null);

        Account backfilled =
                halfRecorded.withProfileBackfilled("ignored@example.com", NOW);

        assertThat(backfilled.email()).isEqualTo("kept@example.com");
        assertThat(backfilled.createdAt()).isEqualTo(NOW);
    }

    /**
     * A backfill is administrative bookkeeping on a row that predates two
     * columns. It must not disturb the login history, which the login path owns
     * and may have written a moment earlier.
     */
    @Test
    void backfillingPreservesTheFailureRunAndLockout() {
        Account locked = failTimes(ACCOUNT, 3, NOW);

        Account backfilled = locked.withProfileBackfilled("ada@example.com", NOW);

        assertThat(backfilled.failedLoginAttempts()).isEqualTo(3);
        assertThat(backfilled.lockedUntil()).isEqualTo(locked.lockedUntil());
        assertThat(backfilled.isLocked(NOW)).isTrue();
    }

    /** The two refusal mechanisms are independent; neither implies the other. */
    @Test
    void aDisabledAccountIsNotLockedAndALockedAccountIsNotDisabled() {
        Account disabled = new Account(
                "ada", "hash", AccountRole.USER, 0, null, "ada@example.com", false, NOW);
        Account locked = failTimes(ACCOUNT, 3, NOW);

        assertThat(disabled.isLocked(NOW)).isFalse();
        assertThat(locked.enabled()).isTrue();
    }

    @Test
    void theLoginTransitionsCarryTheProfileThrough() {
        Account seeded = new Account(
                "ada", "hash", AccountRole.USER, 0, null, "ada@example.com", false, NOW);

        Account afterFailure = seeded.withFailureRecorded(POLICY, NOW);
        Account afterSuccess = afterFailure.withSuccessfulLogin();

        assertThat(afterFailure.email()).isEqualTo("ada@example.com");
        assertThat(afterFailure.createdAt()).isEqualTo(NOW);
        assertThat(afterFailure.enabled()).isFalse();
        assertThat(afterSuccess.email()).isEqualTo("ada@example.com");
        assertThat(afterSuccess.createdAt()).isEqualTo(NOW);
        assertThat(afterSuccess.enabled()).isFalse();
    }
}
