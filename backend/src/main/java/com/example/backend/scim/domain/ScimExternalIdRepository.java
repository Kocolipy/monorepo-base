package com.example.backend.scim.domain;

import java.util.UUID;

/**
 * Persistence port for connector-scoped {@code externalId} aliases.
 *
 * <p>Only the deletion is here. Reading and writing an alias belongs to the User
 * and Group use cases, which do not exist yet; what this ticket needs is that
 * deleting a connector takes its whole namespace with it, in the same transaction.
 * Adding the read methods now would be adding a port shape for a caller nobody has
 * written, and the resource table they would join against does not exist.
 */
public interface ScimExternalIdRepository {

    /** Deletes every alias this connector holds, and reports how many there were. */
    int deleteAllForConnector(UUID connectorId);
}
