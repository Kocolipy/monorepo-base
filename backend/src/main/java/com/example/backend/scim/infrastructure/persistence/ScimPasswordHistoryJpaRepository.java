package com.example.backend.scim.infrastructure.persistence;

import com.example.backend.scim.infrastructure.persistence.entity.ScimPasswordHistoryEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data access to a User's remembered password hashes. */
interface ScimPasswordHistoryJpaRepository
        extends JpaRepository<ScimPasswordHistoryEntity, UUID> {

    /**
     * The User's entries, newest first. {@code id} breaks a tie between two entries set within
     * one clock tick, so the order — and therefore which entries a trim keeps — is total.
     */
    List<ScimPasswordHistoryEntity> findAllByUserIdOrderBySetAtDescIdDesc(UUID userId);
}
