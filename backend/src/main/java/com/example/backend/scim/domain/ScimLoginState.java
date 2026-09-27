package com.example.backend.scim.domain;

import java.time.Instant;

/**
 * A User's authentication state: the stored credential, the run of consecutive
 * failed logins, and the instant a lock was imposed.
 *
 * <p>Grouped into a record of its own for the reason {@link ScimUserProfile} is: this
 * is exactly the set the login path writes, and the profile is exactly the set a SCIM
 * replacement writes. Keeping them apart is what lets each be written as one
 * assignment, and is what makes "a failed login does not change the resource's
 * representation" expressible — none of these three components is a SCIM attribute,
 * so a write here does not bump the version or {@code meta.lastModified}.
 *
 * <p>{@code passwordHash} is nullable, and that is a supported state rather than an
 * incomplete one: SCIM allows a User to be created with no {@code password}, and such
 * a User exists, can be read, can be put in a Group and cannot log in. It is never a
 * plaintext value — the create use case hashes before this record is built, so there
 * is no constructor here that could hold one even briefly.
 *
 * <p>{@code lockedAt} records <em>when</em> the lock was imposed and nothing about
 * when it ends, because it does not end on its own: there is no duration, no expiry
 * and no clock in the decision. That is what makes {@link #isLocked()} a question
 * about this value alone — a lock cannot be "in the past", so no caller has to agree
 * with the server about the time to agree about the state.
 *
 * @param passwordHash         the stored credential, or {@code null} for a
 *                             credentialless User
 * @param failedLoginAttempts  consecutive rejected attempts, never negative
 * @param lockedAt             when a lock was imposed, or {@code null} when none is
 */
public record ScimLoginState(String passwordHash, int failedLoginAttempts, Instant lockedAt) {

    /** A User that cannot authenticate and has no history: what a SCIM create yields. */
    public static final ScimLoginState CREDENTIALLESS = new ScimLoginState(null, 0, null);

    public ScimLoginState {
        if (failedLoginAttempts < 0) {
            throw new IllegalArgumentException("a failure run cannot be negative");
        }
    }

    /**
     * A User with a credential and no login history.
     *
     * <p>Takes the hash rather than the password: hashing happens in the use case that
     * received the plaintext, which is what "hashed immediately" means in practice.
     */
    public static ScimLoginState of(String passwordHash) {
        return passwordHash == null ? CREDENTIALLESS : new ScimLoginState(passwordHash, 0, null);
    }

    /**
     * Whether a credential is configured. A boolean rather than exposure of the hash:
     * whether a User can authenticate at all is an operational question an
     * administrative projection answers, and answering it does not require the value.
     */
    public boolean hasPassword() {
        return passwordHash != null;
    }

    /**
     * Whether the User is closed to logins by a lockout. A lock stands until an
     * administrator lifts it, so the recorded instant being present <em>is</em> the
     * state: nothing is compared to a clock, and no passage of time changes the answer.
     */
    public boolean isLocked() {
        return lockedAt != null;
    }

    /**
     * The state after one rejected login attempt.
     *
     * <p>A locked User is returned unchanged: the lock is already in force, so attempts
     * made against it neither count nor deepen it. Otherwise the failure is counted, and
     * reaching the policy's limit locks the User as of {@code now} — permanently, in the
     * sense that no later call here and no elapsed time lifts it. Only
     * {@link #withFailureRunCleared()} does.
     */
    public ScimLoginState withFailureRecorded(LockoutPolicy policy, Instant now) {
        if (isLocked()) {
            return this;
        }
        int attempts = failedLoginAttempts + 1;
        return new ScimLoginState(
                passwordHash, attempts, attempts >= policy.maxAttempts() ? now : null);
    }

    /**
     * The state after one rejected login attempt that must never lock the User: the
     * failure run lengthens and no lock is imposed, whatever the policy's limit says.
     *
     * <p>This is the Bootstrap Admin's path, and the reason it is a transition of its own
     * rather than a flag on {@link #withFailureRecorded}: with no automatic lift, a
     * locked recovery identity is an unrecoverable deployment, so "counted but never
     * locked" is a distinct rule and is named as one. The run is still counted because
     * the failures are still evidence — the audit trail records each of them either way.
     */
    public ScimLoginState withFailureCounted() {
        return new ScimLoginState(passwordHash, failedLoginAttempts + 1, lockedAt);
    }

    /**
     * The state with no failures recorded and no lockout standing.
     *
     * <p>Two callers reach it for unrelated reasons — an accepted login, and an
     * administrator lifting a lockout — and it is one method because "no failures, no
     * lock" is one state. A value with nothing to clear is returned as-is, so a caller
     * can use the identity of the result to avoid a pointless write.
     *
     * <p>A locked User never reaches here by the login route: it is refused before its
     * password is compared. So on that path this clears a run rather than a lock.
     */
    public ScimLoginState withFailureRunCleared() {
        if (failedLoginAttempts == 0 && lockedAt == null) {
            return this;
        }
        return new ScimLoginState(passwordHash, 0, null);
    }
}
