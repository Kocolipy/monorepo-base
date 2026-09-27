package com.example.backend.counter.domain;

import java.util.UUID;

/**
 * A per-user tally. Pure domain: no persistence or framework concerns.
 *
 * <p>Keyed by the SCIM User's stable, non-reassignable resource id rather than its
 * userName: a userName is mutable, and the tally must keep pointing at the
 * same identity afterward.
 */
public class UserCounter {

    private final UUID userId;

    private long count;

    private UserCounter(UUID userId, long count) {
        this.userId = userId;
        this.count = count;
    }

    /** A brand new counter, starting at zero. */
    public static UserCounter createFor(UUID userId) {
        return new UserCounter(userId, 0L);
    }

    /** Rebuilds a counter from previously stored state. */
    public static UserCounter rehydrate(UUID userId, long count) {
        return new UserCounter(userId, count);
    }

    public long increment() {
        return ++count;
    }

    public void reset() {
        count = 0;
    }

    public long getCount() {
        return count;
    }

    public UUID getUserId() {
        return userId;
    }
}
