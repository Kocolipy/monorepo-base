package com.example.backend.auth.controller;

import com.example.backend.auth.application.RoleListingService;
import com.example.backend.auth.application.RoleSummary;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inbound HTTP adapter for the read-only view of the role mapping: each Role, its Permissions and
 * the Group that confers it.
 *
 * <p>One read and nothing else. Roles and the mapping are deployment configuration, fixed while the
 * application runs, so this adapter declares no write handler: a {@code POST}, {@code PUT},
 * {@code PATCH} or {@code DELETE} here is refused, and no endpoint anywhere creates, changes or
 * deletes a Role or a mapping entry. Assigning a Role is Group membership, which a connector writes
 * over SCIM.
 *
 * <p>Requires {@code group:read}, the Permission that already reads who is in which Group: the
 * mapping is the other half of the same question, who holds which Role.
 */
@RestController
@RequestMapping("/api/admin/roles")
public class AdminRoleController {

    private final RoleListingService roles;

    public AdminRoleController(RoleListingService roles) {
        this.roles = roles;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('group:read')")
    public List<RoleSummary> listRoles() {
        return roles.listRoles();
    }
}
