package com.example.backend.scim.application;

import java.util.List;
import java.util.UUID;

/**
 * A request to replace a SCIM Group wholesale — a PUT.
 *
 * <p>{@code externalId} is read-write and replaced like the other attributes: the submitted value
 * becomes the calling connector's alias, and an omitted one removes it, because RFC 7644 §3.5.1
 * makes an omitted read-write attribute unassigned on replace. Only the calling connector's alias
 * is touched — aliases are keyed by connector, so another connector's name for the same Group is
 * neither read nor written.
 *
 * <p>An absent {@code members} in the submitted document arrives here as an empty list, not
 * as null — a PUT replaces the resource, so an attribute the document does not mention is
 * being set to nothing rather than left alone. That is the difference between PUT and PATCH,
 * and it is resolved at the adapter so nothing below it has to know which verb it came from.
 *
 * @param displayName the Group's new label
 * @param memberIds   the Users the Group should now contain, possibly empty
 * @param externalId  the calling connector's alias after the PUT, or {@code null} to remove it
 */
public record ScimGroupReplacement(String displayName, List<UUID> memberIds, String externalId) {

    public ScimGroupReplacement {
        memberIds = memberIds == null ? List.of() : List.copyOf(memberIds);
    }
}
