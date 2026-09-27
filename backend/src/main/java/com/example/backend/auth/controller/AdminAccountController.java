package com.example.backend.auth.controller;

import com.example.backend.auth.application.IdentityAdministrationService;
import com.example.backend.auth.application.IdentitySummary;
import com.example.backend.auth.application.UnknownIdentityException;
import com.example.backend.auth.application.UnsafeIdentityChangeException;
import java.security.Principal;
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
 *
 * <p>{@link IdentitySummary} is returned as the wire shape rather than copied into
 * a response type of this adapter's own. The copy would have been field-identical
 * and would have had no property to enforce: the guarantee that no password hash
 * can reach a client belongs to {@code IdentitySummary}, which has no field one
 * could be written into, and a second record restating its fields only adds a
 * place for the two to drift.
 */
@RestController
@RequestMapping("/api/admin/accounts")
public class AdminAccountController {

    private final IdentityAdministrationService identities;

    public AdminAccountController(IdentityAdministrationService identities) {
        this.identities = identities;
    }

    @GetMapping
    public List<IdentitySummary> listAccounts() {
        return identities.listIdentities();
    }

    /**
     * Closes an account to new logins and ends the sessions it already holds, so
     * the next request it makes arrives as a stranger. What exactly that costs the
     * holder is {@code IdentityAdministrationService}'s to define.
     */
    @PostMapping("/{username}/disable")
    public IdentitySummary disable(@PathVariable String username, Principal principal) {
        return identities.deactivate(username, principal.getName());
    }

    /** Reopens an account to logins, leaving any lockout it is serving standing. */
    @PostMapping("/{username}/enable")
    public IdentitySummary enable(@PathVariable String username, Principal principal) {
        return identities.activate(username, principal.getName());
    }

    /** Ends a lockout early. Says nothing about whether the account is enabled. */
    @PostMapping("/{username}/unlock")
    public IdentitySummary unlock(@PathVariable String username, Principal principal) {
        return identities.unlock(username, principal.getName());
    }

    @ExceptionHandler(UnknownIdentityException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void unknownIdentity() {
        // The caller is already an administrator, so naming what is missing
        // reveals nothing they could not read from the listing.
    }

    /**
     * A refusal about the action rather than the caller, so neither 403 (the role
     * is fine) nor 400 (the request is well formed) fits.
     */
    @ExceptionHandler(UnsafeIdentityChangeException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public void unsafeChange() {
    }
}
