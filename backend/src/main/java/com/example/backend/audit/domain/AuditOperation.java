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
    ACCOUNT_ENABLE,

    /** An administrator created a SCIM connector. */
    CONNECTOR_CREATE,

    /**
     * An administrator deleted a SCIM connector, which revoked every token it held
     * and removed every {@code externalId} alias it owned.
     */
    CONNECTOR_DELETE,

    /** An administrator minted a new token for a connector. */
    CONNECTOR_TOKEN_ISSUE,

    /**
     * An administrator replaced a connector's token, shortening the old one's life
     * to the overlap window.
     */
    CONNECTOR_TOKEN_ROTATE,

    /**
     * A connector token stopped being accepted because an administrator said so —
     * individually, or as part of deleting the connector.
     */
    CONNECTOR_TOKEN_REVOKE,

    /**
     * A connector created a SCIM User, or was refused one. The outcome tells the two
     * apart, and a refusal carries its reason as the event's error code, from
     * {@link AuditScimRefusal} — a create a connector kept retrying against a
     * {@code userName} that already exists is a broken integration, and it is only
     * visible in the trail if the refusals are recorded beside the successes.
     */
    SCIM_USER_CREATE,

    /**
     * A connector read the User collection — a bulk read, whatever it asked for and
     * whatever came back.
     *
     * <p>Recorded for an empty result and for a single-resource page alike, because
     * what the event exists to make visible is a credential enumerating the
     * directory, and an enumeration that finds nothing is the same act as one that
     * finds everything. Retrieving ONE User by its id is not this operation and is
     * not recorded at all: it is the ordinary unit of provisioning traffic, and
     * recording it would bury the reads that matter under the reads that do not.
     */
    SCIM_USER_LIST
}
