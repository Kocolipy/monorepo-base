package com.example.backend.auth.config;

import com.example.backend.auth.application.DormantAuthorityRevocationService;
import com.example.backend.auth.application.InactivityDeactivationService;
import com.example.backend.observability.LogEvent;
import com.example.backend.observability.LogEvent.Category;
import com.example.backend.observability.LogEvent.Operation;
import com.example.backend.observability.LogEvent.Type;
import com.example.backend.observability.ScheduledJobMetrics;
import com.example.backend.observability.ServiceTimeZone;
import com.example.backend.scim.domain.DormancyPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

/**
 * Puts the two dormancy jobs on their daily cron, and records the schedule and window each was
 * put on.
 *
 * <p>Two tasks, never one task running both: each job serializes on its own lock, and running
 * them as one unit would make a slow run of either delay the other. The scheduler's pool is sized
 * for them in {@code application.yaml} ({@code spring.task.scheduling.pool.size}) so they do not
 * queue behind each other in-process either.
 *
 * <p>The schedules are fixed rather than configurable; the specification makes the windows a
 * deployment decision and says nothing of the time of day. They are staggered from each other and
 * from the audit retention job's 03:30. All three are evaluated in {@link ServiceTimeZone#ZONE},
 * the zone the log timestamps are written in.
 *
 * <p>Both run through {@link ScheduledJobMetrics#instrument}, as the retention job does, so a
 * run is counted and is its own trace: every record a run emits carries one {@code trace.id}.
 */
@Configuration
@EnableScheduling
public class DormancyScheduleConfig implements SchedulingConfigurer {

    /** Daily at 04:00. */
    public static final String DEACTIVATION_SCHEDULE = "0 0 4 * * *";

    /** Daily at 04:30. */
    public static final String AUTHORITY_REVOCATION_SCHEDULE = "0 30 4 * * *";

    /**
     * The {@code job} tag of the inactivity job's run metrics ({@link ScheduledJobMetrics}).
     * {@code ops/prometheus/alerts.yaml}'s {@code InactivityJobFailed} and
     * {@code InactivityJobNotRunning} select on exactly this value.
     */
    static final String DEACTIVATION_JOB = "inactivity";

    /** The {@code job} tag of the dormant-authority job's run metrics. */
    static final String AUTHORITY_REVOCATION_JOB = "dormant-authority-revocation";

    private static final Logger log = LoggerFactory.getLogger(DormancyScheduleConfig.class);

    private final InactivityDeactivationService deactivation;
    private final DormantAuthorityRevocationService authorityRevocation;
    private final DormancyPolicy policy;
    private final ScheduledJobMetrics jobs;

    public DormancyScheduleConfig(
            InactivityDeactivationService deactivation,
            DormantAuthorityRevocationService authorityRevocation,
            DormancyPolicy policy,
            ScheduledJobMetrics jobs) {
        this.deactivation = deactivation;
        this.authorityRevocation = authorityRevocation;
        this.policy = policy;
        this.jobs = jobs;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addCronTask(new CronTask(
                jobs.instrument(DEACTIVATION_JOB, deactivation::deactivateDormantUsers),
                new CronTrigger(DEACTIVATION_SCHEDULE, ServiceTimeZone.ZONE)));
        registrar.addCronTask(new CronTask(
                jobs.instrument(AUTHORITY_REVOCATION_JOB, authorityRevocation::revokeDormantAuthority),
                new CronTrigger(AUTHORITY_REVOCATION_SCHEDULE, ServiceTimeZone.ZONE)));
        scheduled(InactivityDeactivationService.OPERATION, DEACTIVATION_SCHEDULE,
                policy.deactivationWindow().toString());
        scheduled(DormantAuthorityRevocationService.OPERATION, AUTHORITY_REVOCATION_SCHEDULE,
                policy.authorityRevocationWindow().toString());
    }

    private static void scheduled(Operation operation, String schedule, String window) {
        LogEvent.classify(log.atInfo(), operation, Category.CONFIGURATION, Type.INFO)
                .addKeyValue(LogEvent.DORMANCY_SCHEDULE, schedule)
                .addKeyValue(LogEvent.DORMANCY_WINDOW, window)
                .log("Dormancy job scheduled");
    }
}
