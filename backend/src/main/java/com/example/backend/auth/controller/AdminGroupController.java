package com.example.backend.auth.controller;

import com.example.backend.auth.application.GroupSummary;
import com.example.backend.auth.application.IdentityAdministrationService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inbound HTTP adapter for the Groups projection of the Accounts page.
 *
 * <p>One read and nothing else. Groups and their membership belong to the directory, written by a
 * connector over SCIM, so this adapter declares no write handler at all: a {@code POST},
 * {@code PUT}, {@code PATCH} or {@code DELETE} here is refused by the dispatcher, which is the
 * refusal the read-only view relies on rather than a check someone must remember to keep.
 *
 * <p>Requires {@code group:read}, declared on the handler and repeated by the chain as a backstop,
 * for the reason {@link AdminAccountController} gives.
 */
@RestController
@RequestMapping("/api/admin/groups")
public class AdminGroupController {

    private final IdentityAdministrationService identities;

    public AdminGroupController(IdentityAdministrationService identities) {
        this.identities = identities;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('group:read')")
    public List<GroupSummary> listGroups() {
        return identities.listGroups();
    }
}
