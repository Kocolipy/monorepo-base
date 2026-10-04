package com.example.backend.scim.domain;

import java.time.Duration;
import java.time.Instant;

/**
 * How long a User may go without authenticating before each step of the dormancy job applies to
 * it (ADR 0011).
 *
 * <p>Two windows, both measured on the same basis — {@link ScimUser#dormancyBasis()}, the last
 * successful login, explicit reactivation or Unlock, falling back to creation:
 *
 * <ul>
 *   <li><strong>lockout</strong> — past it, the User is locked with {@link LockCause#DORMANCY}
 *       until an administrator's Unlock;
 *   <li><strong>role revocation</strong> — past it, the User's direct membership of every mapped
 *       Group is removed, so it holds no Role even if it is later unlocked.
 * </ul>
 *
 * <p>Here, beside the state it governs, for the reason {@link LockoutPolicy} is: the rule reads
 * the User, and the dependency between the slices runs from the login surface to the directory.
 * The defaults are the domain's, not a config file's, so a deployment that configures nothing
 * and a test that constructs the policy directly agree on what "90 days" means — the pattern
 * {@code AuditRetentionPolicy} set.
 *
 * <p>The windows are ordered: role revocation is the later step. A role-revocation window not
 * longer than the lockout window would strip the Roles of a User the same run locks — or before
 * it is locked at all — which is a configuration mistake rather than a stricter policy, so it
 * stops startup like a non-positive window does.
 *
 * @param lockoutWindow        how long without authenticating before the dormancy lockout
 * @param roleRevocationWindow how long without authenticating before every mapped Group
 *                             membership is removed
 */
public record DormancyPolicy(Duration lockoutWindow, Duration roleRevocationWindow) {

    /** Ninety days, applied when a deployment configures nothing. */
    public static final Duration DEFAULT_LOCKOUT_WINDOW = Duration.ofDays(90);

    /** One hundred and eighty days, applied when a deployment configures nothing. */
    public static final Duration DEFAULT_ROLE_REVOCATION_WINDOW = Duration.ofDays(180);

    public DormancyPolicy {
        lockoutWindow = lockoutWindow == null
                ? DEFAULT_LOCKOUT_WINDOW
                : requirePositive(lockoutWindow, "lockout");
        roleRevocationWindow = roleRevocationWindow == null
                ? DEFAULT_ROLE_REVOCATION_WINDOW
                : requirePositive(roleRevocationWindow, "role revocation");
        if (roleRevocationWindow.compareTo(lockoutWindow) <= 0) {
            throw new IllegalArgumentException(
                    "The dormancy role revocation window is configured at " + roleRevocationWindow
                            + " and the lockout window at " + lockoutWindow
                            + "; the role revocation window must be longer than the lockout"
                            + " window.");
        }
    }

    /** The policy a deployment that configures nothing runs with. */
    public static DormancyPolicy defaults() {
        return new DormancyPolicy(null, null);
    }

    /** The instant a User's dormancy basis must lie before for the lockout to apply at {@code now}. */
    public Instant lockoutCutoff(Instant now) {
        return now.minus(lockoutWindow);
    }

    /**
     * The instant a User's dormancy basis must lie before for its mapped Group memberships to be
     * removed at {@code now}.
     */
    public Instant roleRevocationCutoff(Instant now) {
        return now.minus(roleRevocationWindow);
    }

    /**
     * Refuses a zero or negative window. Either would make every User dormant the moment the job
     * ran — a deployment-wide lockout from a typo — so it stops startup instead, with the value
     * in the message.
     */
    private static Duration requirePositive(Duration window, String step) {
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException(
                    "The dormancy " + step + " window is configured at " + window
                            + "; it must be positive, or every User would be dormant at once.");
        }
        return window;
    }
}
