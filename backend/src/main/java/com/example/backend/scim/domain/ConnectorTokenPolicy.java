package com.example.backend.scim.domain;

import java.time.Duration;
import java.time.Instant;

/**
 * The bounds on a connector token's life, and the arithmetic of a rotation
 * overlap.
 *
 * <p>Constants rather than configuration. A deployment that could raise the
 * maximum lifetime would make "a connector token lives at most a year" a claim
 * about one environment instead of about the service, and the number exists to
 * bound the damage of a leaked credential — which is not a per-deployment
 * preference. An Admin may ask for less; nobody may ask for more.
 */
public final class ConnectorTokenPolicy {

    /** The hard ceiling on a token's life, and also its default. */
    public static final Duration MAX_LIFETIME = Duration.ofDays(365);

    /** What an issue request with no stated lifetime gets. */
    public static final Duration DEFAULT_LIFETIME = MAX_LIFETIME;

    /**
     * The longest an old token may keep working after being rotated. Long enough
     * to redeploy an integration, short enough that a rotation nobody finished is
     * not a second live credential for a year.
     */
    public static final Duration MAX_ROTATION_OVERLAP = Duration.ofDays(14);

    private ConnectorTokenPolicy() {
    }

    /**
     * The lifetime to issue with.
     *
     * @param requested what the Admin asked for, or {@code null} for the default
     * @throws InvalidConnectorTokenLifetimeException if the request is not positive
     *                                                or exceeds {@link #MAX_LIFETIME}
     */
    public static Duration lifetime(Duration requested) {
        if (requested == null) {
            return DEFAULT_LIFETIME;
        }
        if (requested.isZero() || requested.isNegative()) {
            throw new InvalidConnectorTokenLifetimeException(
                    "A token lifetime must be positive");
        }
        if (requested.compareTo(MAX_LIFETIME) > 0) {
            throw new InvalidConnectorTokenLifetimeException(
                    "A token lifetime may not exceed " + MAX_LIFETIME.toDays() + " days");
        }
        return requested;
    }

    /**
     * When a rotated token's overlap window ends.
     *
     * <p>The earlier of the requested overlap and the expiry the old token already
     * had. Three cases, and the second is the one worth stating: a requested
     * overlap longer than the token had left does not extend it, and a token
     * already expired or with days left short of the window keeps its own expiry.
     * Clamping happens here rather than being refused, because an Admin asking for
     * "as much overlap as possible" on a token with three days left is asking for
     * three days, not making a mistake.
     *
     * <p>A negative or {@code null} overlap means none: the old token ends now.
     *
     * @param now                the rotation's instant
     * @param requestedOverlap   how long the old token should keep working
     * @param currentExpiry      the old token's expiry as it stands, which is also
     *                           the ceiling — using the ISSUED expiry instead would
     *                           let a second rotation undo the first one's shortening
     */
    public static Instant overlapEnd(
            Instant now, Duration requestedOverlap, Instant currentExpiry) {
        Duration overlap = requestedOverlap == null || requestedOverlap.isNegative()
                ? Duration.ZERO
                : requestedOverlap;
        if (overlap.compareTo(MAX_ROTATION_OVERLAP) > 0) {
            overlap = MAX_ROTATION_OVERLAP;
        }
        Instant requestedEnd = now.plus(overlap);
        return requestedEnd.isBefore(currentExpiry) ? requestedEnd : currentExpiry;
    }
}
