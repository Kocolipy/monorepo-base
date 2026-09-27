package com.example.backend.scim.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence port for SCIM Users and the resource rows that carry their identity. */
public interface ScimUserRepository {

    /**
     * Creates the resource row and the User row together, and reports the User as
     * stored.
     *
     * <p>Creates an UNRESERVED User. A reservation is applied only by
     * {@link #createReserved}, so no provisioning path can produce a resource that
     * protects itself from being changed.
     *
     * @throws DuplicateUserNameException when a live User already holds the
     *                                    normalized {@code userName}. Thrown from
     *                                    the adapter on the constraint violation
     *                                    rather than decided by a prior read, so two
     *                                    concurrent creates cannot both pass the
     *                                    check and then both insert.
     */
    ScimUser create(ScimUser user);

    /**
     * Creates a User the deployment reserves — the Bootstrap Admin — and reports it
     * with its marker set.
     *
     * <p>A separate operation rather than a reservation component on the value passed
     * to {@link #create}, because the two have different callers and only one of them
     * is allowed to exist: seeding. A single {@code create} that read a reservation off
     * its argument would be reachable from the SCIM create use case, and the only thing
     * stopping a connector from minting a protected resource would be that the use case
     * happens to pass null.
     *
     * @throws DuplicateUserNameException when a live User already holds the normalized
     *                                    {@code userName}, or when the reservation is
     *                                    already held — seeding treats either as "it is
     *                                    already there"
     */
    ScimUser createReserved(ScimUser user, ReservedResourceName reservedName);

    /** The live User with this id, or empty — including for an id of a Group. */
    Optional<ScimUser> findById(UUID id);

    /**
     * The live User holding this normalized {@code userName}, or empty.
     *
     * <p>The login path's lookup. It takes the normalized form rather than the submitted
     * string so the comparison happens on the value the unique constraint is built on: a
     * lookup by the raw {@code userName} would be case-sensitive and would refuse a
     * correct password because of how the name was typed.
     */
    Optional<ScimUser> findByNormalizedUserName(NormalizedUserName normalizedUserName);

    /** The User the deployment reserves under this name, or empty before seeding has run. */
    Optional<ScimUser> findByReservedName(ReservedResourceName reservedName);

    /**
     * Writes only the authentication-state columns of a User that already exists.
     *
     * <p>Narrow on purpose, and in two directions at once. It cannot revert a profile
     * attribute an administrator or a connector changed in between — the login path
     * writes this row on every rejected attempt, and a full-row write from a value read
     * at the start of the attempt would race them. And it deliberately does NOT advance
     * the version or {@code lastModified}: a failure run and a lock instant are not SCIM
     * attributes, so nothing a client can read has changed, and moving the ETag would
     * make every failed login invalidate every connector's cached copy.
     */
    void updateLoginState(UUID id, ScimLoginState loginState);

    /**
     * Writes only the {@code active} column, advancing the version and
     * {@code lastModified}.
     *
     * <p>Unlike {@link #updateLoginState}, this one DOES advance them, and the difference
     * is the whole reason the two are separate operations: {@code active} is a SCIM
     * attribute, so deactivating a User changes what a connector reads and must change
     * the ETag it reads it behind.
     *
     * @return the User as it now stands, or empty when no User has that id
     */
    Optional<ScimUser> updateActive(UUID id, boolean active, Instant now);

    /**
     * Every live User, ordered by the normalized {@code userName}.
     *
     * <p>For the administrative listing, which is a complete review of who can log in
     * rather than a page of a directory. Ordering is the port's promise because only the
     * adapter can push it into the query, and the normalized form is the key so the order
     * does not depend on case.
     */
    List<ScimUser> findAllOrderedByNormalizedUserName();

    /**
     * One page of live Users, ordered by the normalized {@code userName}.
     *
     * <p>Ordering is the port's promise rather than the caller's sort, because only the
     * adapter can push it into the query, and a stable order is what makes stateless
     * paging return each resource once. The normalized form is the sort key so the order
     * does not depend on case.
     */
    List<ScimUser> findPage(ScimPageRequest page);

    /**
     * How many live Users there are, irrespective of the page.
     *
     * <p>Separate from {@link #findPage} because {@code totalResults} must be reported
     * even for {@code count=0}, where there is no page to count.
     */
    long countAll();
}
