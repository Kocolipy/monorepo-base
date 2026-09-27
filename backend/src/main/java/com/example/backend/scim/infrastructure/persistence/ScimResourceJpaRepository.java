package com.example.backend.scim.infrastructure.persistence;

import com.example.backend.scim.infrastructure.persistence.entity.ScimResourceEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data access to the row every SCIM resource's identity and version live in.
 *
 * <p>Shared by the User and Group adapters rather than owned by either, because the
 * version is shared: a Group's membership change advances the versions of Users, and a
 * User's deactivation advances its own. One place that knows how to advance a version is
 * what keeps the two adapters from growing two subtly different ways to do it.
 */
interface ScimResourceJpaRepository extends JpaRepository<ScimResourceEntity, UUID> {

    /**
     * The resource row, locked {@code FOR UPDATE} until the transaction ends.
     *
     * <p>The serialization point of a conditional write. A second writer holding the same
     * precondition blocks here until the first commits, and then — because a locking read in
     * Postgres returns the row as the lock holder left it — reads the version the first one
     * produced, so its precondition fails instead of both writes succeeding.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ScimResourceEntity r where r.id = :id")
    Optional<ScimResourceEntity> lockById(@Param("id") UUID id);

    /**
     * Advances the version of every named resource and records when it changed.
     *
     * <p>A bulk statement rather than a read-modify-write per row, and that is the point:
     * renaming a Group with two hundred members has to advance two hundred and one
     * versions, and doing it row by row would both cost two hundred round trips and leave
     * a window in which some members' ETags had moved and others' had not.
     *
     * <p>{@code version = version + 1} in the database rather than a value computed here,
     * so a concurrent bump cannot be lost: two statements incrementing the same row
     * serialize on it, where two writes of a locally-computed number would not.
     *
     * <p>The persistence context is flushed before and cleared after, for the reason the
     * account port's narrow writes were: the statement bypasses any managed copy of these
     * rows, so a stale one must not survive the call.
     *
     * @return how many rows were advanced
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update ScimResourceEntity r
               set r.version = r.version + 1,
                   r.lastModifiedAt = :now
             where r.id in :ids""")
    int advanceVersions(
            @Param("ids") Collection<UUID> ids, @Param("now") Instant now);
}
