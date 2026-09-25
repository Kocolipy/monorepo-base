package com.example.backend.audit.application;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.example.backend.audit.CapturedLog;
import com.example.backend.audit.domain.AuditEventRetention;
import com.example.backend.audit.domain.AuditRetentionPolicy;
import com.example.backend.auth.MutableClock;
import com.example.backend.observability.LogEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * What one retention run reports.
 *
 * <p>A retention job is the one thing in this service whose correct behaviour and
 * total failure look identical from outside: rows quietly disappearing and rows
 * quietly not disappearing both produce no response and no error. The record each
 * run writes is the only evidence either way, so it is asserted as a field-by-field
 * claim rather than as "something was logged".
 */
class AuditRetentionServiceTests {

    private static final Instant NOW = Instant.parse("2026-09-25T03:30:00Z");

    private final MutableClock clock = new MutableClock(NOW);

    private CapturedLog log;

    @BeforeEach
    void attachLog() {
        log = CapturedLog.attach();
    }

    @AfterEach
    void detachLog() {
        log.close();
    }

    @Test
    void theCutoffIsTheConfiguredWindowBeforeNow() {
        CountingRetention retention = new CountingRetention(3, Duration.ZERO, clock);
        AuditRetentionService service = new AuditRetentionService(
                retention, new AuditRetentionPolicy(Duration.ofDays(365), null), clock);

        assertThat(service.deleteAgedOutEvents()).isEqualTo(3);
        assertThat(retention.cutoff).isEqualTo(NOW.minus(Duration.ofDays(365)));
    }

    @Test
    void eachRunLogsItsDeletedRowCountAndDuration() {
        CountingRetention retention =
                new CountingRetention(7, Duration.ofMillis(250), clock);
        AuditRetentionService service = new AuditRetentionService(
                retention, new AuditRetentionPolicy(Duration.ofDays(120), null), clock);

        service.deleteAgedOutEvents();

        Map<String, Object> fields = CapturedLog.fields(onlyRunRecord());
        assertThat(fields).containsEntry(LogEvent.RETENTION_DELETED_ROWS, 7L);
        assertThat(fields).containsEntry(LogEvent.RETENTION_DURATION_MS, 250L);
        assertThat(fields).containsEntry(LogEvent.RETENTION_PERIOD, "PT2880H");
        assertThat(fields).containsEntry(LogEvent.OUTCOME, LogEvent.SUCCESS);
    }

    /**
     * A run that removed nothing still reports. "Nothing had aged out" and "the job
     * has not run for a month" are different facts, and only the record tells them
     * apart.
     */
    @Test
    void aRunThatRemovedNothingStillReports() {
        AuditRetentionService service = new AuditRetentionService(
                new CountingRetention(0, Duration.ZERO, clock),
                new AuditRetentionPolicy(null, null),
                clock);

        service.deleteAgedOutEvents();

        assertThat(CapturedLog.fields(onlyRunRecord()))
                .containsEntry(LogEvent.RETENTION_DELETED_ROWS, 0L);
    }

    private ILoggingEvent onlyRunRecord() {
        List<ILoggingEvent> records = log.withAction(
                Level.INFO, LogEvent.ACTION, AuditRetentionService.RETENTION_ACTION);
        assertThat(records).hasSize(1);
        return records.get(0);
    }

    /**
     * A retention port that reports a fixed row count and costs a fixed amount of
     * time, by moving the clock the service reads. That is what lets the logged
     * duration be asserted as a value rather than as "greater than zero", which
     * would pass for a job that did nothing at all.
     */
    private static final class CountingRetention implements AuditEventRetention {

        private final long deleted;
        private final Duration cost;
        private final MutableClock clock;

        private Instant cutoff;

        CountingRetention(long deleted, Duration cost, MutableClock clock) {
            this.deleted = deleted;
            this.cost = cost;
            this.clock = clock;
        }

        @Override
        public long deleteOccurredBefore(Instant cutoff) {
            this.cutoff = cutoff;
            clock.advanceBy(cost);
            return deleted;
        }
    }
}
