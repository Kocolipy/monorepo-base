package com.example.backend.scim.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * One external directory integration, as this service knows it.
 *
 * <p>Deletion is a transition rather than a removal: {@code deletedAt} is set and
 * the row stays. An audit event names a connector by this id and is retained for
 * a year, so an id that stopped resolving would make the trail unreadable
 * precisely for the connector somebody is investigating. What deletion actually
 * costs the connector is its tokens and its {@code externalId} aliases, both
 * removed in the same transaction — see
 * {@code ConnectorAdministrationService.delete}.
 *
 * @param id          stable, non-reassignable identity, assigned before the row
 *                    exists because the audit event recording the creation names it
 * @param displayName what an Admin calls this integration; carries no authority
 * @param createdAt   when the connector was created
 * @param deletedAt   when it was deleted, or {@code null} while it is live
 */
public record ScimConnector(UUID id, String displayName, Instant createdAt, Instant deletedAt) {

    /** A new, live connector. */
    public static ScimConnector create(UUID id, String displayName, Instant createdAt) {
        return new ScimConnector(id, displayName, createdAt, null);
    }

    /** Whether this connector has been deleted and may no longer authenticate. */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * This connector, deleted. Idempotent: a connector already deleted keeps its
     * original deletion time, so a repeated delete does not rewrite history.
     */
    public ScimConnector deleted(Instant at) {
        return isDeleted() ? this : new ScimConnector(id, displayName, createdAt, at);
    }
}
