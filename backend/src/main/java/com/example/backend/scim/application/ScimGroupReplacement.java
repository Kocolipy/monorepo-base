package com.example.backend.scim.application;

import java.util.List;
import java.util.UUID;

/**
 * A request to replace a SCIM Group wholesale — a PUT.
 *
 * <p>Separate from {@link NewScimGroup} even though the two carry the same two attributes,
 * because a replacement has no {@code externalId}: the alias belongs to the connector's
 * relationship with the resource and is established when the resource is created. A PUT that
 * could change it would let one connector's replacement silently re-key the resource in its
 * own namespace, and a replacement that had to RESTATE it would delete the alias of every
 * connector that omitted the field.
 *
 * <p>An absent {@code members} in the submitted document arrives here as an empty list, not
 * as null — a PUT replaces the resource, so an attribute the document does not mention is
 * being set to nothing rather than left alone. That is the difference between PUT and PATCH,
 * and it is resolved at the adapter so nothing below it has to know which verb it came from.
 *
 * @param displayName the Group's new label
 * @param memberIds   the Users the Group should now contain, possibly empty
 */
public record ScimGroupReplacement(String displayName, List<UUID> memberIds) {

    public ScimGroupReplacement {
        memberIds = memberIds == null ? List.of() : List.copyOf(memberIds);
    }
}
