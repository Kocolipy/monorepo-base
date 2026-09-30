package com.example.backend.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.TimeGauge;
import java.time.Clock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * The metrics every scheduled job publishes, so "the job failed" and "the job never ran"
 * are both alertable rather than only the first.
 *
 * <ul>
 *   <li>{@code app.job.runs{job, outcome}} — a counter of completed runs, {@code outcome}
 *       {@code success} or {@code failure}. Both series are registered when the job is
 *       scheduled, at zero, so a job that has never run is a series reading 0 rather than
 *       an absent one an alert expression cannot see.
 *   <li>{@code app.job.last.success{job}} — when the job last succeeded, as epoch seconds.
 *       Until the first success it reads the moment the job was scheduled, so
 *       {@code time() - app_job_last_success_seconds} is "how long this job has gone
 *       without succeeding" from startup onwards — which is the whole of the never-ran
 *       alert, and it fires for a job that throws on every run as well as for one whose
 *       trigger never fires.
 * </ul>
 *
 * <p>{@code job} is the name the caller schedules it under, a constant from code. The
 * alert rules in {@code ops/prometheus/alerts.yaml} select on it, so renaming a job is a
 * change to those rules too.
 */
@Component
public class ScheduledJobMetrics {

    static final String RUNS = "app.job.runs";

    static final String LAST_SUCCESS = "app.job.last.success";

    private final MeterRegistry registry;

    private final Clock clock;

    public ScheduledJobMetrics(MeterRegistry registry, Clock clock) {
        this.registry = registry;
        this.clock = clock;
    }

    /**
     * Wraps a job so each run is counted. A failure is counted and then rethrown, so the
     * scheduler's own error handling — logging it, keeping the schedule — is unchanged.
     */
    public Runnable instrument(String job, Runnable task) {
        Counter succeeded = runs(job, "success");
        Counter failed = runs(job, "failure");
        AtomicLong lastSuccessMillis = new AtomicLong(clock.millis());
        TimeGauge.builder(LAST_SUCCESS, lastSuccessMillis, TimeUnit.MILLISECONDS,
                        AtomicLong::doubleValue)
                .description("When the job last succeeded; the scheduling time until it has")
                .tag("job", job)
                .register(registry);
        return () -> {
            try {
                task.run();
            } catch (RuntimeException failure) {
                failed.increment();
                throw failure;
            }
            lastSuccessMillis.set(clock.millis());
            succeeded.increment();
        };
    }

    private Counter runs(String job, String outcome) {
        return Counter.builder(RUNS)
                .description("Completed runs of a scheduled job, by outcome")
                .tag("job", job)
                .tag("outcome", outcome)
                .register(registry);
    }
}
