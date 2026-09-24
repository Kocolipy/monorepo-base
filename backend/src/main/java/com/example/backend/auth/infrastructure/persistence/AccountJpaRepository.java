package com.example.backend.auth.infrastructure.persistence;

import com.example.backend.auth.infrastructure.persistence.entity.AccountEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data access to accounts used by authentication and startup seeding. */
interface AccountJpaRepository extends JpaRepository<AccountEntity, String> {

    List<AccountEntity> findAllByOrderByUsernameAsc();

    /**
     * Single-column write, so an administrative decision and the login path's own
     * bookkeeping cannot overwrite each other. The persistence context is flushed
     * before and cleared after: the statement bypasses any managed copy of this
     * row, so a stale one must not survive the call.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update AccountEntity a set a.enabled = :enabled where a.username = :username")
    int updateEnabled(@Param("username") String username, @Param("enabled") boolean enabled);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update AccountEntity a
               set a.failedLoginAttempts = :failedLoginAttempts,
                   a.lockedUntil = :lockedUntil
             where a.username = :username""")
    int updateLockout(
            @Param("username") String username,
            @Param("failedLoginAttempts") int failedLoginAttempts,
            @Param("lockedUntil") Instant lockedUntil);
}
