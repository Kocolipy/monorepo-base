package com.example.backend.scim.infrastructure.persistence;

import java.util.UUID;

/**
 * One membership as the batch read returns it: which Group, which User, and the label the
 * User is rendered under.
 *
 * <p>Exists so a page of Groups costs one membership query instead of one per Group. The
 * per-Group shape ({@code ScimGroupMember}) carries no Group id, because a single Group's
 * read already knows which Group it asked about; a batch read does not, so it needs the
 * third field and cannot reuse the domain type.
 *
 * <p>Package-private, and deliberately not in the domain: it is the shape of a query, and
 * the only thing it is ever used for is being grouped by {@link #groupId()} and thrown
 * away.
 *
 * @param groupId the Group the membership belongs to
 * @param userId  the member User's stable id
 * @param display the referenced User's label, read-only per RFC 7643 §4.2
 */
record ScimGroupMemberRow(UUID groupId, UUID userId, String display) {
}
