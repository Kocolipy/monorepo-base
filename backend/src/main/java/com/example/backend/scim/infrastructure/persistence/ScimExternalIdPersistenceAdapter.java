package com.example.backend.scim.infrastructure.persistence;

import com.example.backend.scim.domain.ScimExternalIdRepository;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Maps the connector-scoped alias port onto JPA. */
@Repository
class ScimExternalIdPersistenceAdapter implements ScimExternalIdRepository {

    private final ScimExternalIdJpaRepository aliases;

    ScimExternalIdPersistenceAdapter(ScimExternalIdJpaRepository aliases) {
        this.aliases = aliases;
    }

    @Override
    public int deleteAllForConnector(UUID connectorId) {
        return aliases.deleteAllByConnectorId(connectorId);
    }
}
