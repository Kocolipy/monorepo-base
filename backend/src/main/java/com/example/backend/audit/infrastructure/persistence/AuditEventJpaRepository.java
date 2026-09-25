package com.example.backend.audit.infrastructure.persistence;

import com.example.backend.audit.infrastructure.persistence.entity.AuditEventEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data access to the audit table.
 *
 * <p>Inherits {@code delete*} and {@code save} of an existing row from
 * {@link JpaRepository} and neither is reachable in practice: the application's
 * database role holds no {@code UPDATE} or {@code DELETE} on this table and the
 * table's trigger refuses the statement anyway, so a call to one of them fails
 * against the database rather than quietly succeeding. The narrow port
 * {@link com.example.backend.audit.domain.AuditEventRepository} is what the
 * application layer sees.
 */
interface AuditEventJpaRepository extends JpaRepository<AuditEventEntity, UUID> {
}
