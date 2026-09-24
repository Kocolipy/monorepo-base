package com.example.backend.auth.controller;

import com.example.backend.auth.application.AccountAdministrationService;
import com.example.backend.auth.application.AccountSummary;
import com.example.backend.auth.application.UnknownAccountException;
import com.example.backend.auth.application.UnsafeAccountChangeException;
import com.example.backend.auth.domain.AccountRole;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inbound HTTP adapter for administrative account review and control.
 *
 * <p>Authorization is not expressed here. {@code /api/admin/**} is restricted to
 * {@code ROLE_ADMIN} by the filter chain, so a non-admin never reaches this
 * class — which keeps every access rule in one readable place instead of half
 * here and half there. The chain is what {@code SecurityConfigTests} asserts on.
 *
 * <p>Enabling and unlocking are separate endpoints because they are separate
 * capabilities: one governs whether an account is permitted at all, the other
 * whether it is being penalised for failed logins right now. Collapsing them
 * would make an administrator restoring access silently forgive a failure run
 * they never looked at.
 */
@RestController
@RequestMapping("/api/admin/accounts")
public class AdminAccountController {

    private final AccountAdministrationService accounts;

    public AdminAccountController(AccountAdministrationService accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    public List<AdminAccountResponse> listAccounts() {
        return accounts.listAccounts().stream().map(AdminAccountResponse::of).toList();
    }

    /**
     * Closes an account to new logins and ends the sessions it already holds, so
     * the next request it makes arrives as a stranger. What exactly that costs the
     * holder is {@code AccountAdministrationService}'s to define.
     */
    @PostMapping("/{username}/disable")
    public AdminAccountResponse disable(@PathVariable String username, Principal principal) {
        return AdminAccountResponse.of(accounts.disable(username, principal.getName()));
    }

    /** Reopens an account to logins, leaving any lockout it is serving standing. */
    @PostMapping("/{username}/enable")
    public AdminAccountResponse enable(@PathVariable String username) {
        return AdminAccountResponse.of(accounts.enable(username));
    }

    /** Ends a lockout early. Says nothing about whether the account is enabled. */
    @PostMapping("/{username}/unlock")
    public AdminAccountResponse unlock(@PathVariable String username) {
        return AdminAccountResponse.of(accounts.unlock(username));
    }

    @ExceptionHandler(UnknownAccountException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void unknownAccount() {
        // The caller is already an administrator, so naming what is missing
        // reveals nothing they could not read from the listing.
    }

    /**
     * A refusal about the action rather than the caller, so neither 403 (the role
     * is fine) nor 400 (the request is well formed) fits.
     */
    @ExceptionHandler(UnsafeAccountChangeException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public void unsafeChange() {
    }

    /**
     * The wire shape. It is built from an {@link AccountSummary}, which has no
     * password hash to copy, so this response cannot carry one.
     */
    public record AdminAccountResponse(
            String username,
            AccountRole role,
            boolean enabled,
            boolean locked,
            Instant lockedUntil,
            Instant createdAt) {

        static AdminAccountResponse of(AccountSummary summary) {
            return new AdminAccountResponse(
                    summary.username(),
                    summary.role(),
                    summary.enabled(),
                    summary.locked(),
                    summary.lockedUntil(),
                    summary.createdAt());
        }
    }
}
