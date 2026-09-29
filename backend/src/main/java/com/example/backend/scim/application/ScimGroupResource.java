package com.example.backend.scim.application;

import com.example.backend.scim.domain.ScimGroup;
import com.example.backend.scim.domain.ScimGroupMember;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A SCIM Group as everything outside this slice may see it.
 *
 * <p>The projection exists for a narrower reason than the User's does — a Group holds no
 * credential — but the same structural one: it has no field the reservation marker could be
 * written into. Whether a resource is protected is an internal fact that decides whether a
 * write is refused, not an attribute RFC 7643 defines, and a renderer handed the aggregate
 * would eventually put it on the wire as though it were one.
 *
 * <p>{@code externalId} is resolved for ONE connector — the one that made the request that
 * produced this projection. It is a component here rather than a separate lookup so a
 * renderer cannot omit it by forgetting, and so no code path exists that renders a resource
 * with another connector's alias in it.
 *
 * @param id             the resource's stable id
 * @param displayName    the Group's label, as stored
 * @param members        direct User members, each with its read-only label
 * @param externalId     the calling connector's alias, or null when it set none
 * @param version        the representation version, rendered as the ETag
 * @param createdAt      creation instant, UTC
 * @param lastModifiedAt last representation change, UTC
 */
public record ScimGroupResource(
        UUID id,
        String displayName,
        List<ScimGroupMember> members,
        String externalId,
        long version,
        Instant createdAt,
        Instant lastModifiedAt) implements ScimListedResource {

    public ScimGroupResource {
        members = members == null ? List.of() : List.copyOf(members);
    }

    /**
     * The projection of a stored Group for one connector.
     *
     * <p>Takes the alias as an argument rather than reading it, so the caller that knows
     * which connector is asking is the one that supplies it; a projection that looked the
     * alias up itself would need the connector's identity and could get it wrong silently.
     */
    static ScimGroupResource of(ScimGroup group, String externalId) {
        return new ScimGroupResource(
                group.id(),
                group.displayName(),
                group.members(),
                externalId,
                group.version(),
                group.createdAt(),
                group.lastModifiedAt());
    }
}
