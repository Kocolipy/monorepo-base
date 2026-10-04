package com.example.backend.audit.config;

import com.example.backend.audit.application.AuditRetentionService;
import com.example.backend.audit.domain.AuditRetentionPolicy;
import com.example.backend.observability.LogEvent;
import com.example.backend.observability.ScheduledJobMetrics;
import com.example.backend.observability.ServiceTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

/**
 * Puts the retention job on its cron, and records the schedule it was put on.
 *
 * <p>The cron is evaluated in {@link ServiceTimeZone#ZONE}, the zone the service's log
 * timestamps are written in, so "03:30" is the 03:30 an operator reads in the job's own
 * records rather than 03:30 in whatever zone the host defaults to.
 *
 * <p>Registered through {@link SchedulingConfigurer} rather than with
 * {@code @Scheduled(cron = "...")} because that annotation's value must be a
 * constant expression: the cron would have to be repeated as a property
 * placeholder instead of read from {@link AuditRetentionPolicy}, which already
 * owns it and its default.
 *
 * <p>The schedule is logged here rather than inside the job. A job only logs once
 * it runs, and the first thing an operator needs to know — after a deploy, and
 * before anything has aged out — is when rows will start disappearing.
 */
@Configuration
@EnableScheduling
public class AuditRetentionScheduleConfig implements SchedulingConfigurer {

    private static final Logger log =
            LoggerFactory.getLogger(AuditRetentionScheduleConfig.class);

    /** The {@code job} tag of this job's run metrics ({@link ScheduledJobMetrics}). */
    static final String RETENTION_JOB = "audit-retention";

    /** What the job does, on its startup record. */
    public static final String RETENTION_DESCRIPTION =
            "Deletes every audit event recorded longer ago than the retention period";

    private final AuditRetentionService retention;
    private final AuditRetentionPolicy policy;
    private final ScheduledJobMetrics jobs;

    public AuditRetentionScheduleConfig(
            AuditRetentionService retention,
            AuditRetentionPolicy policy,
            ScheduledJobMetrics jobs) {
        this.retention = retention;
        this.policy = policy;
        this.jobs = jobs;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addCronTask(new CronTask(
                jobs.instrumentLocked(RETENTION_JOB, AuditRetentionService.OPERATION,
                        retention::deleteAgedOutEvents),
                new CronTrigger(policy.schedule(), ServiceTimeZone.ZONE)));
        LogEvent.jobScheduled(log, AuditRetentionService.OPERATION,
                        RETENTION_JOB, policy.schedule(), RETENTION_DESCRIPTION)
                .addKeyValue(LogEvent.RETENTION_PERIOD, policy.period().toString())
                .log();
    }
}
