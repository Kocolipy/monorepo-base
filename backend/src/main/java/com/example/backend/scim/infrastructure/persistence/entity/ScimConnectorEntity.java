package com.example.backend.scim.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Database representation of a SCIM connector. */
@Entity
@Table(name = "scim_connectors")
public class ScimConnectorEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String displayName;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Null while the connector is live. Deletion is a transition, not a DELETE. */
    @Column
    private Instant deletedAt;

    protected ScimConnectorEntity() {
    }

    public ScimConnectorEntity(
            UUID id, String displayName, Instant createdAt, Instant deletedAt) {
        this.id = id;
        this.displayName = displayName;
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
