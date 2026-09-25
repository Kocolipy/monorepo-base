package com.example.backend.audit.config;

import com.example.backend.audit.application.AuditRetentionService;
import com.example.backend.audit.domain.AuditRetentionPolicy;
import com.example.backend.observability.LogEvent;
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

    private final AuditRetentionService retention;
    private final AuditRetentionPolicy policy;

    public AuditRetentionScheduleConfig(
            AuditRetentionService retention, AuditRetentionPolicy policy) {
        this.retention = retention;
        this.policy = policy;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addCronTask(new CronTask(
                retention::deleteAgedOutEvents, new CronTrigger(policy.schedule())));
        log.atInfo()
                .addKeyValue(LogEvent.ACTION, AuditRetentionService.RETENTION_ACTION)
                .addKeyValue(LogEvent.RETENTION_SCHEDULE, policy.schedule())
                .addKeyValue(LogEvent.RETENTION_PERIOD, policy.period().toString())
                .log("Audit retention job scheduled");
    }
}
