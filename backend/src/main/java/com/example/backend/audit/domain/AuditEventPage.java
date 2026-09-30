package com.example.backend.audit.domain;

import java.util.List;

/**
 * One page of the audit listing, newest first, and where it sits in the whole.
 *
 * <p>The events are the recorded {@link AuditEvent}s themselves, not a copy with fields removed.
 * That is what makes the listing unable to leak: an event has no field a password, a bearer value,
 * a hash, a username or a filter literal could be written into, so there is nothing for a read
 * projection to redact, and a second record restating the same fields would only be a place for
 * the two to drift.
 *
 * @param events        this page's events, newest first
 * @param page          the zero-based page number that was asked for
 * @param size          the page size that was asked for
 * @param totalElements how many events match the filters across every page
 * @param totalPages    how many pages of {@code size} those events fill
 */
public record AuditEventPage(
        List<AuditEvent> events, int page, int size, long totalElements, long totalPages) {

    public AuditEventPage {
        events = List.copyOf(events);
    }

    /** The page {@code query} asked for, holding {@code events} of {@code totalElements}. */
    public static AuditEventPage of(
            AuditEventQuery query, List<AuditEvent> events, long totalElements) {
        long totalPages = (totalElements + query.size() - 1) / query.size();
        return new AuditEventPage(events, query.page(), query.size(), totalElements, totalPages);
    }
}
