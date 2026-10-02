package com.example.backend.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.audit.application.AuditRetentionService;
import com.example.backend.audit.config.AuditRetentionScheduleConfig;
import com.example.backend.audit.domain.AuditRetentionPolicy;
import com.example.backend.auth.application.DormantAuthorityRevocationService;
import com.example.backend.auth.application.InactivityDeactivationService;
import com.example.backend.auth.config.DormancyScheduleConfig;
import com.example.backend.scim.domain.DormancyPolicy;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.SimpleTriggerContext;
import tools.jackson.databind.JsonNode;

/**
 * The scheduled jobs as the scheduler holds them: each cron is evaluated in Singapore time,
 * the zone the log timestamps are written in, and each job runs through
 * {@link ScheduledJobMetrics#instrument} — which is what gives a run its own trace.
 */
@SpringBootTest
@Import(com.example.backend.ContainerTestConfiguration.class)
class ScheduledJobScheduleTests {

    /** 08:00 in Singapore, 00:00 UTC: a moment at which the two zones disagree on every job's next run. */
    private static final Instant NOW = Instant.parse("2026-10-01T00:00:00Z");

    private static final ZoneId SINGAPORE = ZoneId.of("Asia/Singapore");

    @Autowired
    private ScheduledTaskHolder scheduledTasks;

    @Autowired
    private MeterRegistry registry;

    @Autowired
    private Environment environment;

    @Autowired
    private InactivityDeactivationService deactivation;

    @Autowired
    private DormantAuthorityRevocationService authorityRevocation;

    @Autowired
    private AuditRetentionService retentionJob;

    @Autowired
    private ScheduledJobMetrics jobs;

    @Autowired
    private DormancyPolicy dormancy;

    @Autowired
    private AuditRetentionPolicy retention;

    @Test
    void everyCronIsEvaluatedInSingaporeTime() {
        List<CronTask> crons = cronTasks();
        assertThat(crons).as("the three scheduled jobs").hasSize(3);

        for (CronTask cron : crons) {
            CronExpression expression = CronExpression.parse(cron.getExpression());
            Instant inSingapore = expression.next(NOW.atZone(SINGAPORE)).toInstant();
            Instant inUtc = expression.next(NOW.atZone(ZoneOffset.UTC)).toInstant();
            assertThat(inSingapore).as("the zones disagree, so the check discriminates").isNotEqualTo(inUtc);

            Instant next = cron.getTrigger().nextExecution(
                    new SimpleTriggerContext(Clock.fixed(NOW, ZoneOffset.UTC)));

            assertThat(next).as(cron.getExpression()).isEqualTo(inSingapore);
        }
    }

    /**
     * Each job is registered with the run metrics — and so with the observation around each
     * run — under its own name. Series exist from scheduling, so their presence proves the
     * job went through {@link ScheduledJobMetrics#instrument}.
     */
    @Test
    void everyJobRunsThroughTheInstrumentedWrapper() {
        assertThat(List.of("audit-retention", "inactivity", "dormant-authority-revocation"))
                .allSatisfy(job -> assertThat(registry.find("app.job.runs").tag("job", job).counters())
                        .as(job).hasSize(2));
    }

    private List<CronTask> cronTasks() {
        return scheduledTasks.getScheduledTasks().stream()
                .map(ScheduledTask::getTask)
                .filter(CronTask.class::isInstance)
                .map(CronTask.class::cast)
                .toList();
    }

    /**
     * The two configurations built and driven here, against a registrar of the test's own, so
     * what {@code configureTasks} registers and logs is observed while the test runs rather
     * than inferred from a context started earlier. Each job is registered once, on its
     * cron, in Singapore time; the startup record states its schedule and window; and
     * running the registered task gives the run a trace, which only the instrumented wrapper
     * opens — this thread is in no request.
     */
    @Test
    void theDormancyConfigurationRegistersBothJobsInstrumentedInSingaporeTime() {
        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();
        try (EcsLogCapture logs = EcsLogCapture.attach(environment)) {
            new DormancyScheduleConfig(deactivation, authorityRevocation, dormancy, jobs)
                    .configureTasks(registrar);

            List<CronTask> crons = registrar.getCronTaskList();
            assertThat(crons).extracting(CronTask::getExpression).containsExactly(
                    DormancyScheduleConfig.DEACTIVATION_SCHEDULE,
                    DormancyScheduleConfig.AUTHORITY_REVOCATION_SCHEDULE);
            crons.forEach(ScheduledJobScheduleTests::assertEvaluatedInSingaporeTime);

            List<JsonNode> scheduled = records(logs, "Dormancy job scheduled");
            assertThat(scheduled).extracting(record -> record.at("/batch/job/name").asText())
                    .containsExactly("inactivity", "dormant-authority-revocation");
            assertThat(scheduled).extracting(record -> record.at("/trigger/cron/expression").asText())
                    .containsExactly(DormancyScheduleConfig.DEACTIVATION_SCHEDULE,
                            DormancyScheduleConfig.AUTHORITY_REVOCATION_SCHEDULE);
            assertThat(scheduled).extracting(record -> record.at("/trigger/cron/timezone").asText())
                    .containsOnly("Asia/Singapore");
            assertThat(scheduled).extracting(record -> record.at("/app/job/description").asText())
                    .containsExactly(DormancyScheduleConfig.DEACTIVATION_DESCRIPTION,
                            DormancyScheduleConfig.AUTHORITY_REVOCATION_DESCRIPTION)
                    .allSatisfy(description -> assertThat(description).isNotBlank());
            assertThat(scheduled).extracting(record -> record.at("/app/event/action").asText())
                    .containsExactly("identity.inactivity_deactivation",
                            "identity.dormant_authority_revocation");
            assertThat(scheduled).extracting(record -> record.at("/dormancy/window").asText())
                    .containsExactly(dormancy.deactivationWindow().toString(),
                            dormancy.authorityRevocationWindow().toString());

            for (CronTask cron : crons) {
                logs.reset();
                cron.getRunnable().run();
                List<JsonNode> run = onThisThread(logs);
                assertThat(run).as(cron.getExpression()).isNotEmpty()
                        .allSatisfy(record -> assertThat(record.at("/trace/id").asText()).isNotBlank());
                assertJobStartThenJobEndUnderOneRunId(run);
                assertNoJobKeyLeftInContext();
            }
        }
    }

