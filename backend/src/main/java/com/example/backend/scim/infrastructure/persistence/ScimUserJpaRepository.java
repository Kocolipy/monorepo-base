package com.example.backend.scim.infrastructure.persistence;

import com.example.backend.scim.infrastructure.persistence.entity.ScimUserEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * The User holding this normalized {@code userName}, or empty.
     *
     * <p>The login path's lookup, and the only one there is: the raw {@code userName}
     * column is deliberately not queried, because it is case-sensitive and the
     * uniqueness constraint is not — a lookup by it would refuse a correct password on
     * account of how the name was typed.
     */
    Optional<ScimUserEntity> findByNormalizedUserName(String normalizedUserName);

    /** The User the deployment reserves under this name, or empty before seeding has run. */
    Optional<ScimUserEntity> findByResource_ReservedName(String reservedName);

    /**
     * Writes the three authentication-state columns and nothing else.
     *
     * <p>Narrow for the reason the account port's writes were: the login path writes this
     * row on every rejected attempt, and a full-row write from a value read at the start
     * of the attempt could revert a profile change that landed in between.
     *
     * <p>It deliberately does not touch {@code scim_resources}. None of these three
     * columns is a SCIM attribute, so nothing a client can read has changed and the
     * resource's version must not move — otherwise every wrong password would invalidate
     * every connector's cached copy of the User.
     *
     * @return how many rows were written; zero when no User has that id
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update ScimUserEntity u
               set u.login.passwordHash = :passwordHash,
                   u.login.failedLoginAttempts = :failedLoginAttempts,
                   u.login.lockedAt = :lockedAt
             where u.resourceId = :id""")
    int updateLoginState(
            @Param("id") UUID id,
            @Param("passwordHash") String passwordHash,
            @Param("failedLoginAttempts") int failedLoginAttempts,
            @Param("lockedAt") Instant lockedAt);

    /**
     * Writes the {@code active} column alone. Unlike {@link #updateLoginState} the
     * caller must also advance the resource's version, because {@code active} IS a SCIM
     * attribute — see {@code ScimUserPersistenceAdapter#updateActive}, which does both in
     * one transaction.
     *
     * @return how many rows were written; zero when no User has that id
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update ScimUserEntity u set u.active = :active where u.resourceId = :id")
    int updateActive(@Param("id") UUID id, @Param("active") boolean active);
}
