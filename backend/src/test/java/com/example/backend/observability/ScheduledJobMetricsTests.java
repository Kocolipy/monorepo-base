package com.example.backend.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * The run metrics a scheduled job publishes, read back from a registry: what each outcome
 * does to the counters and to the last-success time, and what they read before the job
 * has run at all — which is what the never-ran alert depends on.
 */
class ScheduledJobMetricsTests {

    private static final Instant SCHEDULED = Instant.parse("2026-09-30T00:00:00Z");

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    private final AtomicReference<Instant> now = new AtomicReference<>(SCHEDULED);

    /** Every observation a run opened, in the order they stopped. */
    private final List<Observation.Context> stopped = new ArrayList<>();

    private final ObservationRegistry observations = ObservationRegistry.create();

    {
        observations.observationConfig().observationHandler(new ObservationHandler<>() {
            @Override
            public boolean supportsContext(Observation.Context context) {
                return true;
            }

            @Override
            public void onStop(Observation.Context context) {
                stopped.add(context);
            }
        });
    }

    private final ScheduledJobMetrics jobs = new ScheduledJobMetrics(registry, observations, new Clock() {
        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    });

    @Test
    void before_any_run_both_outcomes_read_zero_and_last_success_reads_the_scheduling_time() {
        jobs.instrument("probe", () -> { });

        assertThat(runs("success")).isZero();
        assertThat(runs("failure")).isZero();
        assertThat(lastSuccessSeconds()).isEqualTo(SCHEDULED.getEpochSecond());
    }

    @Test
    void a_successful_run_counts_a_success_and_moves_the_last_success_time() {
        Runnable job = jobs.instrument("probe", () -> { });
        now.set(SCHEDULED.plusSeconds(3600));

        job.run();

        assertThat(runs("success")).isEqualTo(1);
        assertThat(runs("failure")).isZero();
        assertThat(lastSuccessSeconds()).isEqualTo(SCHEDULED.plusSeconds(3600).getEpochSecond());
    }

    @Test
    void a_failed_run_counts_a_failure_rethrows_it_and_leaves_the_last_success_time() {
        IllegalStateException failure = new IllegalStateException("job failed");
        Runnable job = jobs.instrument("probe", () -> {
            throw failure;
        });
        now.set(SCHEDULED.plusSeconds(3600));

        assertThatThrownBy(job::run).isSameAs(failure);

        assertThat(runs("failure")).isEqualTo(1);
        assertThat(runs("success")).isZero();
        assertThat(lastSuccessSeconds()).isEqualTo(SCHEDULED.getEpochSecond());
    }

    /** The success counter moves only after the job returned, never before it ran. */
    @Test
    void the_job_itself_runs_once_per_run() {
        int[] ran = {0};
        Runnable job = jobs.instrument("probe", () -> ran[0]++);

        job.run();
        job.run();

        assertThat(ran[0]).isEqualTo(2);
        assertThat(runs("success")).isEqualTo(2);
    }

    @Test
    void jobs_are_told_apart_by_their_job_tag() {
        jobs.instrument("probe", () -> { }).run();
        jobs.instrument("other-job", () -> { });

        assertThat(registry.get("app.job.runs").tag("job", "other-job")
                .tag("outcome", "success").counter().count()).isZero();
        assertThat(runs("success")).isEqualTo(1);
    }

    /**
     * Both series carry a description, which is the {@code # HELP} line an operator reads in
     * the scrape beside the alert that fired on it.
     */
    @Test
    void both_series_are_described_for_the_scrape() {
        jobs.instrument("probe", () -> { });

        assertThat(registry.get("app.job.runs").tag("job", "probe").tag("outcome", "success")
                .counter().getId().getDescription()).isNotBlank();
        assertThat(registry.get("app.job.runs").tag("job", "probe").tag("outcome", "failure")
                .counter().getId().getDescription()).isNotBlank();
        assertThat(registry.get("app.job.last.success").tag("job", "probe")
                .timeGauge().getId().getDescription()).isNotBlank();
    }

    /**
     * A run executes inside an observation of its own — the one Boot's tracing handler
     * turns into the run's trace — named for runs and tagged with the job, and the
     * observation ends when the run does.
     */
    @Test
    void each_run_executes_inside_its_own_observation_tagged_with_the_job() {
        List<Observation> current = new ArrayList<>();
        Runnable job = jobs.instrument("probe", () -> current.add(observations.getCurrentObservation()));

        job.run();
        job.run();

        assertThat(current).hasSize(2).doesNotContainNull();
        assertThat(current.get(0)).isNotSameAs(current.get(1));
        assertThat(stopped).hasSize(2);
        assertThat(stopped).allSatisfy(context -> {
            assertThat(context.getName()).isEqualTo("app.job.run");
            assertThat(context.getLowCardinalityKeyValue("job").getValue()).isEqualTo("probe");
            assertThat(context.getError()).isNull();
        });
        assertThat(current).extracting(Observation::getContextView)
                .containsExactlyElementsOf(stopped);
        assertThat(observations.getCurrentObservation()).as("closed after the run").isNull();
    }

    /** No observation is opened by scheduling a job, only by running it. */
    @Test
    void scheduling_a_job_opens_no_observation() {
        jobs.instrument("probe", () -> { });

        assertThat(stopped).isEmpty();
    }

    /** A failed run's observation ends too, carrying the failure, and the failure still escapes. */
    @Test
    void a_failed_run_ends_its_observation_with_the_failure() {
        IllegalStateException failure = new IllegalStateException("job failed");
        Runnable job = jobs.instrument("probe", () -> {
            throw failure;
        });

        assertThatThrownBy(job::run).isSameAs(failure);

        assertThat(stopped).singleElement()
                .satisfies(context -> assertThat(context.getError()).isSameAs(failure));
    }

    private double runs(String outcome) {
        return registry.get("app.job.runs").tag("job", "probe").tag("outcome", outcome)
                .counter().count();
    }

    private double lastSuccessSeconds() {
        return registry.get("app.job.last.success").tag("job", "probe")
                .timeGauge().value(TimeUnit.SECONDS);
    }
}
