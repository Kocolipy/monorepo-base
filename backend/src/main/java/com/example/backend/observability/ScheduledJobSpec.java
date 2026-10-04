package com.example.backend.observability;

import com.example.backend.observability.LogEvent.Operation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * One scheduled job as its schedule config names it, for {@link ScheduledJobMetrics#schedule}
 * to register: the job's name, what it is classified as, its cron and what it does, the task
 * one run executes, the fields particular to its startup record, and which of the counts its
 * runs report are also counters.
 *
 * <p>Everything else — the cron task, the cron trigger in {@link ServiceTimeZone#ZONE}, the
 * startup record, the run metrics and records, and writing each run's counts — is the
 * module's, so a schedule config only names its job.
 */
public final class ScheduledJobSpec {

    /**
     * A count the job's runs report, under {@code countField}, that is also published as the
     * counter {@code name}: registered at zero when the job is scheduled, and moved by what
     * each run that did the work reports.
     */
    record RunCounter(String countField, String name, String description) {}

    private final String name;
    private final Operation operation;
    private final String cron;
    private final String description;
    private final Supplier<? extends SkippableJobRun> task;
    private final Map<String, Object> startupFields;
    private final List<RunCounter> counters;

    private ScheduledJobSpec(
            String name,
            Operation operation,
            String cron,
            String description,
            Supplier<? extends SkippableJobRun> task,
            Map<String, Object> startupFields,
            List<RunCounter> counters) {
        this.name = name;
        this.operation = operation;
        this.cron = cron;
        this.description = description;
        this.task = task;
        this.startupFields = startupFields;
        this.counters = counters;
    }

    /**
     * A job named {@code name} — the {@code job} tag of its run metrics and its
     * {@code batch.job.name} — classified as {@code operation}, run on {@code cron} in the
     * service time zone, described on its startup record by {@code description}, and doing
     * {@code task} each run.
     */
    public static ScheduledJobSpec of(
            String name,
            Operation operation,
            String cron,
            String description,
            Supplier<? extends SkippableJobRun> task) {
        return new ScheduledJobSpec(
                name, operation, cron, description, task, Map.of(), List.of());
    }

    /** This job with {@code field} on its startup record, after any added before it. */
    public ScheduledJobSpec startupField(String field, Object value) {
        Map<String, Object> fields = new LinkedHashMap<>(startupFields);
        fields.put(field, value);
        return new ScheduledJobSpec(name, operation, cron, description, task, fields, counters);
    }

    /**
     * This job with the count its runs report under {@code countField} also published as the
     * counter {@code counter}, described for the scrape by {@code counterDescription}.
     */
    public ScheduledJobSpec countedAs(String countField, String counter, String counterDescription) {
        List<RunCounter> all = new ArrayList<>(counters);
        all.add(new RunCounter(countField, counter, counterDescription));
        return new ScheduledJobSpec(name, operation, cron, description, task, startupFields, all);
    }

    String name() {
        return name;
    }

    Operation operation() {
        return operation;
    }

    String cron() {
        return cron;
    }

    String description() {
        return description;
    }

    Supplier<? extends SkippableJobRun> task() {
        return task;
    }

    Map<String, Object> startupFields() {
        return startupFields;
    }

    List<RunCounter> counters() {
        return counters;
    }
}
