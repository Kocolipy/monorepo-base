package com.example.backend.observability;

/**
 * What one run of a job that serializes on a lock reports back to
 * {@link ScheduledJobMetrics#instrumentLocked}: whether it found another run of the same
 * job holding the lock, and so did no work.
 *
 * <p>The lock is taken inside the job, in the job's own transaction, so only the job knows
 * whether it ran; this is how it says so, and the wrapper logs the run's end accordingly.
 */
public interface SkippableJobRun {

    /** Whether another run of the same job held the lock, so this one did nothing. */
    boolean skipped();
}
