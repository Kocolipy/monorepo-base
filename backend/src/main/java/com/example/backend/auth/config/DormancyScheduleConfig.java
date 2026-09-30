package com.example.backend.auth.config;

import com.example.backend.auth.application.DormantAuthorityRevocationService;
import com.example.backend.auth.application.InactivityDeactivationService;
import com.example.backend.observability.LogEvent;
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
 * from the audit retention job's 03:30.
 */
@Configuration
@EnableScheduling
public class DormancyScheduleConfig implements SchedulingConfigurer {

    /** Daily at 04:00. */
    public static final String DEACTIVATION_SCHEDULE = "0 0 4 * * *";

    /** Daily at 04:30. */
    public static final String AUTHORITY_REVOCATION_SCHEDULE = "0 30 4 * * *";

    private static final Logger log = LoggerFactory.getLogger(DormancyScheduleConfig.class);

    private final InactivityDeactivationService deactivation;
    private final DormantAuthorityRevocationService authorityRevocation;
    private final DormancyPolicy policy;

    public DormancyScheduleConfig(
            InactivityDeactivationService deactivation,
            DormantAuthorityRevocationService authorityRevocation,
            DormancyPolicy policy) {
        this.deactivation = deactivation;
        this.authorityRevocation = authorityRevocation;
        this.policy = policy;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addCronTask(new CronTask(
                deactivation::deactivateDormantUsers, new CronTrigger(DEACTIVATION_SCHEDULE)));
        registrar.addCronTask(new CronTask(
                authorityRevocation::revokeDormantAuthority,
                new CronTrigger(AUTHORITY_REVOCATION_SCHEDULE)));
        scheduled(InactivityDeactivationService.ACTION, DEACTIVATION_SCHEDULE,
                policy.deactivationWindow().toString());
        scheduled(DormantAuthorityRevocationService.ACTION, AUTHORITY_REVOCATION_SCHEDULE,
                policy.authorityRevocationWindow().toString());
    }

    private static void scheduled(String action, String schedule, String window) {
        log.atInfo()
                .addKeyValue(LogEvent.ACTION, action)
                .addKeyValue(LogEvent.DORMANCY_SCHEDULE, schedule)
                .addKeyValue(LogEvent.DORMANCY_WINDOW, window)
                .log("Dormancy job scheduled");
    }
}
