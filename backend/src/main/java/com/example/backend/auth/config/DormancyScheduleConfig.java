package com.example.backend.auth.config;

import com.example.backend.auth.application.DormancyRun;
import com.example.backend.auth.application.DormancyService;
import com.example.backend.observability.LogEvent;
import com.example.backend.observability.ScheduledJobMetrics;
import com.example.backend.observability.ServiceTimeZone;
import com.example.backend.scim.domain.DormancyPolicy;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

/**
 * Puts the dormancy job on its daily cron, records the schedule and windows it was put on, and
 * counts what its runs change.
 *
 * <p>The schedule is fixed rather than configurable; the specification makes the windows a
 * deployment decision and fixes the time of day at 04:00, staggered from the audit retention
 * job's 03:30. Both are evaluated in {@link ServiceTimeZone#ZONE}, the zone the log timestamps
 * are written in.
 *
 * <p>The job runs through {@link ScheduledJobMetrics#instrumentLocked}, so a run is counted, is
 * its own trace, and is logged from start to end under its own {@code batch.job.run.id} — a run
 * that finds its lock held as {@code lock-held}, a run that did the work with its counts. Beside
 * those, two counters make an unexpected mass lockout or revocation visible as a spike:
 * {@value #LOCKED_USERS} and {@value #ROLES_REVOKED_USERS}. They are incremented once the run has
 * returned, which is after its transaction committed, so a rolled-back run counts nothing.
 */
@Configuration
@EnableScheduling
public class DormancyScheduleConfig implements SchedulingConfigurer {

    /** Daily at 04:00. */
    public static final String SCHEDULE = "0 0 4 * * *";

    /**
     * The {@code job} tag of the dormancy job's run metrics ({@link ScheduledJobMetrics}), and
     * its {@code batch.job.name}. {@code ops/prometheus/alerts.yaml}'s {@code DormancyJobFailed}
     * and {@code DormancyJobNotRunning} select on exactly this value.
     */
    static final String JOB = "dormancy";

    /** What the dormancy job does, on its startup record. */
    public static final String DESCRIPTION =
            "Locks every User that has not authenticated within the dormancy lockout window, and"
                    + " removes the mapped Group memberships of every User that has not"
                    + " authenticated within the role revocation window";

    /** Users the dormancy job locked, counted per run. */
    static final String LOCKED_USERS = "app.dormancy.users.locked";

    /** Users whose Roles the dormancy job revoked, counted per run. */
    static final String ROLES_REVOKED_USERS = "app.dormancy.users.roles.revoked";

    private static final Logger log = LoggerFactory.getLogger(DormancyScheduleConfig.class);

    private final DormancyService dormancy;
    private final DormancyPolicy policy;
    private final ScheduledJobMetrics jobs;
    private final MeterRegistry registry;

    public DormancyScheduleConfig(
            DormancyService dormancy,
            DormancyPolicy policy,
            ScheduledJobMetrics jobs,
            MeterRegistry registry) {
        this.dormancy = dormancy;
        this.policy = policy;
        this.jobs = jobs;
        this.registry = registry;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        Counter locked = Counter.builder(LOCKED_USERS)
                .description("Users the dormancy job locked")
                .register(registry);
        Counter rolesRevoked = Counter.builder(ROLES_REVOKED_USERS)
                .description("Users whose mapped Group memberships the dormancy job removed")
                .register(registry);
        registrar.addCronTask(new CronTask(
                jobs.instrumentLocked(JOB, DormancyService.OPERATION, () -> {
                    DormancyRun run = dormancy.run();
                    locked.increment(run.locked().size());
                    rolesRevoked.increment(run.rolesRevoked().size());
                    return run;
                }),
                new CronTrigger(SCHEDULE, ServiceTimeZone.ZONE)));
        LogEvent.jobScheduled(log, DormancyService.OPERATION, JOB, SCHEDULE, DESCRIPTION)
                .addKeyValue(LogEvent.DORMANCY_LOCKOUT_WINDOW, policy.lockoutWindow().toString())
                .addKeyValue(LogEvent.DORMANCY_ROLE_REVOCATION_WINDOW,
                        policy.roleRevocationWindow().toString())
                .log();
    }
}
