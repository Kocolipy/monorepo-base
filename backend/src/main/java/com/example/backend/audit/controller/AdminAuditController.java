package com.example.backend.audit.controller;

import com.example.backend.audit.application.AuditEventListingService;
import com.example.backend.audit.domain.AuditEventPage;
import com.example.backend.audit.domain.AuditEventQuery;
import com.example.backend.audit.domain.AuditOperation;
import com.example.backend.audit.domain.AuditOutcome;
import com.example.backend.audit.domain.InvalidAuditQueryException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inbound HTTP adapter for the redacted administrative audit listing.
 *
 * <p>Under {@code /api/admin}, so it is session authenticated and restricted to
 * {@code ROLE_ADMIN} by the filter chain; authorization is not expressed here, for the reason
 * {@code AdminAccountController} does not express it either.
 *
 * <p>Every filter binds to a typed value — an operation or outcome from its closed set, an id, an
 * instant — so a malformed one is refused with {@code 400} by the binder before this method runs,
 * and none can carry free text into the query.
 */
@RestController
@RequestMapping("/api/admin/audit-events")
public class AdminAuditController {

    private final AuditEventListingService listing;

    public AdminAuditController(AuditEventListingService listing) {
        this.listing = listing;
    }

    @GetMapping
    public AuditEventPage list(
            @RequestParam(required = false) AuditOperation operation,
            @RequestParam(required = false) AuditOutcome outcome,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) UUID resourceId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + AuditEventQuery.DEFAULT_SIZE) int size) {
        return listing.list(new AuditEventQuery(
                operation, outcome, actorId, resourceId, from, to, page, size));
    }

    /** A page or page size out of range: the request itself is malformed. */
    @ExceptionHandler(InvalidAuditQueryException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void invalidQuery() {
    }
}
