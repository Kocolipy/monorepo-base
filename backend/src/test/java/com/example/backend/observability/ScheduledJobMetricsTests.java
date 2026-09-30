package com.example.backend.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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

    private final ScheduledJobMetrics jobs = new ScheduledJobMetrics(registry, new Clock() {
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

    private double runs(String outcome) {
        return registry.get("app.job.runs").tag("job", "probe").tag("outcome", outcome)
                .counter().count();
    }

    private double lastSuccessSeconds() {
        return registry.get("app.job.last.success").tag("job", "probe")
                .timeGauge().value(TimeUnit.SECONDS);
    }
}
