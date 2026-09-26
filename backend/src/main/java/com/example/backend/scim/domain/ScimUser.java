package com.example.backend.scim.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * A SCIM User as the directory holds it: identity, profile, credential and version.
 *
 * <p>{@code passwordHash} is the reason no caller outside this slice receives a
 * {@code ScimUser}. It is the only component that must never be rendered, and the
 * shape that keeps it from being rendered is that the SCIM adapter is handed
 * {@code ScimUserResource} instead — a projection with no field a hash could occupy.
 * {@code ArchitectureTest} holds that boundary, so a future controller cannot reach
 * around the projection by taking this type directly.
 *
 * <p>The hash is nullable, and that is a supported state rather than an incomplete
 * one: SCIM allows a User to be created with no {@code password}, and such a User
 * exists, can be read, can be put in a Group and cannot log in. Provisioning that
 * sets credentials later, or never — because the deployment authenticates against
 * something else — is the ordinary case, not a degraded one.
 *
 * <p>{@code version} is the resource's own monotonically increasing counter, not a
 * timestamp and not a hash of the body: two changes within one clock tick must
 * still produce two versions, and an ETag a client can compare has to change
 * whenever the representation does. It starts at 1, so every live resource has a
 * version a client could have been given.
 *
 * @param id             stable, non-reassignable, unique across Users and Groups
 * @param profile        the attributes a connector writes
 * @param passwordHash   the stored credential, or {@code null} for a credentialless
 *                       User; never a plaintext password
 * @param version        monotonic representation version, from 1
 * @param createdAt      when the resource was created, UTC
 * @param lastModifiedAt when its representation last changed, UTC
 */
public record ScimUser(
        UUID id,
        ScimUserProfile profile,
        String passwordHash,
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
        if (version < INITIAL_VERSION) {
            throw new IllegalArgumentException("a live SCIM User's version starts at 1");
        }
    }

    /**
     * A newly created User: version 1, and one timestamp used for both
     * {@code created} and {@code lastModified} because nothing has changed since.
     *
     * <p>Takes the hash rather than the password. Hashing happens in the create use
     * case, before this is called, which is what "hashed immediately" means in
     * practice: there is no constructor here that could hold a plaintext value even
     * briefly.
     */
    public static ScimUser created(
            UUID id, ScimUserProfile profile, String passwordHash, Instant now) {
        return new ScimUser(id, profile, passwordHash, INITIAL_VERSION, now, now);
    }

    /**
     * Whether a credential is configured. A boolean rather than exposure of the
     * hash: whether a User can authenticate at all is an operational question an
     * Admin projection answers, and answering it does not require the value.
     */
    public boolean hasPassword() {
        return passwordHash != null;
    }

    /** The form {@code userName} uniqueness is decided on. */
    public NormalizedUserName normalizedUserName() {
        return profile.normalizedUserName();
    }
}
