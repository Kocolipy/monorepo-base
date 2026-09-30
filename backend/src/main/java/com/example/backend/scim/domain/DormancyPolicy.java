package com.example.backend.scim.domain;

import java.time.Duration;
import java.time.Instant;

/**
 * How long a User may go without authenticating before each stage of inactivity governance
 * applies to it.
 *
 * <p>Two windows, both measured on the same basis — {@link ScimUser#dormancyBasis()}, the last
 * successful login or explicit reactivation, falling back to creation:
 *
 * <ul>
 *   <li><strong>deactivation</strong> — past it, the inactivity job sets {@code active=false};
 *   <li><strong>authority revocation</strong> — past it, the dormant-authority job removes the
 *       User's direct membership of the Admin group, leaving baseline access and ordinary Group
 *       memberships alone.
 * </ul>
 *
 * <p>Here, beside the state it governs, for the reason {@link LockoutPolicy} is: the rule reads
 * the User, and the dependency between the slices runs from the login surface to the directory.
 * The defaults are the domain's, not a config file's, so a deployment that configures nothing
 * and a test that constructs the policy directly agree on what "90 days" means — the pattern
 * {@code AuditRetentionPolicy} set.
 *
 * <p>The windows are not required to be ordered. The specification describes authority
 * revocation as a later stage, and the defaults are, but a deployment that wants elevated
 * authority dropped sooner than baseline access is making a stricter choice, not an
 * inconsistent one.
 *
 * @param deactivationWindow        how long without authenticating before deactivation
 * @param authorityRevocationWindow how long without authenticating before the Admin-group
 *                                  membership is removed
 */
public record DormancyPolicy(Duration deactivationWindow, Duration authorityRevocationWindow) {

    /** Ninety days, applied when a deployment configures nothing. */
    public static final Duration DEFAULT_DEACTIVATION_WINDOW = Duration.ofDays(90);

    /** One hundred and eighty days, applied when a deployment configures nothing. */
    public static final Duration DEFAULT_AUTHORITY_REVOCATION_WINDOW = Duration.ofDays(180);

    public DormancyPolicy {
        deactivationWindow = deactivationWindow == null
                ? DEFAULT_DEACTIVATION_WINDOW
                : requirePositive(deactivationWindow, "deactivation");
        authorityRevocationWindow = authorityRevocationWindow == null
                ? DEFAULT_AUTHORITY_REVOCATION_WINDOW
                : requirePositive(authorityRevocationWindow, "authority revocation");
    }

    /** The policy a deployment that configures nothing runs with. */
    public static DormancyPolicy defaults() {
        return new DormancyPolicy(null, null);
    }

    /** The instant a User's dormancy basis must lie before for deactivation to apply at {@code now}. */
    public Instant deactivationCutoff(Instant now) {
        return now.minus(deactivationWindow);
    }

    /**
     * The instant a User's dormancy basis must lie before for its Admin-group membership to be
     * removed at {@code now}.
     */
    public Instant authorityRevocationCutoff(Instant now) {
        return now.minus(authorityRevocationWindow);
    }

    /**
     * Refuses a zero or negative window. Either would make every User dormant the moment the job
     * ran — a deployment-wide deactivation from a typo — so it stops startup instead, with the
     * value in the message.
     */
    private static Duration requirePositive(Duration window, String stage) {
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException(
                    "The inactivity " + stage + " window is configured at " + window
                            + "; it must be positive, or every User would be dormant at once.");
        }
        return window;
    }
}
