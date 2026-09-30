package com.example.backend.auth.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * What a signed-in User may read about itself — and the whole of it.
 *
 * <p>A projection for the reason {@link IdentitySummary} is one: the web adapter serialises this
 * record as-is, so a component that is absent here cannot reach the wire, whatever the handler
 * does. That is how the lockout state and the failure run are kept out of the self-read
 * <em>structurally</em>: there is no component either could be written into. Telling a caller how
 * close it is to a lock helps someone guessing its password more than it helps the owner, and the
 * owner's remedy for a lock is an Admin, not the count. {@code ArchitectureTest} holds this shape,
 * so a later edit adding such a component fails the build rather than a review.
 *
 * <p>The openapi {@code SelfRecord} schema is this record's documented counterpart; a component
 * added here is published.
 *
 * @param id                     the caller's stable SCIM resource id
 * @param userName               the caller's login attribute, as stored now — read from the
 *                               directory, not from the session, so a rename made since login is
 *                               reported
 * @param displayName            the caller's display name, or {@code null} when none is set
 * @param groups                 the Groups the caller is a DIRECT member of, never null
 * @param passwordChangeRequired whether the caller's change-required flag is set
 * @param lastAuthenticatedAt    when the caller last authenticated or was reactivated, or
 *                               {@code null} when neither has happened
 */
public record SelfRecord(
        UUID id,
        String userName,
        String displayName,
        List<Group> groups,
        boolean passwordChangeRequired,
        Instant lastAuthenticatedAt) {

    public SelfRecord {
        groups = List.copyOf(groups);
    }

    /**
     * A Group the caller belongs to, by id and label — nothing that would carry the Group's other
     * members into one User's read.
     *
     * @param id          the Group's stable SCIM resource id
     * @param displayName the Group's label
     */
    public record Group(UUID id, String displayName) {
    }
}
