package com.example.backend.scim.domain;

import java.time.Duration;
import java.time.Instant;

/**
 * How long a User may leave a required password change unmade before the grace-period job
 * deactivates it.
 *
 * <p>The default is the domain's, as {@link DormancyPolicy}'s are, so a deployment that
 * configures nothing and a test that constructs the policy directly agree on what "30 days" means.
 *
 * @param window how long after the flag was set a flagged User stays active
 */
public record PasswordChangeGracePolicy(Duration window) {

    /** Thirty days, applied when a deployment configures nothing. */
    public static final Duration DEFAULT_WINDOW = Duration.ofDays(30);

    public PasswordChangeGracePolicy {
        if (window == null) {
            window = DEFAULT_WINDOW;
        } else if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException(
                    "The password-change grace period is configured at " + window
                            + "; it must be positive, or every flagged User would be deactivated"
                            + " on the next run.");
        }
    }

    /** The policy a deployment that configures nothing runs with. */
    public static PasswordChangeGracePolicy defaults() {
        return new PasswordChangeGracePolicy(null);
    }

    /** The instant the flag must have been set before for deactivation to apply at {@code now}. */
    public Instant cutoff(Instant now) {
        return now.minus(window);
    }
}
