package com.example.backend.scim.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Database representation of one remembered password hash of one User.
 *
 * <p>No JPA association to the User: the history is only ever read and trimmed by user id, and
 * it is deleted with the User by the foreign key's cascade, which an association would not add
 * anything to. Nothing is updatable — a history entry is appended and eventually deleted, never
 * changed.
 */
@Entity
@Table(name = "scim_user_password_history")
public class ScimPasswordHistoryEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, updatable = false, length = 256)
    private String passwordHash;

    @Column(nullable = false, updatable = false)
    private Instant setAt;

    protected ScimPasswordHistoryEntity() {
    }

    public ScimPasswordHistoryEntity(UUID id, UUID userId, String passwordHash, Instant setAt) {
        this.id = id;
        this.userId = userId;
        this.passwordHash = passwordHash;
        this.setAt = setAt;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
