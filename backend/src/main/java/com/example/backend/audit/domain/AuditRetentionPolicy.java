package com.example.backend.audit.domain;

import java.time.Duration;

/**
 * How long recorded audit events are kept, and when the job that enforces it runs.
 *
 * <p>The floor is the reason this is a value with a constructor rather than a
 * number read where it is used. Ninety days is the shortest window in which an
 * investigation into account activity is still possible at all — a compromise
 * noticed at the end of a quarter has to have something left to read — so a
 * deployment configuring less than that is a misconfiguration, not a preference,
 * and it stops the application from starting rather than silently shortening the
 * trail. Refusing at construction is what makes that a startup failure: the bean
 * cannot be created, so the context cannot come up.
 *
 * <p>Held as a value rather than as configuration so the rule can be exercised
 * without a running application, exactly as {@code LockoutPolicy} is.
 *
 * @param period   how long an event is kept after it was recorded
 * @param schedule cron expression for the retention job
 */
public record AuditRetentionPolicy(Duration period, String schedule) {

    /** One year, applied when a deployment configures nothing. */
    public static final Duration DEFAULT_PERIOD = Duration.ofDays(365);

    /**
     * Ninety days. Not a default and not a suggestion — a configured value below
     * this is refused.
     */
    public static final Duration MINIMUM_PERIOD = Duration.ofDays(90);

    /** Daily, at 03:30, away from the hours a deployment is busiest. */
    public static final String DEFAULT_SCHEDULE = "0 30 3 * * *";

    public AuditRetentionPolicy {
        period = period == null ? DEFAULT_PERIOD : period;
        schedule = schedule == null || schedule.isBlank() ? DEFAULT_SCHEDULE : schedule;
        if (period.compareTo(MINIMUM_PERIOD) < 0) {
            throw new IllegalArgumentException(
                    "Audit retention is configured at " + period
                            + ", below the " + MINIMUM_PERIOD + " floor. An audit trail shorter"
                            + " than that cannot support an investigation, so the application"
                            + " refuses to start rather than keep less.");
        }
    }
}
