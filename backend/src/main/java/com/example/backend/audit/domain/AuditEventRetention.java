package com.example.backend.audit.domain;

import java.time.Instant;

/**
 * The one capability that may remove a recorded event: deleting what has aged out
 * of the retention window.
 *
 * <p>A separate port from {@link AuditEventRepository} rather than another method
 * on it, because the two are performed with different database privileges. The
 * application's own role holds insert and read on the audit table; the delete
 * belongs to a role reserved for this, and the adapter behind this port is the
 * only place in the service that assumes it.
 */
public interface AuditEventRetention {

    /**
     * Deletes every event recorded strictly before {@code cutoff}.
     *
     * @return how many rows were removed
     */
    long deleteOccurredBefore(Instant cutoff);
}
