package com.example.backend.auth.application;

import com.example.backend.authorization.domain.Permission;
import com.example.backend.authorization.domain.Role;
import com.example.backend.authorization.domain.RoleMapping;
import com.example.backend.scim.domain.ScimGroup;
import com.example.backend.scim.domain.ScimGroupRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The role mapping as an operator reads it: each Role, its Permissions, and the Group that confers
 * it, named by the directory.
 *
 * <p>A read and nothing else. The Roles and the mapping come from the one {@link RoleMapping} bean
 * built from configuration at startup; only the Groups' display names are read, from the
 * directory, because the mapping holds ids and an id alone tells an operator nothing.
 */
@Service
public class RoleListingService {

    private final RoleMapping mapping;
    private final ScimGroupRepository groups;

    public RoleListingService(RoleMapping mapping, ScimGroupRepository groups) {
        this.mapping = mapping;
        this.groups = groups;
    }

    /**
     * Every defined Role, sorted by name, with its Permissions sorted by name and the Groups that
     * confer it in mapping order.
     */
    @Transactional(readOnly = true)
    public List<RoleSummary> listRoles() {
        Map<UUID, Role> assignments = mapping.assignments();
        Map<UUID, String> displayNames = groups.findAllById(List.copyOf(assignments.keySet()))
                .stream()
                .collect(Collectors.toMap(ScimGroup::id, ScimGroup::displayName));
        Map<String, List<RoleSummary.MappedGroup>> groupsByRole = mapping.roles().stream()
                .collect(Collectors.toMap(Role::name, role -> new ArrayList<>()));
        assignments.forEach((groupId, role) -> groupsByRole.get(role.name()).add(
                new RoleSummary.MappedGroup(
                        groupId,
                        displayNames.get(groupId),
                        groupId.equals(mapping.superuserGroupId()))));
        return mapping.roles().stream()
                .sorted(Comparator.comparing(Role::name))
                .map(role -> new RoleSummary(
                        role.name(),
                        role.permissions().stream()
                                .sorted(Permission.BY_VALUE)
                                .map(Permission::value)
                                .toList(),
                        groupsByRole.get(role.name())))
                .toList();
    }
}
