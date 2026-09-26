package com.example.backend.counter.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.auth.domain.Account;
import com.example.backend.auth.domain.AccountRepository;
import com.example.backend.auth.domain.AccountRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * Two of the seeded accounts ({@code test-user}, {@code test-admin}) back the
 * single-user assertions below; a counter no longer exists independent of an
 * account, since it is keyed by the account's stable id, so a test needing a
 * second independent user creates one through the real account repository
 * rather than inventing an arbitrary username with nothing behind it.
 */
@SpringBootTest
@Import(com.example.backend.ContainerTestConfiguration.class)
@Transactional
class UserCounterServiceTests {

    @Autowired
    private UserCounterService service;

    @Autowired
    private AccountRepository accounts;

    @Test
    void getsExistingCountWithoutChangingIt() {
        service.increment("test-user");
        service.increment("test-user");

        assertThat(service.getCount("test-user")).isEqualTo(2);
        assertThat(service.getCount("test-user")).isEqualTo(2);
    }

    @Test
    void getsZeroWhenUserHasNoCounterYet() {
        assertThat(service.getCount("test-user")).isZero();
    }

    @Test
    void maintainsIndependentCountsForEachUser() {
        assertThat(service.increment("test-user")).isEqualTo(1);
        assertThat(service.increment("test-user")).isEqualTo(2);
        assertThat(service.increment("test-admin")).isEqualTo(1);
    }

    @Test
    void resetSetsExistingCountToZero() {
        service.increment("test-user");
        service.increment("test-user");

        assertThat(service.reset("test-user")).isZero();
        assertThat(service.increment("test-user")).isEqualTo(1);
    }

    @Test
    void resetCreatesAZeroCountWhenUserHasNoCounterYet() {
        assertThat(service.reset("test-user")).isZero();
        assertThat(service.increment("test-user")).isEqualTo(1);
    }

    /**
     * The counter is keyed by the account's stable id. Renaming the account
     * (directly against the fixture, as no rename API exists yet) must not
     * disconnect the tally from it.
     */
    @Test
    void survivesAUsernameChangeMadeDirectlyAgainstTheFixture() {
        Account created = accounts.save(new Account("original-name", "hash", AccountRole.USER));
        service.increment(created.username());
        service.increment(created.username());

        Account renamed = new Account(
                created.id(),
                "renamed",
                created.passwordHash(),
                created.role(),
                created.failedLoginAttempts(),
                created.lockedAt(),
                created.enabled(),
                created.createdAt());
        accounts.save(renamed);

        assertThat(service.getCount("renamed")).isEqualTo(2);
        assertThat(service.increment("renamed")).isEqualTo(3);
    }
}
