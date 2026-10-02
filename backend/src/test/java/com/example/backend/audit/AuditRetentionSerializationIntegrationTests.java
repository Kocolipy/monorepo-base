package com.example.backend.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.ContainerTestConfiguration;
import com.example.backend.audit.domain.AuditRetentionPolicy;
import com.example.backend.observability.EcsLogCapture;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.scheduling.config.Task;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;

/**
 * Two overlapping runs of the audit retention job, against real Postgres: one deletes, the
 * other finds the job's lock held and deletes nothing.
 *
 * <p>Both runs are the task the scheduler actually holds, not a re-wrapped copy of the
 * service, so the claim covers the deployed wiring — the lock, the {@code lock-held} end the
 * wrapper writes for a skipped run, and the migration that gives the job its lock row.
 *
 * <p>The overlap is made by the database rather than by a test hook in the job. This test
 * row-locks one of the aged-out events it inserted, so the first run takes the job's lock,
 * reaches its {@code DELETE} and waits there on that row — still inside its transaction, still
 * holding the job's lock — exactly as a slow delete on another instance would. The second run
 * starts only once Postgres reports the first one waiting. Releasing the row lets the first
 * run finish.
 *
 * <p>The annotations match {@code EcsLogFormatTests}', so the two share one cached context
 * and one Postgres container.
 */
@SpringBootTest
@Import(ContainerTestConfiguration.class)
class AuditRetentionSerializationIntegrationTests {

    private static final String INSERT_EVENT = """
            INSERT INTO audit_events
                (id, occurred_at, operation, outcome, resource_type, status_class)
                VALUES (?, ?, 'LOGOUT', 'SUCCESS', 'User', 'ok')""";

    private static final String COUNT_EVENT = "SELECT count(*) FROM audit_events WHERE id = ?";

    private static final String COUNT_AGED_OUT =
            "SELECT count(*) FROM audit_events WHERE occurred_at < ?";

    private static final String ROW_LOCK_EVENT =
            "SELECT id FROM audit_events WHERE id = ? FOR UPDATE";

    /** A backend waiting on a row lock in the retention delete — the first run, held. */
    private static final String WAITING_RETENTION_DELETES = """
            SELECT count(*) FROM pg_stat_activity
             WHERE wait_event_type = 'Lock'
               AND query LIKE 'DELETE FROM audit_events WHERE occurred_at < %'""";

    private static final String DELETE_EVENT = "DELETE FROM audit_events WHERE id = ?";

    private static final String ASSUME_RETENTION_ROLE =
            "SET LOCAL ROLE backend_audit_retention";

    private static final String FIRST_RUN_THREAD = "retention-first-run";

    @Autowired
    private ScheduledTaskHolder scheduledTasks;

    @Autowired
    private AuditRetentionPolicy policy;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private Environment environment;

    private final List<UUID> inserted = new ArrayList<>();

    private EcsLogCapture logs;

    @BeforeEach
    void attachLogs() {
        logs = EcsLogCapture.attach(environment);
    }

