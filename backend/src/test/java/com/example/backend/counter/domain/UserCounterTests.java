package com.example.backend.counter.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Covers the aggregate now that its behaviour is separated from JPA. These run
 * without Spring or a database.
 *
 * <p>The key is the SCIM User's stable resource id — the identity a counter belongs
 * to — which is the same value the former account id was, under the name the
 * unified identity model gives it.
 */
class UserCounterTests {

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void newCounterStartsAtZero() {
        assertThat(UserCounter.createFor(USER_ID).getCount()).isZero();
    }

    @Test
    void incrementReturnsTheUpdatedCount() {
        UserCounter counter = UserCounter.createFor(USER_ID);

        assertThat(counter.increment()).isEqualTo(1);
        assertThat(counter.increment()).isEqualTo(2);
        assertThat(counter.getCount()).isEqualTo(2);
    }

    @Test
    void resetReturnsCountToZero() {
        UserCounter counter = UserCounter.createFor(USER_ID);
        counter.increment();

        counter.reset();

        assertThat(counter.getCount()).isZero();
    }

    @Test
    void rehydratePreservesStoredState() {
        UserCounter counter = UserCounter.rehydrate(USER_ID, 7L);

        assertThat(counter.getUserId()).isEqualTo(USER_ID);
        assertThat(counter.getCount()).isEqualTo(7L);
        assertThat(counter.increment()).isEqualTo(8L);
    }
}
