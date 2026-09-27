package com.example.backend.scim.application;

import com.example.backend.scim.domain.ScimGroupReference;
import com.example.backend.scim.domain.ScimUser;
import com.example.backend.scim.domain.ScimUserProfile;
import java.time.Instant;
import java.util.List;
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
 * @param groups         the Groups this User is a direct member of — computed, read-only
 * @param externalId     the calling connector's alias, or null when it set none
 * @param version        the representation version, rendered as the ETag
 * @param createdAt      creation instant, UTC
 * @param lastModifiedAt last representation change, UTC
 */
public record ScimUserResource(
        UUID id,
        ScimUserProfile profile,
        List<ScimGroupReference> groups,
        String externalId,
        long version,
        Instant createdAt,
        Instant lastModifiedAt) {

    public ScimUserResource {
        groups = groups == null ? List.of() : List.copyOf(groups);
    }

    /**
     * The projection of a stored User for one connector.
     *
     * <p>Takes the alias and the Group memberships as arguments rather than reading them, so
     * the caller that knows which connector is asking is the one that supplies the alias; a
     * projection that looked it up itself would need the connector's identity and could get it
     * wrong silently.
     *
     * <p>The memberships are passed in for a different reason: they are the REVERSE view, and
     * a projection that resolved them would make every place a User is projected reach into
     * the Group port — including a listing, where that is N+1 queries. The use case resolves
     * them once and hands them over.
     */
    static ScimUserResource of(
            ScimUser user, List<ScimGroupReference> groups, String externalId) {
        return new ScimUserResource(
                user.id(),
                user.profile(),
                groups,
                externalId,
                user.version(),
                user.createdAt(),
                user.lastModifiedAt());
    }
}
