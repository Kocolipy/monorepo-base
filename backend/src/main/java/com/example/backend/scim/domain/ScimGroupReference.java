package com.example.backend.scim.domain;

import java.util.UUID;

/**
 * A Group as it is referred to from somewhere else: its id and its label.
 *
 * <p>The shape of the read-only reverse view on User. A User's {@code groups} attribute
 * renders the id, the label and a {@code $ref} built from the id, so this carries exactly
 * those two facts — returning whole {@link ScimGroup} values instead would drag every
 * other member of every Group into one User's read, and would give a renderer access to
 * membership it has no business rendering there.
 *
 * @param id          the Group's stable id
 * @param displayName the Group's label, as stored
 */
public record ScimGroupReference(UUID id, String displayName) {

    public ScimGroupReference {
        if (id == null) {
            throw new IllegalArgumentException("a Group reference names a Group");
        }
    }
}
