package com.example.backend.auth.domain;

import java.time.Instant;

/**
 * A login account as understood by authentication, independent of persistence.
 *
 * <p>Besides its credentials an account carries the state of its recent login
 * history: how many consecutive failures have been recorded, and — once the
 * policy's limit is reached — the instant its lockout expires. Both are
 * behaviour here rather than in the caller, so "what counts as locked" has one
 * answer that a unit test can reach.
 */
public record Account(
        String username,
        String passwordHash,
        AccountRole role,
        int failedLoginAttempts,
        Instant lockedUntil) {

    /** A newly registered account: nothing failed yet, nothing locked. */
    public Account(String username, String passwordHash, AccountRole role) {
        this(username, passwordHash, role, 0, null);
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
                limitReached ? now.plus(policy.lockDuration()) : null);
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
        return new Account(username, passwordHash, role, 0, null);
    }
}
