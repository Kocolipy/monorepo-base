package com.example.backend.scim.domain;

import java.util.UUID;

/**
 * One membership of a Group: the User it refers to, and the label rendered beside the
 * reference.
 *
 * <p>{@code display} is <strong>read-only</strong>, as RFC 7643 §4.2 makes every
 * sub-attribute of {@code members} other than {@code value}. It is a projection of the
 * referenced User's own attributes, so a submitted value is ignored rather than stored:
 * accepting one would let a connector write a label that disagrees with the User it
 * names, and there is no row it could be written to — {@code scim_group_members} holds
 * the pair of ids and nothing else.
 *
 * <p>It is a component here rather than resolved by the renderer so that a Group read
 * cannot omit it by forgetting, and so the resolution happens in the one place that can
 * do it in the same query as the membership itself.
 *
 * @param userId  the member User's stable id; the only part of a membership that is stored
 * @param display the referenced User's label, or {@code null} on a value being written
 */
public record ScimGroupMember(UUID userId, String display) {

    public ScimGroupMember {
        if (userId == null) {
            throw new IllegalArgumentException("a Group membership names a User");
        }
    }

    /**
     * A membership as a write states it: the referenced id, and no label.
     *
     * <p>Named for what the caller has rather than for what it lacks, so a write path
     * cannot accidentally invent a display value by using the canonical constructor with
     * whatever string was to hand.
     */
    public static ScimGroupMember reference(UUID userId) {
        return new ScimGroupMember(userId, null);
    }
}
