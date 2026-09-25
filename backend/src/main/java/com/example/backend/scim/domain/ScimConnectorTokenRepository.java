package com.example.backend.scim.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence port for connector tokens. */
public interface ScimConnectorTokenRepository {

    /** Writes a new token or replaces an existing one wholly. */
    void save(ScimConnectorToken token);

    /**
     * The token with this non-secret lookup id.
     *
     * <p>The only lookup the authentication path performs, and the reason a token
     * value has two halves: the secret one never appears in a query. Returns the
     * row whatever its state — expired and revoked tokens are found and then
     * refused, because a lookup that filtered them out would make "revoked" and
     * "never existed" two different amounts of work.
     */
    Optional<ScimConnectorToken> findByLookupId(String lookupId);

    Optional<ScimConnectorToken> findById(UUID id);

    /** Every token this connector has ever held, newest issue first. */
    List<ScimConnectorToken> findByConnectorIdOrderedByIssuedAtDesc(UUID connectorId);

    /**
     * Revokes every token the connector holds that is not already revoked, and
     * reports how many that was.
     *
     * <p>One statement rather than a read-modify-write loop: this runs inside the
     * connector-deletion transaction, where the point is that no token survives it.
     * A loop would also read every token into memory to write most of them back
     * unchanged.
     */
    int revokeAllForConnector(UUID connectorId, java.time.Instant revokedAt);
}
