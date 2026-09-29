package com.example.backend.scim.domain;

import java.util.Set;
import java.util.UUID;

/**
 * Ending a User's sessions because a SCIM write changed something they were issued against.
 *
 * <p>A port in the directory's domain for a capability the login surface owns. Sessions are a
 * fact about the login surface, and the dependency between the two slices runs one way —
 * {@code auth} reaches the directory, never the reverse — so the SCIM write use case cannot name
 * the session port or the after-commit seam that live there. It names this instead, and the
 * login surface implements it.
 *
 * <p>The name says <em>after commit</em> because that is the whole contract, and a call site
 * should read as what it does (see {@code /docs/adr/0002-revoke-sessions-after-commit.md}): the
 * sessions end once the calling transaction commits, and not at all if it rolls back — so a
 * refused, stale or rolled-back write revokes nothing. The outcome of the revocation is audited by
 * the implementation, because only it knows whether the revocation succeeded.
 */
public interface ScimUserSessions {

    /** Why a write ends a User's sessions — each is a change a live session must not outlast. */
    enum Cause {
        /** {@code active} went from true to false. */
        DEACTIVATED,
        /** The password was set, changed or removed. */
        PASSWORD_CHANGED,
        /** {@code userName} changed. */
        USER_NAME_CHANGED,
        /** The User was deleted. */
        DELETED
    }

    /**
     * Ends every session the User holds once the current transaction commits.
     *
     * @param connectorId the connector whose write caused it, recorded as the actor
     * @param userId      the User whose sessions end
     * @param causes      why; never empty
     */
    void revokeAfterCommit(UUID connectorId, UUID userId, Set<Cause> causes);
}
