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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Inbound HTTP adapter for the redacted administrative audit listing.
 *
 * <p>Under {@code /api/admin}, so it is session authenticated; it requires {@code audit:read},
 * declared here with method security and repeated by the chain as a backstop, for the reason
 * {@code AdminAccountController} gives.
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
    @PreAuthorize("hasAuthority('audit:read')")
    public AuditEventPage list(
            @RequestParam(required = false) AuditOperation operation,
            @RequestParam(required = false) AuditOutcome outcome,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) UUID resourceId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + AuditEventQuery.DEFAULT_SIZE) int size) {
        try {
            return listing.list(new AuditEventQuery(
                    operation, outcome, actorId, resourceId, from, to, page, size));
        } catch (InvalidAuditQueryException outOfRange) {
            // A page or page size out of range: the request itself is malformed. Answered by the
            // app-wide handler, so it carries the same 400 body as a parameter that cannot be
            // read at all (an operation this service does not know, an id that is not a UUID).
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, null, outOfRange);
        }
    }
}
