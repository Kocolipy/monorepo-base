package com.example.backend.auth.domain;

import java.time.Instant;

/**
 * A login account as understood by authentication, independent of persistence.
 *
 * <p>Besides its credentials an account carries two unrelated kinds of state.
 * Its recent login history — how many consecutive failures have been recorded,
 * and, once the policy's limit is reached, the instant its lockout expires — is
 * behaviour here rather than in the caller, so "what counts as locked" has one
 * answer that a unit test can reach. Its administrative profile — whether it is
 * enabled, when it was created — is plain data that only account administration
 * reads.
 *
 * <p>The two are deliberately separate. A lockout is automatic, temporary, and
 * imposed by the failure run; {@code enabled} is a standing decision that no
 * passage of time reverses. Collapsing them would make one of those two
 * behaviours unexpressible.
 *
 * <p>{@code passwordHash} is the reason no caller outside this slice receives an
 * {@code Account}: the administrative listing is served as
 * {@link com.example.backend.auth.application.AccountSummary}, which has no
 * field to leak it into. It is nullable: a credentialless account — one an
 * administrator has created or reset without setting a password — carries no
 * hash at all, and is refused at login like a wrong-password attempt rather
 * than being unable to exist.
 *
 * <p>{@code createdAt} is nullable for one reason only: a row written before the
 * column existed has no value for it, and inventing one would be worse than
 * reporting that none is recorded. Startup seeding backfills what it can (see
 * {@link com.example.backend.auth.application.AccountService#seedDefaults}).
 */
public record Account(
        String username,
        String passwordHash,
        AccountRole role,
        int failedLoginAttempts,
        Instant lockedUntil,
        boolean enabled,
        Instant createdAt) {

    /**
     * A newly registered account: nothing failed yet, nothing locked, and no
     * creation timestamp recorded. Kept because most callers — every test of the
     * lockout rule among them — have no interest in the profile fields, and
     * spelling out seven arguments there would bury what each case is actually
     * about.
     */
    public Account(String username, String passwordHash, AccountRole role) {
        this(username, passwordHash, role, 0, null, true, null);
    }

    /**
     * An account with a login history and no creation timestamp recorded — the
     * shape the lockout rule reasons about.
     */
    public Account(
            String username,
            String passwordHash,
            AccountRole role,
            int failedLoginAttempts,
            Instant lockedUntil) {
        this(username, passwordHash, role, failedLoginAttempts, lockedUntil, true, null);
    }

    /**
     * Whether the account is closed to logins at {@code now}. A lockout that has
     * run out is not a lockout: the recorded instant is kept until the next login
     * either resets it or replaces it, so expiry has to be decided by comparison
     * rather than by the field being absent.
     */
    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    /**
     * The account as it stands after one rejected login attempt.
     *
     * <p>A locked account is returned unchanged: the window is a fixed penalty,
     * so attempts made during it neither count nor extend it. Otherwise the
     * failure is counted — from zero when an earlier lockout has since expired,
     * so the account gets a fresh run of attempts — and reaching the policy's
     * limit locks the account from {@code now}.
     */
    public Account withFailureRecorded(LockoutPolicy policy, Instant now) {
        if (isLocked(now)) {
            return this;
        }
        int attempts = (lockedUntil == null ? failedLoginAttempts : 0) + 1;
        boolean limitReached = attempts >= policy.maxAttempts();
        return new Account(
                username,
                passwordHash,
                role,
                attempts,
                limitReached ? now.plus(policy.lockDuration()) : null,
                enabled,
                createdAt);
    }

    /**
     * The account as it stands after a login it accepted: the failure run is
     * over, so the count returns to zero and any expired lockout is discarded.
     * An account with nothing to clear is returned as-is, so a caller can use
     * the identity of the result to avoid a pointless write.
     */
    public Account withSuccessfulLogin() {
        return withFailureRunCleared();
    }

    /**
     * The account with a creation timestamp filled in where it had none. Startup
     * seeding's backfill for a row that predates that column; anything already
     * recorded is left alone, credentials and login history included.
     */
    public Account withCreatedAtBackfilled(Instant fallbackCreatedAt) {
        if (createdAt != null) {
            return this;
        }
        return new Account(
                username,
                passwordHash,
                role,
                failedLoginAttempts,
                lockedUntil,
                enabled,
                fallbackCreatedAt);
    }

    /**
     * The account with its administrative standing changed, and nothing else.
     *
     * <p>Notably not the lockout: enabling an account does not unlock it, and
     * disabling one does not clear its failure run. The two are separate
     * capabilities because they answer different questions — whether an account
     * is permitted at all, and whether it is being penalised right now — and an
     * administrator restoring access after a suspension is not thereby deciding
     * that a run of failed logins did not happen.
     *
     * <p>Returned unchanged when it already stands this way, so a caller can use
     * the identity of the result to avoid a pointless write.
     */
    public Account withEnabled(boolean shouldBeEnabled) {
        if (enabled == shouldBeEnabled) {
            return this;
        }
        return new Account(
                username,
                passwordHash,
                role,
                failedLoginAttempts,
                lockedUntil,
                shouldBeEnabled,
                createdAt);
    }

    /**
     * The account with its lockout lifted: the failure run ends and any recorded
     * instant is discarded, exactly as an accepted login would leave it.
     *
     * <p>This is how a lockout ends early. Left alone it ends by itself when
     * {@code lockedUntil} passes, so this exists for the case where an
     * administrator has established that the failures were the account holder's
     * own mistake and will not make them wait it out. It says nothing about
     * whether the account is enabled.
     */
    public Account withLockoutCleared() {
        return withFailureRunCleared();
    }

    /**
     * The single implementation of "no failures recorded, no lockout standing".
     * Two callers reach it for unrelated reasons — an accepted login and an
     * administrator lifting a lockout — and naming it after neither is what keeps
     * the other from reading as a side effect of the first.
     */
    private Account withFailureRunCleared() {
        if (failedLoginAttempts == 0 && lockedUntil == null) {
            return this;
        }
        return new Account(username, passwordHash, role, 0, null, enabled, createdAt);
    }
}
