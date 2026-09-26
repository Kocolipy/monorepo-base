package com.example.backend.scim.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Database representation of the identity every SCIM resource has, whatever its type.
 *
 * <p>Its own table because the SCIM {@code id} namespace is shared across resource
 * types: a primary key here is what makes "no User and Group ever share an id" a
 * property of the schema rather than a property of how ids happen to be generated.
 * The version lives here for the same reason — every type versions identically, and
 * a conditional write locks this row whatever it is writing.
 */
@Entity
@Table(name = "scim_resources")
public class ScimResourceEntity {

    /** Assigned by the application before the INSERT, like every other id here. */
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    /**
     * {@code User} or {@code Group}, stored as the name RFC 7643 uses, because this
     * value is rendered into {@code meta.resourceType} unchanged. A string rather than
     * a mapped enum so the stored bytes are the wire value and a future rename of a
     * Java constant cannot change what is in the column.
     */
    @Column(nullable = false, length = 16, updatable = false)
    private String resourceType;

    @Column(nullable = false)
    private long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant lastModifiedAt;

    protected ScimResourceEntity() {
    }

    public ScimResourceEntity(
            UUID id,
            String resourceType,
            long version,
            Instant createdAt,
            Instant lastModifiedAt) {
        this.id = id;
        this.resourceType = resourceType;
        this.version = version;
        this.createdAt = createdAt;
        this.lastModifiedAt = lastModifiedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getResourceType() {
        return resourceType;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastModifiedAt() {
        return lastModifiedAt;
    }
}
