package com.example.backend.scim.infrastructure.persistence;

import com.example.backend.scim.domain.ScimExternalIdRepository;
import com.example.backend.scim.infrastructure.persistence.entity.ScimExternalIdEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Maps the connector-scoped alias port onto JPA. */
@Repository
class ScimExternalIdPersistenceAdapter implements ScimExternalIdRepository {

    private final ScimExternalIdJpaRepository aliases;

    ScimExternalIdPersistenceAdapter(ScimExternalIdJpaRepository aliases) {
        this.aliases = aliases;
    }

    /**
     * Writes the alias, replacing any this connector already held for this resource.
     *
     * <p>{@code save} on a composite-key entity is an upsert here rather than a blind
     * insert: the key is {@code (connector, resource)}, so re-writing the same pair
     * replaces the value instead of adding a second row. That is the right semantics
     * for an attribute a connector may re-assert on every sync.
     */
    @Override
    public void put(UUID connectorId, UUID resourceId, String externalId) {
        aliases.save(new ScimExternalIdEntity(connectorId, resourceId, externalId));
    }

    /**
     * Deletes by the whole key, so only this connector's row for this resource can go. Absent
     * is not an error: removing an alias nobody set is a no-op, as RFC 7644 makes removing an
     * unassigned attribute.
     */
    @Override
    public void remove(UUID connectorId, UUID resourceId) {
        aliases.deleteById(new ScimExternalIdEntity.Key(connectorId, resourceId));
    }

    /**
     * This connector's alias for this resource.
     *
     * <p>The lookup is by the whole key, so no query here can return a row belonging to
     * a different connector. That is what makes alias isolation a property of the
     * access path rather than of a filter a caller has to remember to apply.
     */
    @Override
    public Optional<String> find(UUID connectorId, UUID resourceId) {
        return aliases.findById(new ScimExternalIdEntity.Key(connectorId, resourceId))
                .map(ScimExternalIdEntity::getExternalId);
    }

    @Override
    public int deleteAllForConnector(UUID connectorId) {
        return aliases.deleteAllByConnectorId(connectorId);
    }
}
