package com.example.backend.audit.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * What an administrator asked the audit listing for: which events, and which page of them.
 *
 * <p>Every filter is optional and they combine with AND; a {@code null} means "do not narrow by
 * this". Each one is typed — an operation and an outcome from their closed sets, an actor and a
 * resource by stable id, a time window as instants — so a filter can only ever be compared with a
 * stored column, never become part of the statement that compares it.
 *
 * <p>The listing is newest first. A page is addressed by number rather than by cursor because an
 * Admin reading the trail jumps to a page; appends only ever land on page zero, so a later page
 * shifts by exactly the number of events recorded since it was read, never reorders.
 *
 * @param operation  only events of this operation, or {@code null}
 * @param outcome    only events with this outcome, or {@code null}
 * @param actorId    only events whose actor is this stable id, or {@code null}
 * @param resourceId only events about this resource's stable id, or {@code null}
 * @param from       only events that occurred at or after this instant, or {@code null}
 * @param to         only events that occurred strictly before this instant, or {@code null}
 * @param page       the zero-based page number
 * @param size       how many events a page holds, from 1 to {@link #MAX_SIZE}
 */
public record AuditEventQuery(
        AuditOperation operation,
        AuditOutcome outcome,
        UUID actorId,
        UUID resourceId,
        Instant from,
        Instant to,
        int page,
        int size) {

    /** The page size when a caller asks for none. */
    public static final int DEFAULT_SIZE = 50;

    /**
     * The largest page a caller may ask for. A bound rather than "as many as you like" because an
     * unbounded read of a year's trail is one request away from exhausting the heap.
     */
    public static final int MAX_SIZE = 200;

    public AuditEventQuery {
        if (page < 0) {
            throw new InvalidAuditQueryException("page must not be negative");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidAuditQueryException("size must be between 1 and " + MAX_SIZE);
        }
    }

    /**
     * How many events precede this page. A {@code long} because {@code page * size} can exceed an
     * {@code int} for a page number the caller is entitled to ask for, even though it holds nothing.
     */
    public long offset() {
        return (long) page * size;
    }
}
