package com.example.backend.scim.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Database representation of one connector's {@code externalId} alias for one SCIM
 * resource.
 *
 * <p>Mapped even though this ticket only ever deletes from the table, because
 * {@code ddl-auto: validate} then checks the mapping against the migration on every
 * context start — which is how a column this ticket's deletion depends on cannot be
 * quietly renamed by the migration that adds the resource tables.
 *
 * <p>The key is the pair, so a connector holds at most one alias per resource. The
 * VALUE is deliberately not unique: RFC 7643 allows two resources to carry the same
 * {@code externalId} within one client's namespace, and refusing that here would
 * reject a conformant provisioning client.
 */
@Entity
@Table(name = "scim_external_ids")
@IdClass(ScimExternalIdEntity.Key.class)
public class ScimExternalIdEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID connectorId;

    /**
     * The SCIM resource. No JPA association, because {@code scim_resources} does not
     * exist yet — the migration adds the foreign key when it does.
     */
    @Id
    @Column(nullable = false, updatable = false)
    private UUID resourceId;

    @Column(nullable = false, length = 256)
    private String externalId;

    protected ScimExternalIdEntity() {
    }

    public ScimExternalIdEntity(UUID connectorId, UUID resourceId, String externalId) {
        this.connectorId = connectorId;
        this.resourceId = resourceId;
        this.externalId = externalId;
    }

    public UUID getConnectorId() {
        return connectorId;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public String getExternalId() {
        return externalId;
    }

    /** The composite primary key: one alias per connector per resource. */
    public static class Key implements Serializable {

        private UUID connectorId;

        private UUID resourceId;

        protected Key() {
        }

        public Key(UUID connectorId, UUID resourceId) {
            this.connectorId = connectorId;
            this.resourceId = resourceId;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key key
                    && Objects.equals(connectorId, key.connectorId)
                    && Objects.equals(resourceId, key.resourceId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(connectorId, resourceId);
        }
    }
}
