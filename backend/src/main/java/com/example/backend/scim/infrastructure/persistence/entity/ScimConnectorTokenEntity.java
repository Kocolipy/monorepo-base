package com.example.backend.scim.infrastructure.persistence.entity;

import com.example.backend.scim.domain.ConnectorTokenScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Database representation of a connector token.
 *
 * <p>{@code tokenHash} is the digest of the complete presented value and is the
 * only trace of the credential that exists. There is no column for the value
 * itself, which is what "shown once" means at rest.
 *
 * <p>{@code originalExpiresAt} is {@code updatable = false} while {@code expiresAt}
 * is not, because rotation moves exactly one of them. The database says the same
 * thing more strongly, in {@code ck_scim_connector_tokens_expiry_never_extended}:
 * an {@code expires_at} past the issued expiry is refused whatever this mapping
 * permits.
 */
@Entity
@Table(name = "scim_connector_tokens")
public class ScimConnectorTokenEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID connectorId;

    @Column(nullable = false, updatable = false, length = 64)
    private String lookupId;

    @Column(nullable = false, updatable = false)
    private byte[] tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 16)
    private ConnectorTokenScope scope;

    @Column(nullable = false, updatable = false)
    private Instant issuedAt;

    /** Brought forward by a rotation's overlap window; never pushed back. */
    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false, updatable = false)
    private Instant originalExpiresAt;

    @Column
    private Instant revokedAt;

    @Column
    private UUID replacedByTokenId;

    protected ScimConnectorTokenEntity() {
    }

    public ScimConnectorTokenEntity(
            UUID id,
            UUID connectorId,
            String lookupId,
            byte[] tokenHash,
            ConnectorTokenScope scope,
            Instant issuedAt,
            Instant expiresAt,
            Instant originalExpiresAt,
            Instant revokedAt,
            UUID replacedByTokenId) {
        this.id = id;
        this.connectorId = connectorId;
        this.lookupId = lookupId;
        this.tokenHash = tokenHash;
        this.scope = scope;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.originalExpiresAt = originalExpiresAt;
        this.revokedAt = revokedAt;
        this.replacedByTokenId = replacedByTokenId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getConnectorId() {
        return connectorId;
    }

    public String getLookupId() {
        return lookupId;
    }

    public byte[] getTokenHash() {
        return tokenHash;
    }

    public ConnectorTokenScope getScope() {
        return scope;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getOriginalExpiresAt() {
        return originalExpiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public UUID getReplacedByTokenId() {
        return replacedByTokenId;
    }
}
