package com.example.backend.authorization.domain;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * A named set of Permissions, as a deployment defines it. A User holds a Role by being a direct
 * member of the Group the role mapping assigns it to; the Role itself knows nothing of Groups.
 *
 * <p>The Permissions are held in an {@link EnumSet}, so they iterate in one fixed order on every
 * JVM — declaration order — rather than in the per-process order an immutable {@code Set.copyOf}
 * would give.
 *
 * @param name        the Role's name, unique within the mapping
 * @param permissions what holding the Role grants; may be empty
 */
public record Role(String name, Set<Permission> permissions) {

    public Role {
        Set<Permission> copy = EnumSet.noneOf(Permission.class);
        copy.addAll(permissions);
        permissions = Collections.unmodifiableSet(copy);
    }
}
