package com.example.backend.auth.domain;

/**
 * The scheduled jobs that must never overlap themselves, each with the name it is serialized
 * under.
 *
 * <p>A closed set because the lock rows are: every member here has a row in
 * {@code scheduled_job_locks}, written by the migration that introduced it, and a job the
 * schema does not know cannot be serialized at all.
 */
public enum ScheduledJob {

    /** Deactivates Users past the inactivity window. */
    INACTIVITY_DEACTIVATION("inactivity-deactivation"),

    /** Removes the Admin-group membership of Users past the dormant-authority window. */
    DORMANT_AUTHORITY_REVOCATION("dormant-authority-revocation");

    private final String lockName;

    ScheduledJob(String lockName) {
        this.lockName = lockName;
    }

    /** The name the job's lock row is stored under. */
    public String lockName() {
        return lockName;
    }
}
