package com.example.backend.audit.application;

import com.example.backend.audit.domain.AuditEventPage;
import com.example.backend.audit.domain.AuditEventQuery;
import com.example.backend.audit.domain.AuditEventReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The administrative read of the audit trail.
 *
 * <p>Read-only and unaudited. Reading the trail is not one of the operations the trail records —
 * an event type for it would be a new event type, which this surface deliberately does not add —
 * and the caller is an Admin already authorized by the filter chain for {@code /api/admin/**}.
 *
 * <p>{@code REPEATABLE READ} rather than the default, because the reader issues two statements —
 * the count and the page — and under {@code READ COMMITTED} each would see its own snapshot, so an
 * event appended between them would be counted but not listed. In Postgres a repeatable-read
 * transaction takes one snapshot at its first statement, so the total an Admin is shown describes
 * exactly the events beside it.
 */
@Service
public class AuditEventListingService {

    private final AuditEventReader events;

    public AuditEventListingService(AuditEventReader events) {
        this.events = events;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public AuditEventPage list(AuditEventQuery query) {
        return events.find(query);
    }
}
