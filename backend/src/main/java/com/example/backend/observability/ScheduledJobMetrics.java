package com.example.backend.observability;

import com.example.backend.observability.LogEvent.Category;
import com.example.backend.observability.LogEvent.ErrorCategory;
import com.example.backend.observability.LogEvent.Operation;
import com.example.backend.observability.LogEvent.Severity;
import com.example.backend.observability.LogEvent.Type;
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
 *       {@code event.duration_ms}; with {@code event.reason} {@code lock-held} in place of
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

    /**
     * A failed run's {@code error_code}. The schema aligns the code with HTTP statuses, and a
     * job failing is the service's own fault: there is no caller to have sent anything wrong.
     */
    static final int FAILED_RUN_ERROR_CODE = 500;

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
     * The startup record of a job's schedule, classified as the job's operation, with the
     * job's name, its cron and the zone that cron is evaluated in, and what the job does.
     * The caller adds what is particular to the job — its window, say — and logs it, from
     * its own logger.
     *
     * @return {@code record}, for the rest of the fluent chain
     */
    public static LoggingEventBuilder scheduled(
            LoggingEventBuilder record, Operation operation, String job, String cron,
            String description) {
        return LogEvent.classify(record, operation, Category.CONFIGURATION, Type.INFO)
                .addKeyValue(LogContext.JOB_NAME, job)
                .addKeyValue(LogEvent.JOB_DESCRIPTION, description)
                .addKeyValue(LogEvent.TRIGGER_CRON_EXPRESSION, cron)
                .addKeyValue(LogEvent.TRIGGER_CRON_TIMEZONE, ServiceTimeZone.ZONE.getId());
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
                        LogEvent.classify(log.atInfo(), operation, Category.BATCH, Type.JOB_START)
                                .log("Scheduled job started");
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
                        logEnd(operation, result.skipped(), endedAt - startedAt);
                    }
                });
    }

    private static void logEnd(Operation operation, boolean skipped, long durationMillis) {
        LoggingEventBuilder end = LogEvent.classify(
                        log.atInfo(), operation, Category.BATCH, Type.JOB_END)
                .addKeyValue(LogEvent.OUTCOME, LogEvent.SUCCESS)
                .addKeyValue(LogEvent.DURATION_MS, durationMillis);
        if (skipped) {
            end.addKeyValue(LogEvent.REASON, LogEvent.REASON_LOCK_HELD)
                    .log("Scheduled job skipped: another run holds its lock");
        } else {
            end.log("Scheduled job completed");
        }
    }

    /**
     * The failed run's one application record, at {@code ERROR}, with the exception attached
     * whole. Not a redacted copy, as the SCIM handler attaches: the same exception goes on
     * to the scheduler's own error handler, which logs it whole regardless, so a redacted
     * copy here would hide nothing and would cost {@code error.type} its real class name.
     */
    private static void logFailure(Operation operation, RuntimeException failure, long durationMillis) {
        LogEvent.classify(
                        LogEvent.atError(log, FAILED_RUN_ERROR_CODE, category(failure), true)
                                .setCause(failure),
                        operation, Category.BATCH, Type.JOB_END)
                .addKeyValue(LogEvent.OUTCOME, LogEvent.FAILURE)
                .addKeyValue(LogEvent.SEVERITY, Severity.HIGH.value())
                .addKeyValue(LogEvent.DURATION_MS, durationMillis)
                .log("Scheduled job failed");
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
