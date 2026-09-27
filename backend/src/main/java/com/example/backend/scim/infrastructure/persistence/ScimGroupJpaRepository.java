package com.example.backend.scim.infrastructure.persistence;

import com.example.backend.scim.infrastructure.persistence.entity.ScimGroupEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data access to the SCIM Group table. */
interface ScimGroupJpaRepository extends JpaRepository<ScimGroupEntity, UUID> {

    /**
     * One page of Groups ordered by the normalized {@code displayName}, which is unique
     * and therefore a total order needing no tie-breaker — what stateless paging requires
     * to return each resource exactly once.
     */
    List<ScimGroupEntity> findAllByOrderByNormalizedDisplayNameAsc(Pageable page);

    /** The Group the deployment reserves under this name, or empty before seeding has run. */
    Optional<ScimGroupEntity> findByResource_ReservedName(String reservedName);

    /**
     * Writes the two name columns and nothing else.
     *
     * <p>Narrow for the reason every other write here is: it cannot revert the reservation
     * marker or the creation timestamp, which a full-row write assembled from a value read
     * earlier could.
     *
     * @return how many rows were written; zero when no Group has that id
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update ScimGroupEntity g
               set g.displayName = :displayName,
                   g.normalizedDisplayName = :normalizedDisplayName
             where g.resourceId = :id""")
    int updateDisplayName(
            @Param("id") UUID id,
            @Param("displayName") String displayName,
            @Param("normalizedDisplayName") String normalizedDisplayName);
}
