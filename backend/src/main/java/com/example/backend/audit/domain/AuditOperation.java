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
     * An administrator lifted a lockout. There is one cause and so no cause to
     * carry: a lockout has no duration, so it never ends unrequested, and every
     * event of this operation names the administrator who ended it.
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
     * A connector queried the User collection — {@code GET /Users} or
     * {@code POST /Users/.search} — a bulk read, whatever it asked for and whatever came
     * back. The event carries how many resources the response held and the filter's shape.
     *
     * <p>Recorded for an empty result and for a single-resource page alike, because
     * what the event exists to make visible is a credential enumerating the
     * directory, and an enumeration that finds nothing is the same act as one that
     * finds everything. Retrieving ONE User by its id is not this operation and is
     * not recorded at all: it is the ordinary unit of provisioning traffic, and
     * recording it would bury the reads that matter under the reads that do not.
     */
    SCIM_USER_LIST,

    /**
     * A connector replaced a User's attributes (PUT) or patched them (PATCH), or was refused.
     * One operation for both verbs, as {@link #SCIM_GROUP_REPLACE} is: the stored change is the
     * same, and which attributes moved is carried as the event's changed paths — {@code password}
     * among them when the credential changed, never its value. A refusal carries its reason from
     * {@link AuditScimRefusal}; a reused password is {@code INVALID_VALUE}.
     */
    SCIM_USER_REPLACE,

    /**
     * A connector deleted a User, or was refused — the Bootstrap Admin cannot be deleted. The
     * event names the User by its stable id, which is what keeps the trail about this identity
     * readable after its {@code userName} has been reused by another. The sessions the deletion
     * ended are recorded after the commit as {@link #USER_SESSIONS_REVOKE}.
     */
    SCIM_USER_DELETE,

    /**
     * A User's sessions were ended after a committed write changed something they were issued
     * against — deactivation, a password change, a {@code userName} change, or the User's
     * deletion. Recorded after the
     * commit, as its own event, because that is when the revocation happens and only then is its
     * outcome known: {@code SUCCESS} when the session store ended them, {@code FAILURE} when it
     * could not, in which case the write stands and its sessions survive. The changed paths name
     * the attributes whose change caused it.
     */
    USER_SESSIONS_REVOKE,

    /**
     * A connector created a SCIM Group, or was refused one. The outcome tells the two
     * apart, and a refusal carries its reason as the event's error code, from
     * {@link AuditScimRefusal} — a create refused because a member id names no live User
     * is a broken integration, and it is only visible in the trail if the refusals are
     * recorded beside the successes.
     */
    SCIM_GROUP_CREATE,

    /**
     * A connector replaced a Group's attributes or membership, or was refused. One
     * operation for PUT and PATCH alike, because the stored change is the same and the
     * distinction a reader cares about is which attributes moved — which the event's
     * changed paths carry.
     *
     * <p>A refused rename of the Admin group, and a refused removal of the Bootstrap
     * Admin's membership from it, are both this operation with a {@code FAILURE} outcome
     * and {@link AuditScimRefusal#MUTABILITY}. That is deliberate: an attempt to
     * provision the deployment's recovery authority away is exactly what an administrator
     * reading the trail needs to see, and it is invisible if a refusal records nothing.
     */
    SCIM_GROUP_REPLACE,

    /** A connector deleted a Group, or was refused — the Admin group cannot be deleted. */
    SCIM_GROUP_DELETE,

    /**
     * A connector read the Group collection — a bulk read, whatever it asked for and
     * whatever came back, for the reason {@link #SCIM_USER_LIST} is recorded that way.
     * Retrieving ONE Group by its id is not this operation and is not recorded.
     */
    SCIM_GROUP_LIST,

    /**
     * A connector searched Users and Groups together through the base {@code /.search}
     * endpoint — one bulk read spanning both types, recorded once, for the reason
     * {@link #SCIM_USER_LIST} is recorded that way.
     */
    SCIM_RESOURCE_LIST,

    /**
     * The server created one of the resources it reserves for recovery — the Bootstrap
     * Admin, or the Admin group — on a database that did not have it.
     *
     * <p>Recorded with no actor, because there is none: seeding is the deployment
     * establishing its own recovery path at startup, not a principal acting. The event
     * exists because the appearance of an administrative identity is the single most
     * consequential thing that happens to this directory, and "it was always there" and
     * "it was created at 03:14 on a restart nobody expected" must be distinguishable.
     */
    SCIM_RESOURCE_SEED
}
