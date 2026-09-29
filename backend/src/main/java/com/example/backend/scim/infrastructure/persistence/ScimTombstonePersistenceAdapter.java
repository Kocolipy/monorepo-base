package com.example.backend.scim.infrastructure.persistence;

import com.example.backend.scim.domain.ScimResourceType;
import com.example.backend.scim.domain.ScimTombstoneRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Writes SCIM tombstones.
 *
 * <p>JDBC rather than JPA: a tombstone is one insert that is never read back by the
 * application, so an entity would be a mapped type with no reader. The statement joins the
 * caller's transaction through the shared data source, so a rolled-back delete leaves no
 * tombstone behind.
 */
@Repository
class ScimTombstonePersistenceAdapter implements ScimTombstoneRepository {

    /** A whole statement as a constant, never assembled; see the local Semgrep ruleset. */
    private static final String INSERT_TOMBSTONE = """
            INSERT INTO scim_tombstones (resource_id, resource_type, deleted_at)
            VALUES (?, ?, ?)""";

    private final JdbcTemplate jdbc;

    ScimTombstonePersistenceAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void record(ScimResourceType type, UUID id, Instant deletedAt) {
        jdbc.update(INSERT_TOMBSTONE, id, type.resourceTypeName(),
                OffsetDateTime.ofInstant(deletedAt, ZoneOffset.UTC));
    }
}
