package com.example.backend.counter.infrastructure.persistence;

import com.example.backend.counter.infrastructure.persistence.entity.UserCounterEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data access to {@link UserCounterEntity}. Infrastructure detail; the
 * application layer talks to the domain port instead.
 */
interface UserCounterJpaRepository extends JpaRepository<UserCounterEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select counter from UserCounterEntity counter where counter.accountId = :accountId")
    Optional<UserCounterEntity> findByAccountIdForUpdate(@Param("accountId") UUID accountId);
}
