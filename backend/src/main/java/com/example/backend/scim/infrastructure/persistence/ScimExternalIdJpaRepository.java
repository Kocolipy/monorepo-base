package com.example.backend.scim.infrastructure.persistence;

import com.example.backend.scim.infrastructure.persistence.entity.ScimExternalIdEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data access to the connector-scoped alias table. */
interface ScimExternalIdJpaRepository
        extends JpaRepository<ScimExternalIdEntity, ScimExternalIdEntity.Key> {

    /**
     * Deletes one connector's whole alias namespace in a single statement, returning
     * how many aliases there were.
     *
     * <p>A bulk delete rather than {@code deleteAll(findBy...)}: this runs inside the
     * connector-deletion transaction, where loading every alias to delete it
     * individually would read a namespace that can be large and would give a
     * half-finished loop somewhere to fail. Bound parameter, never concatenation.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ScimExternalIdEntity a WHERE a.connectorId = :connectorId")
    int deleteAllByConnectorId(@Param("connectorId") UUID connectorId);
}
