package com.example.backend.auth.application;

import java.util.List;
import java.util.UUID;

/**
 * One Role as an operator holding {@code group:read} is shown it: what it grants and which Group
 * confers it. The read-only view of the role mapping, so the mapping can be inspected without
 * reading deployment configuration.
 *
 * <p>Read-only in its entirety. The mapping is deployment configuration, fixed for the life of the
 * process, and no endpoint creates, changes or deletes a Role or a mapping entry.
 *
 * @param name        the Role's name, unique within the mapping
 * @param permissions the Permissions it holds, sorted by name
 * @param groups      the Groups that confer it, in mapping order; empty for a Role no Group confers
 */
public record RoleSummary(String name, List<String> permissions, List<MappedGroup> groups) {

    public RoleSummary {
        permissions = List.copyOf(permissions);
        groups = List.copyOf(groups);
    }

    /**
     * A Group the mapping assigns the Role to.
     *
     * @param id          the Group's stable id, as the mapping names it
     * @param displayName the directory's current name for it, or {@code null} when no live Group
     *                    has that id — an ordinary mapped Group stays deletable over SCIM, and a
     *                    deleted one confers nothing until the deployment's mapping is replaced
     * @param superuser   whether this is the Superuser Group, whose Role holds every Permission
     */
    public record MappedGroup(UUID id, String displayName, boolean superuser) {
    }
}
