package com.example.backend.auth.domain;

import java.time.Duration;

/**
 * How many consecutive failed logins an account tolerates, and how long it stays
 * locked once that many have happened. Held as a value rather than read from
 * configuration where it is applied, so the lockout rule can be exercised
 * without a running application.
 */
public record LockoutPolicy(int maxAttempts, Duration lockDuration) {

    public LockoutPolicy {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("A lockout policy needs at least one attempt");
        }
        if (lockDuration == null || lockDuration.isNegative() || lockDuration.isZero()) {
            throw new IllegalArgumentException("A lockout needs a positive duration");
        }
    }
}