    @Test
    void theRetentionConfigurationRegistersItsJobInstrumentedInSingaporeTime() {
        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();
        try (EcsLogCapture logs = EcsLogCapture.attach(environment)) {
            new AuditRetentionScheduleConfig(retentionJob, retention, jobs).configureTasks(registrar);

            List<CronTask> crons = registrar.getCronTaskList();
            assertThat(crons).extracting(CronTask::getExpression).containsExactly(retention.schedule());
            assertEvaluatedInSingaporeTime(crons.getFirst());

            assertThat(records(logs, "Audit retention job scheduled")).singleElement().satisfies(record -> {
                assertThat(record.at("/batch/job/name").asText()).isEqualTo("audit-retention");
                assertThat(record.at("/trigger/cron/expression").asText()).isEqualTo(retention.schedule());
                assertThat(record.at("/trigger/cron/timezone").asText()).isEqualTo("Asia/Singapore");
                assertThat(record.at("/app/job/description").asText())
                        .isEqualTo(AuditRetentionScheduleConfig.RETENTION_DESCRIPTION).isNotBlank();
                assertThat(record.at("/app/event/action").asText()).isEqualTo("audit.retention");
                assertThat(record.at("/audit/retention/period").asText())
                        .isEqualTo(retention.period().toString());
            });

            logs.reset();
            crons.getFirst().getRunnable().run();
            List<JsonNode> run = onThisThread(logs);
            assertThat(run).isNotEmpty()
                    .allSatisfy(record -> assertThat(record.at("/trace/id").asText()).isNotBlank());
            assertJobStartThenJobEndUnderOneRunId(run);
            assertNoJobKeyLeftInContext();
        }
    }

    /**
     * The run's first record is its {@code job-start} and its last its {@code job-end}, and
     * every record between them — the job's own — carries the same run id.
     */
    private static void assertJobStartThenJobEndUnderOneRunId(List<JsonNode> run) {
        assertThat(run.getFirst().at("/event/type/0").asText()).isEqualTo("job-start");
        assertThat(run.getLast().at("/event/type/0").asText()).isEqualTo("job-end");
        String runId = run.getFirst().at("/batch/job/run/id").asText();
        assertThat(runId).isNotBlank();
        assertThat(run).allSatisfy(record ->
                assertThat(record.at("/batch/job/run/id").asText()).isEqualTo(runId));
    }

    private static void assertNoJobKeyLeftInContext() {
        assertThat(Optional.ofNullable(MDC.getCopyOfContextMap()).orElseGet(Map::of).keySet())
                .noneMatch(key -> key.startsWith("batch.") || key.startsWith("trigger."));
    }

    private static void assertEvaluatedInSingaporeTime(CronTask cron) {
        Instant expected = CronExpression.parse(cron.getExpression()).next(NOW.atZone(SINGAPORE)).toInstant();
        assertThat(cron.getTrigger().nextExecution(new SimpleTriggerContext(Clock.fixed(NOW, ZoneOffset.UTC))))
                .as(cron.getExpression())
                .isEqualTo(expected);
    }

    private static List<JsonNode> records(EcsLogCapture logs, String message) {
        return logs.records().stream()
                .filter(record -> message.equals(record.at("/message").asText()))
                .toList();
    }

    private static List<JsonNode> onThisThread(EcsLogCapture logs) {
        String thread = Thread.currentThread().getName();
        return logs.records().stream()
                .filter(record -> thread.equals(record.at("/process/thread/name").asText()))
                .toList();
    }
}