    @AfterEach
    void cleanUp() {
        logs.close();
        // Only the rows this class inserted; recent ones would otherwise outlive the test.
        // The table is append-only to every role but retention's, so the cleanup assumes it.
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.execute(ASSUME_RETENTION_ROLE);
            inserted.forEach(id -> jdbc.update(DELETE_EVENT, id));
        });
        inserted.clear();
    }

    @Test
    void ofTwoOverlappingRunsOneDeletesAndTheOtherSkipsLockHeldDeletingNothing()
            throws Exception {
        Instant agedOutAt = Instant.now().minus(policy.period()).minus(Duration.ofDays(30));
        UUID held = insertEventAt(agedOutAt);
        UUID alsoAgedOut = insertEventAt(agedOutAt);
        UUID recent = insertEventAt(Instant.now());
        Runnable retention = theScheduledRetentionTask();

        CountDownLatch rowLocked = new CountDownLatch(1);
        CountDownLatch releaseRow = new CountDownLatch(1);
        ExecutorService rowHolder = Executors.newSingleThreadExecutor();
        ExecutorService firstRunner = Executors.newSingleThreadExecutor(
                runnable -> new Thread(runnable, FIRST_RUN_THREAD));
        try {
            Future<?> holding = rowHolder.submit(() -> new TransactionTemplate(transactionManager)
                    .executeWithoutResult(status -> {
                        jdbc.queryForList(ROW_LOCK_EVENT, UUID.class, held);
                        rowLocked.countDown();
                        awaitQuietly(releaseRow);
                    }));
            assertThat(rowLocked.await(30, TimeUnit.SECONDS)).isTrue();
            long agedOutBefore = jdbc.queryForObject(
                    COUNT_AGED_OUT, Long.class, Timestamp.from(Instant.now().minus(policy.period())));

            Future<?> first = firstRunner.submit(retention);
            awaitFirstRunWaitingInsideItsDelete();

            // The second run, here, while the first holds the job's lock mid-delete.
            String secondThread = Thread.currentThread().getName();
            retention.run();
            assertThat(jdbc.queryForObject(COUNT_EVENT, Integer.class, alsoAgedOut))
                    .as("nothing the first run has not committed is gone")
                    .isEqualTo(1);

            releaseRow.countDown();
            holding.get(60, TimeUnit.SECONDS);
            first.get(60, TimeUnit.SECONDS);

            List<JsonNode> firstRun = onThread(FIRST_RUN_THREAD);
            List<JsonNode> secondRun = onThread(secondThread);

            assertThat(firstRun).extracting(record -> record.at("/message").asText())
                    .containsExactly("Scheduled job started", "Audit retention run complete",
                            "Scheduled job completed");
            assertThat(firstRun.get(1).at("/audit/retention/deleted_rows").asLong())
                    .isEqualTo(agedOutBefore)
                    .isGreaterThanOrEqualTo(2);
            assertThat(firstRun.get(2).at("/event/outcome").asText()).isEqualTo("success");
            assertThat(firstRun.get(2).at("/event/reason").isMissingNode())
                    .as("the run that did the work has no skip reason").isTrue();

            assertThat(secondRun).extracting(record -> record.at("/message").asText())
                    .containsExactly("Scheduled job started",
                            "Scheduled job skipped: another run holds its lock");
            assertThat(secondRun.get(1).at("/event/reason").asText()).isEqualTo("lock-held");
            assertThat(secondRun.get(1).at("/event/outcome").asText()).isEqualTo("success");
            assertThat(secondRun.get(1).at("/batch/job/name").asText()).isEqualTo("audit-retention");

            assertThat(jdbc.queryForObject(COUNT_EVENT, Integer.class, held)).isZero();
            assertThat(jdbc.queryForObject(COUNT_EVENT, Integer.class, alsoAgedOut)).isZero();
            assertThat(jdbc.queryForObject(COUNT_EVENT, Integer.class, recent)).isEqualTo(1);
        } finally {
            releaseRow.countDown();
            rowHolder.shutdownNow();
            firstRunner.shutdownNow();
        }
    }

    /**
     * The single-instance case is unchanged: with no other run, a run takes the lock, deletes
     * what has aged out and logs it, and the next run takes the lock again.
     */
    @Test
    void aLoneRunTakesTheLockDeletesAndLogsAndTheNextRunCanTakeItAgain() {
        UUID agedOut = insertEventAt(
                Instant.now().minus(policy.period()).minus(Duration.ofDays(30)));
        Runnable retention = theScheduledRetentionTask();
        String thread = Thread.currentThread().getName();

        retention.run();
        List<JsonNode> firstRun = onThread(thread);
        logs.reset();
        retention.run();
        List<JsonNode> secondRun = onThread(thread);

        assertThat(jdbc.queryForObject(COUNT_EVENT, Integer.class, agedOut)).isZero();
        assertThat(firstRun).extracting(record -> record.at("/message").asText())
                .containsExactly("Scheduled job started", "Audit retention run complete",
                        "Scheduled job completed");
        assertThat(firstRun.get(1).at("/audit/retention/deleted_rows").asLong())
                .isGreaterThanOrEqualTo(1);
        assertThat(secondRun).extracting(record -> record.at("/message").asText())
                .as("the first run's commit released the lock")
                .containsExactly("Scheduled job started", "Audit retention run complete",
                        "Scheduled job completed");
    }

    private Runnable theScheduledRetentionTask() {
        List<Runnable> tasks = scheduledTasks.getScheduledTasks().stream()
                .map(ScheduledTask::getTask)
                .filter(CronTask.class::isInstance)
                .map(CronTask.class::cast)
                .filter(task -> task.getExpression().equals(policy.schedule()))
                .map(Task::getRunnable)
                .toList();
        assertThat(tasks).as("the scheduled retention task").hasSize(1);
        return tasks.getFirst();
    }

    /**
     * Waits until Postgres reports a backend blocked on a row lock inside the retention
     * delete: the first run has taken the job's lock and is holding it, mid-transaction.
     */
    private void awaitFirstRunWaitingInsideItsDelete() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (System.nanoTime() < deadline) {
            Long waiting = jdbc.queryForObject(WAITING_RETENTION_DELETES, Long.class);
            if (waiting != null && waiting > 0) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("the first retention run never reached its delete");
    }

    private List<JsonNode> onThread(String thread) {
        return logs.records().stream()
                .filter(record -> thread.equals(record.at("/process/thread/name").asText()))
                .toList();
    }

    private UUID insertEventAt(Instant occurredAt) {
        UUID id = UUID.randomUUID();
        jdbc.update(INSERT_EVENT, id, Timestamp.from(occurredAt));
        inserted.add(id);
        return id;
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
