package com.example.backend.audit.domain;

/**
 * Why a self-service password change was refused, as a closed set — for the reason
 * {@link AuditRefusalReason} is one: the reason is a field later queries group by, and a free-text
 * reason is a place a submitted value could be written. No member carries either password.
 */
public enum AuditPasswordChangeRefusal {

    /** The submitted current password did not verify; the failure counts toward the lockout. */
    BAD_CURRENT_PASSWORD,

    /** A lockout was in force, so nothing was compared. */
    ACCOUNT_LOCKED,

    /** The User is inactive. */
    ACCOUNT_DISABLED,

    /** The new password is shorter than the policy's minimum. */
    TOO_SHORT,

    /** The new password is longer than the policy's maximum. */
    TOO_LONG,

    /** The new password equals or contains the {@code userName}. */
    CONTAINS_USER_NAME,

    /** The new password matches the current one or a retained previous one. */
    REUSED
}
