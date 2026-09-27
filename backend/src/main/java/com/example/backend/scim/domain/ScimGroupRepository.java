package com.example.backend.scim.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence port for SCIM Groups, their membership, and the authority it confers. */
public interface ScimGroupRepository {

    /**
     * Creates the resource row, the Group row and its memberships together, and reports
     * the Group as stored.
     *
     * <p>Advances the version of every User the new Group names, for the reason
     * {@link #replace} does: a create WITH members is a membership change for those Users,
     * whose rendered {@code groups} gains an entry, and a representation that changed behind
     * an unmoved ETag is one a connector will never re-read. The new Group's own version is 1
     * by definition and is not advanced.
     *
     * @throws DuplicateDisplayNameException when a live Group already holds the normalized
     *                                       {@code displayName}
     * @throws UnknownGroupMemberException    when a member id does not name a live User —
     *                                       which covers a Group's id and an id that names
     *                                       nothing, because the membership row's foreign
     *                                       key points at the User table
     */
    ScimGroup create(ScimGroup group);

    /**
     * Creates the Group the deployment reserves — the Admin group — and reports it with its
     * marker set.
     *
     * <p>A separate operation rather than a reservation component on the value passed to
     * {@link #create}, for the reason the User port splits the two: a single {@code create}
     * that read a reservation off its argument would be reachable from the SCIM create use
     * case, and the only thing stopping a connector from minting a Group that claims
     * administrative authority would be that the use case happens to pass null.
     *
     * @throws DuplicateDisplayNameException when a live Group already holds the normalized
     *                                       {@code displayName}, or when the reservation is
     *                                       already held — seeding treats either as "it is
     *                                       already there"
     */
    ScimGroup createReserved(ScimGroup group, ReservedResourceName reservedName);

    /** The live Group with this id, or empty — including for an id of a User. */
    Optional<ScimGroup> findById(UUID id);

    /**
     * The Group the deployment reserves under this name, or empty before seeding has run.
     *
     * <p>How authority is derived: the Admin group is resolved through its reservation and
     * never through its {@code displayName}, so derivation does not depend on an attribute
     * that — for any other Group — a connector could change.
     */
    Optional<ScimGroup> findByReservedName(ReservedResourceName reservedName);

    /**
     * Replaces everything a connector may write — the display name and the whole
     * membership — and advances the version of every resource the change is visible in.
     *
     * <p><strong>The version bumps are this port's promise, not the caller's.</strong>
     * A membership change advances the Group's version and the version of every User
     * added or removed, because a User's rendered {@code groups} attribute changed. A
     * display-name change advances the Group's version and the version of every CURRENT
     * member, because the label rendered inside their {@code groups} changed. Only the
     * adapter can do that in one statement per affected set, and a caller that had to
     * remember it would be a caller that eventually forgets — leaving a User whose
     * representation changed behind an ETag that says it did not.
     *
     * <p>Narrow on purpose, like the User port's login-state write: it touches the
     * attributes a Group write owns and nothing else, so it cannot revert the reservation
     * marker or the creation timestamp.
     *
     * <p>Returns the Group as STORED, whose version is the one the database computed —
     * empty when no Group has that id, which happens when it was deleted between the use
     * case's read and this write. Reporting absence rather than throwing keeps a race
     * indistinguishable from an ordinary {@code 404}, which is what a caller should see.
     *
     * @throws DuplicateDisplayNameException when another live Group already holds the
     *                                       normalized {@code displayName}
     * @throws UnknownGroupMemberException    when a member id does not name a live User
     */
    Optional<ScimGroup> replace(ScimGroup group);

    /**
     * Deletes the Group and its memberships, advancing the version of every User that was
     * a member — their {@code groups} attribute just lost an entry.
     *
     * <p>Reports whether a Group was there to delete, rather than throwing: the use case
     * turns absence into a {@code 404}, and a port that threw would make "already gone"
     * indistinguishable from a failure.
     *
     * <p>No tombstone is written here. Tombstones and reusable identifiers arrive with
     * their own ticket; this is a hard delete of the rows, which is what makes the
     * {@code displayName} immediately reusable in the meantime.
     *
     * @param now when the deletion happened, for the {@code lastModified} of the Users
     *            whose versions it advances. Passed in rather than read from a clock here,
     *            because a port that read the system clock could not be exercised without
     *            one.
     */
    boolean deleteById(UUID id, Instant now);

    /**
     * One page of live Groups, ordered by the normalized {@code displayName}.
     *
     * <p>Ordering is the port's promise rather than the caller's sort, because only the
     * adapter can push it into the query, and a stable total order is what makes stateless
     * paging return each resource once. The normalized form is unique, so it is total with
     * no tie-breaker needed.
     */
    List<ScimGroup> findPage(ScimPageRequest page);

    /**
     * How many live Groups there are, irrespective of the page.
     *
     * <p>Separate from {@link #findPage} because {@code totalResults} must be reported even
     * for {@code count=0}, where there is no page to count.
     */
    long countAll();

    /**
     * The Groups this User is a direct member of, for the read-only reverse view on User.
     *
     * <p>Returns references rather than Groups: rendering a User's {@code groups} needs the
     * id and the label and nothing else, and returning whole Groups would carry every
     * other member of every Group the User belongs to into a single User's read.
     *
     * <p>There is deliberately no write counterpart. The reverse view is computed, so a
     * submitted {@code groups} attribute is ignored — and it is ignored because no port
     * operation exists that could apply it, rather than because a handler remembers to
     * drop it.
     */
    List<ScimGroupReference> findGroupsOfUser(UUID userId);

    /**
     * Whether this User is a direct member of the Group reserved under this name.
     *
     * <p>The authority question, asked in one statement. A caller could load the Group and
     * scan its members instead, and the difference matters at login: that reads every
     * member of the Admin group to answer a question about one User.
     */
    boolean isMemberOfReservedGroup(UUID userId, ReservedResourceName reservedName);
}
