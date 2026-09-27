package com.example.backend.scim.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * The identity of one membership: the Group and the User it joins.
 *
 * <p>The pair IS the key, so a User cannot be in the same Group twice and there is no
 * surrogate id a membership could be addressed by. That matches what a membership is — a
 * fact relating two resources, with no attributes of its own, since RFC 7643 makes every
 * {@code members} sub-attribute other than {@code value} read-only and derived from the
 * referenced resource.
 *
 * <p>A class rather than a record because JPA requires a composite key to be mutable with
 * an accessible no-argument constructor, which a record cannot provide.
 */
@Embeddable
public class ScimGroupMemberId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "group_id", nullable = false, updatable = false)
    private UUID groupId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    protected ScimGroupMemberId() {
    }

    public ScimGroupMemberId(UUID groupId, UUID userId) {
        this.groupId = groupId;
        this.userId = userId;
    }

    /*
     * No accessors. Every read of a membership is a JPQL projection naming `m.id.userId`
     * or `m.id.groupId` directly, so neither field is ever read through this class in
     * Java. equals and hashCode below are not dead in the same sense: JPA requires a
     * composite key to implement both, and Hibernate calls them when it keys the
     * persistence context, which is why they carry a test of their own rather than a
     * justification.
     */

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ScimGroupMemberId that)) {
            return false;
        }
        return Objects.equals(groupId, that.groupId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, userId);
    }
}
