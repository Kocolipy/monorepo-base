package com.example.backend.counter.domain;

/**
 * A per-user tally. Pure domain: no persistence or framework concerns.
 */
public class UserCounter {

    private final String username;

    private long count;

    private UserCounter(String username, long count) {
        this.username = username;
        this.count = count;
    }

    /** A brand new counter, starting at zero. */
    public static UserCounter createFor(String username) {
        return new UserCounter(username, 0L);
    }

    /** Rebuilds a counter from previously stored state. */
    public static UserCounter rehydrate(String username, long count) {
        return new UserCounter(username, count);
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

    public String getUsername() {
        return username;
    }
}
