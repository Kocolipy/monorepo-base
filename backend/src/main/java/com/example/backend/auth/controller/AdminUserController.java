package com.example.backend.auth.controller;

import com.example.backend.auth.application.AccountService;
import com.example.backend.auth.application.AccountSummary;
import com.example.backend.auth.domain.AccountRole;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inbound HTTP adapter for administrative account review.
 *
 * <p>Authorization is not expressed here. {@code /api/admin/**} is restricted to
 * {@code ROLE_ADMIN} by the filter chain, so a non-admin never reaches this
 * class — which keeps every access rule in one readable place instead of half
 * here and half there. The chain is what
 * {@code SecurityConfigTests} asserts on.
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AccountService accounts;

    public AdminUserController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    public List<AdminUserResponse> listUsers() {
        return accounts.listAccounts().stream().map(AdminUserResponse::of).toList();
    }

    /**
     * The wire shape. It is built from an {@link AccountSummary}, which has no
     * password hash to copy, so this response cannot carry one.
     */
    public record AdminUserResponse(
            String username,
            String email,
            AccountRole role,
            boolean enabled,
            Instant createdAt) {

        static AdminUserResponse of(AccountSummary summary) {
            return new AdminUserResponse(
                    summary.username(),
                    summary.email(),
                    summary.role(),
                    summary.enabled(),
                    summary.createdAt());
        }
    }
}
