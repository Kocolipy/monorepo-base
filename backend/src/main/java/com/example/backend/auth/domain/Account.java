package com.example.backend.auth.domain;

import java.time.Instant;

/**
 * A login account as understood by authentication, independent of persistence.
 *
 * <p>Besides its credentials an account carries two unrelated kinds of state.
 * Its recent login history — how many consecutive failures have been recorded,
 * and, once the policy's limit is reached, the instant its lockout expires — is
 * behaviour here rather than in the caller, so "what counts as locked" has one
 * answer that a unit test can reach. Its administrative profile — email, whether
 * it is enabled, when it was created — is plain data that only account
 * administration reads.
 *
 * <p>The two are deliberately separate. A lockout is automatic, temporary, and
 * imposed by the failure run; {@code enabled} is a standing decision that no
 * passage of time reverses. Collapsing them would make one of those two
 * behaviours unexpressible.
 *
 * <p>{@code passwordHash} is the reason no caller outside this slice receives an
 * {@code Account}: the administrative listing is served as
 * {@link com.example.backend.auth.application.AccountSummary}, which has no
 * field to leak it into.
 *
 * <p>{@code email} and {@code createdAt} are nullable for one reason only: a row
 * written before those columns existed has no value for them, and inventing one
 * would be worse than reporting that none is recorded. Startup seeding backfills
 * what it can (see
 * {@link com.example.backend.auth.application.AccountService#seedDefaults}).
 */
public record Account(
        String username,
        String passwordHash,
        AccountRole role,
        int failedLoginAttempts,
        Instant lockedUntil,
        String email,
        boolean enabled,
        Instant createdAt) {

    /**
     * A newly registered account: nothing failed yet, nothing locked, and no
     * profile recorded. Kept because most callers — every test of the lockout
     * rule among them — have no interest in the profile fields, and spelling out
     * eight arguments there would bury what each case is actually about.
     */
    public Account(String username, String passwordHash, AccountRole role) {
        this(username, passwordHash, role, 0, null, null, true, null);
    }

    /**
     * An account with a login history and no profile recorded — the shape the
     * lockout rule reasons about.
     *
     * <p>There is deliberately no sibling overload taking a profile instead: two
     * five-argument constructors distinguished only by the type of the fourth
     * parameter would compile a mistake as readily as the intent. A caller that
     * cares about the profile uses the canonical constructor and names all eight.
     */
    public Account(
            String username,
            String passwordHash,
            AccountRole role,
            int failedLoginAttempts,
            Instant lockedUntil) {
        this(username, passwordHash, role, failedLoginAttempts, lockedUntil, null, true, null);
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
                email,
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
        if (failedLoginAttempts == 0 && lockedUntil == null) {
            return this;
        }
        return new Account(username, passwordHash, role, 0, null, email, enabled, createdAt);
    }

    /**
     * The account with an administrative profile filled in where it had none.
     * Startup seeding's backfill for a row that predates those columns; anything
     * already recorded is left alone, credentials and login history included.
     */
    public Account withProfileBackfilled(String fallbackEmail, Instant fallbackCreatedAt) {
        if (email != null && createdAt != null) {
            return this;
        }
        return new Account(
                username,
                passwordHash,
                role,
                failedLoginAttempts,
                lockedUntil,
                email == null ? fallbackEmail : email,
                enabled,
                createdAt == null ? fallbackCreatedAt : createdAt);
    }
}
