package com.example.backend.scim.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence port for SCIM Users and the resource rows that carry their identity. */
public interface ScimUserRepository {

    /**
     * Creates the resource row and the User row together, and reports the User as
     * stored.
     *
     * <p>One method rather than a {@code save} that could also update, because
     * creation is the only write this ticket performs and a port operation that can
     * do both would have to guess which was meant. Replacement arrives with the
     * conditional-write ticket and will be its own narrow operation, for the reason
     * the account port's {@code updateEnabled} is narrow: a full-row write races
     * anything else that touches the row.
     *
     * @throws DuplicateUserNameException when a live User already holds the
     *                                    normalized {@code userName}. Thrown from
     *                                    the adapter on the constraint violation
     *                                    rather than decided by a prior read, so two
     *                                    concurrent creates cannot both pass the
     *                                    check and then both insert.
     */
    ScimUser create(ScimUser user);

    /** The live User with this id, or empty — including for an id of a Group. */
    Optional<ScimUser> findById(UUID id);

    /**
     * One page of live Users, ordered by the normalized {@code userName}.
     *
     * <p>Ordering is the port's promise rather than the caller's sort, because only
     * the adapter can push it into the query, and a stable order is what makes
     * stateless paging return each resource once. The normalized form is the sort
     * key so the order does not depend on case.
     */
    List<ScimUser> findPage(ScimPageRequest page);

    /**
     * How many live Users there are, irrespective of the page.
     *
     * <p>Separate from {@link #findPage} because {@code totalResults} must be
     * reported even for {@code count=0}, where there is no page to count.
     */
    long countAll();
}
