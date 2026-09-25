package com.example.backend.auth.infrastructure.persistence;

import com.example.backend.auth.infrastructure.persistence.entity.AccountEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data access to accounts used by authentication and startup seeding. */
interface AccountJpaRepository extends JpaRepository<AccountEntity, UUID> {

    Optional<AccountEntity> findByUsername(String username);

    List<AccountEntity> findAllByOrderByUsernameAsc();

    /**
     * Single-column write, so an administrative decision and the login path's own
     * bookkeeping cannot overwrite each other. The persistence context is flushed
     * before and cleared after: the statement bypasses any managed copy of this
     * row, so a stale one must not survive the call. Keyed by {@code id} rather
     * than {@code username}: the stable id is the row's identity, and username is
     * a mutable attribute of it.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update AccountEntity a set a.enabled = :enabled where a.id = :id")
    int updateEnabled(@Param("id") UUID id, @Param("enabled") boolean enabled);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update AccountEntity a
               set a.failedLoginAttempts = :failedLoginAttempts,
                   a.lockedAt = :lockedAt
             where a.id = :id""")
    int updateLockout(
            @Param("id") UUID id,
            @Param("failedLoginAttempts") int failedLoginAttempts,
            @Param("lockedAt") Instant lockedAt);
}
