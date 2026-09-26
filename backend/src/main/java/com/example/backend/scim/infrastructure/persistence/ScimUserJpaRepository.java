package com.example.backend.scim.infrastructure.persistence;

import com.example.backend.scim.infrastructure.persistence.entity.ScimUserEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data access to the SCIM User tables. */
interface ScimUserJpaRepository extends JpaRepository<ScimUserEntity, UUID> {

    /**
     * One page of Users ordered by the normalized {@code userName}.
     *
     * <p>Ordered in the query rather than by the caller, because stateless paging only
     * returns each resource once if the order is total and stable. The normalized form
     * is the key so the order does not depend on case — and it is unique, so it is a
     * total order with no tie-breaker needed.
     */
    List<ScimUserEntity> findAllByOrderByNormalizedUserNameAsc(Pageable page);
}
