package com.example.backend.observability;

import com.example.backend.observability.LogEvent.ErrorCategory;
import com.example.backend.observability.LogEvent.Operation;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;

/**
 * The one wrapper every scheduled job runs through: what each run publishes as metrics and
 * as log records, so that no job re-implements either.
 *
 * <h2>Metrics</h2>
 *
 * <p>So "the job failed" and "the job never ran" are both alertable rather than only the
 * first:
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
 *
 * <h2>Log records</h2>
 *
 * <p>Each run puts its identity in the logging context ({@link LogContext#job}) —
 * {@code batch.job.name} (the same {@code job} name), a fresh {@code batch.job.run.id}, and
 * {@code trigger.type} {@code scheduled} — so every record the run emits, the job's own
 * included, carries it. Then:
 *
 * <ul>
 *   <li>a {@code job-start} record as the run begins;
 *   <li>a {@code job-end} record as it ends: {@code event.outcome} {@code success} and
 *       {@code event.duration_ms}, plus whatever the run {@linkplain SkippableJobRun#counts
 *       counted}; with {@code event.reason} {@code lock-held} in place of
 *       the work when a lock-serialized job found another run holding its lock; or, at
 *       {@code ERROR}, {@code failure} with the error fields and the exception attached;
 *   <li>the context keys removed again when the run ends, however it ends.
 * </ul>
 *
 * <p>Each record is classified as the job's own {@link Operation}, so a search on a job's
 * action finds its start, its end and what it did between them.
 */
@Component
public class ScheduledJobMetrics {

    static final String RUNS = "app.job.runs";

    static final String LAST_SUCCESS = "app.job.last.success";

    /**
     * The observation around one run. Besides the trace, Boot's meter handler records it
     * as a timer, {@code app_job_run_seconds{job,error}}: how long runs take, which the
     * run counter alone cannot say.
     */
    static final String RUN = "app.job.run";

    private static final Logger log = LoggerFactory.getLogger(ScheduledJobMetrics.class);

    private final MeterRegistry registry;

    private final ObservationRegistry observations;

    private final Clock clock;

    public ScheduledJobMetrics(MeterRegistry registry, ObservationRegistry observations, Clock clock) {
        this.registry = registry;
        this.observations = observations;
        this.clock = clock;
    }

    /**
     * Wraps a job so each run is counted, is its own trace, and is logged from start to end.
     * A failure is counted, logged and then rethrown, so the scheduler's own error handling
     * — keeping the schedule — is unchanged. A run that reports it
     * {@linkplain SkippableJobRun#skipped skipped} is logged as such, and counts as a
     * success: nothing failed, another run is doing the work.
     *
     * <p>A run happens off any request, so without an observation of its own it would
     * have no trace, and its records no {@code trace.id} to correlate them by. Each run
     * opens one ({@value #RUN}, tagged with {@code job}), and every record the job emits
     * while it runs carries that run's trace id; the next run gets a different one. The
     * run's context keys are set inside it, so {@code job-start} and {@code job-end} carry
     * the trace too.
     */
    public Runnable instrumentLocked(
            String job, Operation operation, Supplier<? extends SkippableJobRun> task) {
        Counter succeeded = runs(job, "success");
        Counter failed = runs(job, "failure");
        AtomicLong lastSuccessMillis = new AtomicLong(clock.millis());
        TimeGauge.builder(LAST_SUCCESS, lastSuccessMillis, TimeUnit.MILLISECONDS,
                        AtomicLong::doubleValue)
                .description("When the job last succeeded; the scheduling time until it has")
                .tag("job", job)
                .register(registry);
        return () -> Observation.createNotStarted(RUN, observations)
                .lowCardinalityKeyValue("job", job)
                .observe(() -> {
                    try (LogContext.Scope run = LogContext.job(job, UUID.randomUUID())) {
                        long startedAt = clock.millis();
                        LogEvent.jobStart(log, operation).log();
                        SkippableJobRun result;
                        try {
                            result = task.get();
                        } catch (RuntimeException failure) {
                            failed.increment();
                            logFailure(operation, failure, clock.millis() - startedAt);
                            throw failure;
                        }
                        long endedAt = clock.millis();
                        lastSuccessMillis.set(endedAt);
                        succeeded.increment();
                        logEnd(operation, result, endedAt - startedAt);
                    }
                });
    }

    private static void logEnd(Operation operation, SkippableJobRun result, long durationMillis) {
        LoggingEventBuilder end =
                LogEvent.jobEnd(log, operation, durationMillis, result.skipped());
        if (!result.skipped()) {
            result.counts().forEach(end::addKeyValue);
        }
        end.log();
    }

    /**
     * The failed run's one application record, at {@code ERROR}, with the exception attached
     * whole. Not a redacted copy, as the SCIM handler attaches: the same exception goes on
     * to the scheduler's own error handler, which logs it whole regardless, so a redacted
     * copy here would hide nothing and would cost {@code error.type} its real class name.
     */
    private static void logFailure(Operation operation, RuntimeException failure, long durationMillis) {
        LogEvent.jobFailed(log, operation, durationMillis, category(failure))
                .setCause(failure)
                .log();
    }

    /**
     * {@code database} for a failure Spring's data access or transaction layer raised —
     * every job here is a database job, so those are the failures expected — and
     * {@code application} for anything else.
     */
    static ErrorCategory category(RuntimeException failure) {
        return failure instanceof DataAccessException || failure instanceof TransactionException
                ? ErrorCategory.DATABASE
                : ErrorCategory.APPLICATION;
    }

    private Counter runs(String job, String outcome) {
        return Counter.builder(RUNS)
                .description("Completed runs of a scheduled job, by outcome")
                .tag("job", job)
                .tag("outcome", outcome)
                .register(registry);
    }
}
