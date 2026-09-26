package com.example.backend.scim.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence port for connector-scoped {@code externalId} aliases.
 *
 * <p>The whole port is connector-scoped, and every method takes the connector id
 * first for that reason: an alias belongs to one connector's namespace, and there is
 * deliberately no operation that reads or writes an alias without naming whose it
 * is. That is the isolation requirement expressed as a signature — a caller cannot
 * accidentally read another connector's alias, because no method returns one.
 */
public interface ScimExternalIdRepository {

    /**
     * Records this connector's alias for this resource, replacing any it already
     * held.
     *
     * <p>Replacing rather than adding, because the key is the pair: one connector has
     * one alias for one resource. The value is not unique within a connector, which
     * RFC 7643 permits, so nothing here refuses a value another resource also carries.
     */
    void put(UUID connectorId, UUID resourceId, String externalId);

    /** This connector's alias for this resource, or empty when it set none. */
    Optional<String> find(UUID connectorId, UUID resourceId);

    /** Deletes every alias this connector holds, and reports how many there were. */
    int deleteAllForConnector(UUID connectorId);
}
