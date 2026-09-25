package com.example.backend.audit.domain;

/**
 * What an audit event says happened, as a closed set.
 *
 * <p>A closed set rather than a string because the operation is the field every
 * later query groups by, and because a free-text operation is a place a caller's
 * value could be written. Every member below corresponds to a behaviour this
 * service already has; nothing is added here in anticipation of one.
 */
public enum AuditOperation {

    /** Submitted credentials were accepted and a session was issued. */
    LOGIN_SUCCESS,

    /**
     * Submitted credentials were refused — wrong password, unknown username,
     * locked account, disabled account. Which of those is carried as the event's
     * error code, from {@link AuditRefusalReason}.
     */
    LOGIN_FAILURE,

    /** A session was ended by its holder. */
    LOGOUT,

    /** A failure run reached the configured limit and closed the account. */
    LOCKOUT_SET,

    /**
     * A lockout ended. Whether it ran out or an administrator lifted it is
     * carried as the event's error code, from {@link AuditLockoutLift} — the two
     * are the same state transition reached two ways, and collapsing them into
     * one operation while keeping the cause would lose nothing a reader needs.
     */
    LOCKOUT_LIFT,

    /** An administrator closed an account to logins. */
    ACCOUNT_DISABLE,

    /** An administrator reopened an account to logins. */
    ACCOUNT_ENABLE
}
