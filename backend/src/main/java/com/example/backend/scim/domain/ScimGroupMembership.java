package com.example.backend.scim.domain;

import java.util.UUID;

/**
 * One direct membership: this User belongs to this Group. Ids only, for a caller deciding which
 * memberships to move rather than rendering either side.
 *
 * @param userId  the member
 * @param groupId the Group it belongs to
 */
public record ScimGroupMembership(UUID userId, UUID groupId) {
}
