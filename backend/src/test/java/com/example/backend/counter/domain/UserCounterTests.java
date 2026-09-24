package com.example.backend.counter.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Covers the aggregate now that its behaviour is separated from JPA. These run
 * without Spring or a database.
 */
class UserCounterTests {

    @Test
    void newCounterStartsAtZero() {
        assertThat(UserCounter.createFor("ada").getCount()).isZero();
    }

    @Test
    void incrementReturnsTheUpdatedCount() {
        UserCounter counter = UserCounter.createFor("ada");

        assertThat(counter.increment()).isEqualTo(1);
        assertThat(counter.increment()).isEqualTo(2);
        assertThat(counter.getCount()).isEqualTo(2);
    }

    @Test
    void resetReturnsCountToZero() {
        UserCounter counter = UserCounter.createFor("ada");
        counter.increment();

        counter.reset();

        assertThat(counter.getCount()).isZero();
    }

    @Test
    void rehydratePreservesStoredState() {
        UserCounter counter = UserCounter.rehydrate("ada", 7L);

        assertThat(counter.getUsername()).isEqualTo("ada");
        assertThat(counter.getCount()).isEqualTo(7L);
        assertThat(counter.increment()).isEqualTo(8L);
    }
}
