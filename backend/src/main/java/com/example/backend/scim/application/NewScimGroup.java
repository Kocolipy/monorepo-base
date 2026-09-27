package com.example.backend.scim.application;

import java.util.List;
import java.util.UUID;

/**
 * A request to create a SCIM Group, as the use case receives it.
 *
 * <p>{@code memberIds} are resolved ids rather than the submitted {@code members} array: the
 * adapter has already discarded every read-only sub-attribute a connector may have sent, so
 * nothing below it can be misled by a {@code display} label that disagrees with the User it
 * names.
 *
 * <p>{@code externalId} is the calling connector's alias for the resource, or null when it
 * sent none. It is carried as part of the command rather than written by a separate call, so
 * the resource and its alias are created in one transaction.
 *
 * @param displayName the Group's label
 * @param memberIds   the Users to put in it, possibly empty — an empty Group is legitimate
 * @param externalId  the connector's alias for the new resource, or null
 */
public record NewScimGroup(String displayName, List<UUID> memberIds, String externalId) {

    public NewScimGroup {
        memberIds = memberIds == null ? List.of() : List.copyOf(memberIds);
    }
}
