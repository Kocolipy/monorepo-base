package com.example.backend.auth.application;

import com.example.backend.observability.SkippableJobRun;
import java.util.List;
import java.util.UUID;

/**
 * What one run of a dormancy job did: skipped because another run of the same job held its lock,
 * or ran and changed these Users.
 *
 * @param skipped   whether another run of the same job held the lock, so this one did nothing
 * @param processed the Users this run changed, in the order it changed them; empty when it
 *                  skipped or found nobody dormant
 */
public record DormancyRun(boolean skipped, List<UUID> processed) implements SkippableJobRun {

    public DormancyRun {
        processed = List.copyOf(processed);
    }

    /** A run that found the job's lock held and did nothing. */
    static DormancyRun skippedRun() {
        return new DormancyRun(true, List.of());
    }
}
