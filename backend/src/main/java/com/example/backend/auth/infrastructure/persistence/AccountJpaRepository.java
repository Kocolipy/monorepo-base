package com.example.backend.auth.infrastructure.persistence;

import com.example.backend.auth.infrastructure.persistence.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data access to accounts used by authentication and startup seeding. */
interface AccountJpaRepository extends JpaRepository<AccountEntity, String> {
}
