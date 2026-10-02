package com.example.backend.audit.application;

import com.example.backend.observability.SkippableJobRun;

/**
 * What one retention run did: skipped because another run held the job's lock, or ran and
 * removed this many events.
 *
 * @param skipped whether another run of the job held its lock, so this one did nothing
 * @param deleted how many events this run removed; zero when it skipped or nothing had aged out
 */
public record AuditRetentionRun(boolean skipped, long deleted) implements SkippableJobRun {

    /** A run that found the job's lock held and did nothing. */
    static AuditRetentionRun skippedRun() {
        return new AuditRetentionRun(true, 0);
    }
}
