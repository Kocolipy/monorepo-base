package com.example.backend.auth.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * How long a session is honoured after it was created, regardless of how
 * recently it was used.
 *
 * <p>Distinct from the idle timeout the servlet container already enforces
 * ({@code server.servlet.session.timeout}): idle timeout resets on every
 * request, so a session kept continuously active never expires on that basis
 * alone. This is the other half — a fixed ceiling from creation, so a session
 * cannot be kept alive indefinitely just by using it often enough.
 *
 * <p>Pure and clock-free for the same reason {@link LockoutPolicy} is: a test
 * exercises the boundary by choosing {@code createdAt} and {@code now}
 * directly, with nothing to wait for and no system clock to fake.
 */
public record AbsoluteSessionLifetimePolicy(Duration maxLifetime) {

    public AbsoluteSessionLifetimePolicy {
        if (maxLifetime == null || maxLifetime.isZero() || maxLifetime.isNegative()) {
            throw new IllegalArgumentException(
                    "An absolute session lifetime needs a positive duration");
        }
    }

    /**
     * Whether a session created at {@code createdAt} has outlived its allowance
     * as of {@code now}. Equal to the boundary counts as not yet expired, the
     * same convention {@link Account#isLocked} uses for its own instant
     * comparison.
     */
    public boolean isExpired(Instant createdAt, Instant now) {
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(now, "now");
        return createdAt.plus(maxLifetime).isBefore(now);
    }
}
