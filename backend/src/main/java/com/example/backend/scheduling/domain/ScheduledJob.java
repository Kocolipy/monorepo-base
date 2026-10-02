package com.example.backend.scheduling.domain;

/**
 * The scheduled jobs that must never overlap themselves, each with the name it is serialized
 * under.
 *
 * <p>A closed set because the lock rows are: every member here has a row in
 * {@code scheduled_job_locks}, written by the migration that introduced it, and a job the
 * schema does not know cannot be serialized at all.
 *
 * <p>The lock lives in its own shared module rather than in any one job's: the dormancy jobs
 * belong to {@code auth} and the retention job to {@code audit}, and {@code audit} may depend on
 * no business module. See {@code /docs/adr/0005-serialize-scheduled-jobs-on-per-job-lock-rows.md}.
 */
public enum ScheduledJob {

    /** Deactivates Users past the inactivity window. */
    INACTIVITY_DEACTIVATION("inactivity-deactivation"),

    /** Removes the Admin-group membership of Users past the dormant-authority window. */
    DORMANT_AUTHORITY_REVOCATION("dormant-authority-revocation"),

    /** Deletes audit events older than the retention period. */
    AUDIT_RETENTION("audit-retention");

    private final String lockName;

    ScheduledJob(String lockName) {
        this.lockName = lockName;
    }

    /** The name the job's lock row is stored under. */
    public String lockName() {
        return lockName;
    }
}
