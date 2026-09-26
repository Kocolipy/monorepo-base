package com.example.backend.scim.application;

import com.example.backend.scim.domain.ScimUserProfile;

/**
 * A request to create a SCIM User, as the use case receives it.
 *
 * <p>{@code password} is the one plaintext value anywhere in this slice, and it lives
 * on a command that is consumed immediately: the create use case hashes it before the
 * resource is constructed, and {@link com.example.backend.scim.domain.ScimUser} has
 * no constructor that accepts a plaintext value at all. There is therefore no object
 * graph in which a plaintext password survives past the hashing call — which is what
 * "hashed immediately" has to mean to be checkable.
 *
 * <p>{@code externalId} is the calling connector's alias for the resource, or null
 * when it sent none. It is carried as part of the command rather than written by a
 * separate call, so the resource and its alias are created in one transaction.
 *
 * @param profile    the attributes to store
 * @param password   the plaintext credential, or null to create a credentialless User
 * @param externalId the connector's alias for the new resource, or null
 */
public record NewScimUser(ScimUserProfile profile, String password, String externalId) {
}
