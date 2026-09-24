package com.example.backend.counter.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserCounterServiceTests {

    @Autowired
    private UserCounterService service;

    @Test
    void getsExistingCountWithoutChangingIt() {
        service.increment("ada");
        service.increment("ada");

        assertThat(service.getCount("ada")).isEqualTo(2);
        assertThat(service.getCount("ada")).isEqualTo(2);
    }

    @Test
    void getsZeroWhenUserHasNoCounterYet() {
        assertThat(service.getCount("ada")).isZero();
    }

    @Test
    void maintainsIndependentCountsForEachUser() {
        assertThat(service.increment("ada")).isEqualTo(1);
        assertThat(service.increment("ada")).isEqualTo(2);
        assertThat(service.increment("grace")).isEqualTo(1);
    }

    @Test
    void resetSetsExistingCountToZero() {
        service.increment("ada");
        service.increment("ada");

        assertThat(service.reset("ada")).isZero();
        assertThat(service.increment("ada")).isEqualTo(1);
    }

    @Test
    void resetCreatesAZeroCountWhenUserHasNoCounterYet() {
        assertThat(service.reset("ada")).isZero();
        assertThat(service.increment("ada")).isEqualTo(1);
    }
}
