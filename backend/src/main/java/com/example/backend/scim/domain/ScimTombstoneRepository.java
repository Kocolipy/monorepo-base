package com.example.backend.scim.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistence port for SCIM tombstones — what a DELETE leaves behind.
 *
 * <p>Write-only, on purpose. A tombstone holds a stable id, the kind of resource it was and
 * when it went, and nothing else; no use case reads one back, because nothing may depend on
 * one. In particular uniqueness never consults tombstones, so a former {@code userName} or
 * {@code displayName} is free the moment its resource is deleted (RFC 7644 §3.6).
 */
public interface ScimTombstoneRepository {

    /**
     * Records that the resource with this id was deleted, in the caller's transaction, so a
     * delete that rolls back leaves no tombstone and a tombstone never outlives a live resource.
     */
    void record(ScimResourceType type, UUID id, Instant deletedAt);
}
