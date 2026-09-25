package com.example.backend.audit.application;

import com.example.backend.audit.domain.AuditEventRetention;
import com.example.backend.audit.domain.AuditRetentionPolicy;
import com.example.backend.observability.LogEvent;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enforces the retention window: one pass that removes every event older than it.
 *
 * <p>Each run reports what it did — how many rows it removed and how long it took
 * — because a retention job is otherwise the one thing in the service whose
 * correct behaviour and total failure look identical from outside. A run that
 * deletes nothing is logged too: "nothing had aged out" and "the job has not run
 * for a month" are different facts and an operator needs to be able to tell them
 * apart.
 *
 * <p>{@code @Transactional} is load-bearing rather than incidental. The adapter
 * assumes the retention database role with {@code SET LOCAL ROLE}, which needs a
 * transaction to be local to; without one the delete would run as the
 * application's own role and be refused.
 */
@Service
public class AuditRetentionService {

    /** {@code event.action} on every record this job emits, its schedule included. */
    public static final String RETENTION_ACTION = "audit.retention";

    private static final Logger log = LoggerFactory.getLogger(AuditRetentionService.class);

    private final AuditEventRetention retention;
    private final AuditRetentionPolicy policy;
    private final Clock clock;

    public AuditRetentionService(
            AuditEventRetention retention, AuditRetentionPolicy policy, Clock clock) {
        this.retention = retention;
        this.policy = policy;
        this.clock = clock;
    }

    /**
     * Removes every event recorded longer ago than the retention window.
     *
     * @return how many rows were removed, for a caller that drives this directly
     */
    @Transactional
    public long deleteAgedOutEvents() {
        Instant startedAt = clock.instant();
        Instant cutoff = startedAt.minus(policy.period());
        long deleted = retention.deleteOccurredBefore(cutoff);
        Duration took = Duration.between(startedAt, clock.instant());
        log.atInfo()
                .addKeyValue(LogEvent.ACTION, RETENTION_ACTION)
                .addKeyValue(LogEvent.OUTCOME, LogEvent.SUCCESS)
                .addKeyValue(LogEvent.RETENTION_PERIOD, policy.period().toString())
                .addKeyValue(LogEvent.RETENTION_DELETED_ROWS, deleted)
                .addKeyValue(LogEvent.RETENTION_DURATION_MS, took.toMillis())
                .log("Audit retention run complete");
        return deleted;
    }
}
