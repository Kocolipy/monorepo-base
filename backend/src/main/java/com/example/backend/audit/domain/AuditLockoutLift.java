package com.example.backend.audit.domain;

/**
 * How a lockout ended.
 *
 * <p>Both causes reach the same state — no failures recorded, no lockout standing
 * — so they are one {@link AuditOperation#LOCKOUT_LIFT} carrying which of them it
 * was, rather than two operations a reader would have to remember to sum.
 */
public enum AuditLockoutLift {

    /**
     * The lockout window ran out. Nobody performs this, so it is observed rather
     * than commanded: the next login attempt against the account is the moment
     * the expiry becomes a fact the service acts on, and is when it is recorded.
     */
    EXPIRY,

    /** An administrator ended the lockout early. */
    UNLOCK
}
