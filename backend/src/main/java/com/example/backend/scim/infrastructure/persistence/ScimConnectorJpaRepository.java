package com.example.backend.scim.infrastructure.persistence;

import com.example.backend.scim.infrastructure.persistence.entity.ScimConnectorEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data access to the connector table. */
interface ScimConnectorJpaRepository extends JpaRepository<ScimConnectorEntity, UUID> {

    /**
     * Live connectors only, alphabetically. {@code deletedAt IS NULL} is the whole
     * definition of live, expressed as a derived query so no caller can forget the
     * predicate.
     */
    List<ScimConnectorEntity> findByDeletedAtIsNullOrderByDisplayNameAsc();
}
