package com.example.backend.counter.domain;

import java.util.UUID;

/**
 * A per-user tally. Pure domain: no persistence or framework concerns.
 *
 * <p>Keyed by the account's stable, non-reassignable id rather than its
 * username: a username may be renamed, and the tally must keep pointing at the
 * same account afterward.
 */
public class UserCounter {

    private final UUID accountId;

    private long count;

    private UserCounter(UUID accountId, long count) {
        this.accountId = accountId;
        this.count = count;
    }

    /** A brand new counter, starting at zero. */
    public static UserCounter createFor(UUID accountId) {
        return new UserCounter(accountId, 0L);
    }

    /** Rebuilds a counter from previously stored state. */
    public static UserCounter rehydrate(UUID accountId, long count) {
        return new UserCounter(accountId, count);
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

    public UUID getAccountId() {
        return accountId;
    }
}
