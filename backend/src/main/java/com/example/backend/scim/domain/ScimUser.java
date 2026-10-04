package com.example.backend.scim.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * A SCIM User as the directory holds it, and the one login identity this application
 * has.
 *
 * <p>There is no separate account aggregate. The specification plan settled that "SCIM
 * User replaces Account and owns profile plus authentication state", and this record is
 * where that lands: the profile a connector writes, the {@link ScimLoginState} the
 * password-login surface writes, and the reservation marker that says whether the
 * resource may be written at all.
 *
 * <p>{@link ScimLoginState} is the reason no caller outside this slice receives a
 * {@code ScimUser}. It holds the password hash, which is the one component that must
 * never be rendered, and the shape that keeps it from being rendered is that the SCIM
 * adapter is handed {@code ScimUserResource} instead — a projection with no field a hash
 * could occupy. {@code ArchitectureTest} holds that boundary, so a future controller
 * cannot reach around the projection by taking this type directly.
 *
 * <p>{@code version} is the resource's own monotonically increasing counter, not a
 * timestamp and not a hash of the body: two changes within one clock tick must still
 * produce two versions, and an ETag a client can compare has to change whenever the
 * representation does. It starts at 1, so every live resource has a version a client
 * could have been given.
 *
 * <p>Notably, a login does <em>not</em> change it. A failure run and a lock instant are
 * not SCIM attributes, so writing them changes nothing a client can read and must not
 * move the version or {@code meta.lastModified} — which is why they are grouped into
 * {@link ScimLoginState} and written through their own narrow port operation.
 *
 * @param id             stable, non-reassignable, unique across Users and Groups
 * @param profile        the attributes a connector writes
 * @param login          the authentication state the login surface writes
 * @param reservedName   the reservation protecting this User from SCIM writes, or
 *                       {@code null} for an ordinary one
 * @param version        monotonic representation version, from 1
 * @param createdAt      when the resource was created, UTC
 * @param lastModifiedAt when its representation last changed, UTC
 */
public record ScimUser(
        UUID id,
        ScimUserProfile profile,
        ScimLoginState login,
        ReservedResourceName reservedName,
        long version,
        Instant createdAt,
        Instant lastModifiedAt) {

    /** The version every resource starts at. */
    public static final long INITIAL_VERSION = 1L;

    public ScimUser {
        if (id == null) {
            throw new IllegalArgumentException("a SCIM User has a stable id");
        }
        if (profile == null) {
            throw new IllegalArgumentException("a SCIM User has a profile");
        }
        if (login == null) {
            throw new IllegalArgumentException("a SCIM User has an authentication state");
        }
        if (version < INITIAL_VERSION) {
            throw new IllegalArgumentException("a live SCIM User's version starts at 1");
        }
    }

    /**
     * A newly created User: version 1, no login history, unreserved, and one timestamp
     * used for both {@code created} and {@code lastModified} because nothing has changed
     * since.
     *
     * <p>Takes the hash rather than the password. Hashing happens in the create use case,
     * before this is called, which is what "hashed immediately" means in practice: there
     * is no constructor here that could hold a plaintext value even briefly.
     *
     * <p>Unreserved unconditionally, and there is deliberately no factory that produces a
     * reserved one: the marker is written by seeding's own statement, so no provisioning
     * path — present or future — can create a resource that protects itself from being
     * changed.
     */
    public static ScimUser created(
            UUID id, ScimUserProfile profile, String passwordHash, Instant now) {
        return new ScimUser(
                id, profile, ScimLoginState.of(passwordHash), null, INITIAL_VERSION, now, now);
    }

    /**
     * Whether SCIM writes against this User are refused.
     *
     * <p>Asked of the User rather than of the marker so a caller cannot get the test
     * wrong by comparing against the wrong reservation: there is one kind of reserved
     * User, and being reserved at all is what protects it.
     */
    public boolean isProtectedFromWrites() {
        return reservedName != null;
    }

    /**
     * Whether this is the deployment's recovery identity, and so must never be locked
     * however long its failure run grows.
     *
     * <p>Distinct from {@link #isProtectedFromWrites()} even though one marker decides
     * both, because the two rules are about different things and a future third
     * reservation would be protected from writes without being exempt from lockout.
     */
    public boolean isExemptFromLockout() {
        return reservedName == ReservedResourceName.BOOTSTRAP_ADMIN;
    }

    /**
     * Whether the dormancy job must leave this User alone — neither lock it nor remove its mapped
     * Group memberships, however long it has gone without authenticating.
     *
     * <p>The Bootstrap Admin, read off the same marker as the lockout exemption and for the same
     * reason: it is the recovery path for exactly the moment the external directory is
     * unavailable, and a recovery identity that expired while nobody needed it is not one.
     */
    public boolean isExemptFromDormancy() {
        return reservedName == ReservedResourceName.BOOTSTRAP_ADMIN;
    }

    /**
     * The instant dormancy is measured from: the last successful login, explicit reactivation or
     * administrator's Unlock, or — for a User that has had none of those — its creation.
     *
     * <p>The fallback is what keeps a User provisioned without a password from being dormant
     * the moment it exists: it has never authenticated, but it has also not had the chance to.
     */
    public Instant dormancyBasis() {
        Instant lastAuthenticatedAt = login.lastAuthenticatedAt();
        return lastAuthenticatedAt == null ? createdAt : lastAuthenticatedAt;
    }

    /**
     * Whether this User's dormancy basis lies strictly before {@code cutoff} — that is, whether
     * it has gone longer than the window {@code cutoff} was computed from without
     * authenticating. A basis exactly at the cutoff is not yet dormant.
     */
    public boolean isDormantAt(Instant cutoff) {
        return dormancyBasis().isBefore(cutoff);
    }
}
