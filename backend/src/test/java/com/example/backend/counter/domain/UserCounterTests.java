package com.example.backend.counter.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Covers the aggregate now that its behaviour is separated from JPA. These run
 * without Spring or a database.
 */
class UserCounterTests {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();

    @Test
    void newCounterStartsAtZero() {
        assertThat(UserCounter.createFor(ACCOUNT_ID).getCount()).isZero();
    }

    @Test
    void incrementReturnsTheUpdatedCount() {
        UserCounter counter = UserCounter.createFor(ACCOUNT_ID);

        assertThat(counter.increment()).isEqualTo(1);
        assertThat(counter.increment()).isEqualTo(2);
        assertThat(counter.getCount()).isEqualTo(2);
    }

    @Test
    void resetReturnsCountToZero() {
        UserCounter counter = UserCounter.createFor(ACCOUNT_ID);
        counter.increment();

        counter.reset();

        assertThat(counter.getCount()).isZero();
    }

    @Test
    void rehydratePreservesStoredState() {
        UserCounter counter = UserCounter.rehydrate(ACCOUNT_ID, 7L);

        assertThat(counter.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(counter.getCount()).isEqualTo(7L);
        assertThat(counter.increment()).isEqualTo(8L);
    }
}
