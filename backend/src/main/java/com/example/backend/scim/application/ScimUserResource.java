package com.example.backend.scim.application;

import com.example.backend.scim.domain.ScimUser;
import com.example.backend.scim.domain.ScimUserProfile;
import java.time.Instant;
import java.util.UUID;

/**
 * A SCIM User as everything outside this slice may see it.
 *
 * <p><strong>There is no field a credential could be written into</strong>, and that
 * is the whole reason the type exists. The SCIM adapter renders what it is given, so
 * "the password never appears in any response" is a property of this shape rather
 * than of the renderer's care: a future attribute added to the rendering cannot be
 * the hash, because the renderer never receives one.
 * {@code ArchitectureTest.a_scim_user_credential_never_reaches_a_web_adapter} keeps
 * the adapter from reaching past this to {@link ScimUser} itself.
 *
 * <p>{@code externalId} is resolved for ONE connector — the one that made the request
 * that produced this projection. It is a component here rather than a separate lookup
 * so a renderer cannot omit it by forgetting, and so no code path exists that renders
 * a resource with another connector's alias in it.
 *
 * @param id             the resource's stable id
 * @param profile        the stored profile attributes
 * @param externalId     the calling connector's alias, or null when it set none
 * @param version        the representation version, rendered as the ETag
 * @param createdAt      creation instant, UTC
 * @param lastModifiedAt last representation change, UTC
 */
public record ScimUserResource(
        UUID id,
        ScimUserProfile profile,
        String externalId,
        long version,
        Instant createdAt,
        Instant lastModifiedAt) {

    /**
     * The projection of a stored User for one connector.
     *
     * <p>Takes the alias as an argument rather than reading it, so the caller that
     * knows which connector is asking is the one that supplies it; a projection that
     * looked the alias up itself would need the connector's identity and could get it
     * wrong silently.
     */
    static ScimUserResource of(ScimUser user, String externalId) {
        return new ScimUserResource(
                user.id(),
                user.profile(),
                externalId,
                user.version(),
                user.createdAt(),
                user.lastModifiedAt());
    }
}
