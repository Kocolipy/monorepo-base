package com.example.backend.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.auth.InMemoryAccountRepository;
import com.example.backend.auth.MutableClock;
import com.example.backend.auth.domain.Account;
import com.example.backend.auth.domain.AccountRole;
import com.example.backend.auth.domain.LockoutPolicy;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginAttemptServiceTests {

    private static final Instant NOW = Instant.parse("2026-09-24T07:00:00Z");

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final MutableClock clock = new MutableClock(NOW);

    private LoginAttemptService attempts;

    @BeforeEach
    void setUp() {
        attempts = new LoginAttemptService(
                accounts, new LockoutPolicy(3, Duration.ofMinutes(5)), clock);
        accounts.save(new Account("ada", "hash", AccountRole.USER));
    }

    @Test
    void aRefusedAttemptIsCountedAgainstTheAccount() {
        attempts.recordFailure("ada");

        assertThat(accounts.require("ada").failedLoginAttempts()).isEqualTo(1);
        assertThat(accounts.require("ada").isLocked(NOW)).isFalse();
    }

    @Test
    void theThirdConsecutiveRefusalLocksTheAccount() {
        attempts.recordFailure("ada");
        attempts.recordFailure("ada");
        attempts.recordFailure("ada");

        Account locked = accounts.require("ada");
        assertThat(locked.isLocked(NOW)).isTrue();
        assertThat(locked.lockedUntil()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
    }

    @Test
    void anAcceptedLoginResetsTheFailureCount() {
        attempts.recordFailure("ada");
        attempts.recordFailure("ada");

        attempts.recordSuccess("ada");

        assertThat(accounts.require("ada").failedLoginAttempts()).isZero();
        assertThat(accounts.require("ada").lockedUntil()).isNull();
    }

    /**
     * Recording nothing for a name that does not exist is what keeps a refusal
     * uninformative: no row appears, so stored state cannot be used to enumerate
     * accounts.
     */
    @Test
    void aRefusalForAnUnknownUsernameIsNotRecordedAnywhere() {
        attempts.recordFailure("nobody");

        assertThat(accounts.findByUsername("nobody")).isEmpty();
        assertThat(accounts.require("ada").failedLoginAttempts()).isZero();
    }

    @Test
    void anAcceptedLoginForAnUnknownUsernameIsANoOp() {
        attempts.recordSuccess("nobody");

        assertThat(accounts.findByUsername("nobody")).isEmpty();
    }

    /**
     * An account with no failure run has nothing to clear, so the login must not
     * write to it — every accepted login would otherwise cost a pointless update.
     */
    @Test
    void anAcceptedLoginOnAnUntouchedAccountWritesNothing() {
        int savesBefore = accounts.saves();

        attempts.recordSuccess("ada");

        assertThat(accounts.saves()).isEqualTo(savesBefore);
    }

    @Test
    void anAcceptedLoginAfterAFailureDoesWrite() {
        attempts.recordFailure("ada");
        int savesBefore = accounts.saves();

        attempts.recordSuccess("ada");

        assertThat(accounts.saves()).isEqualTo(savesBefore + 1);
    }

    @Test
    void theClockDecidesWhenTheLockoutIsOver() {
        attempts.recordFailure("ada");
        attempts.recordFailure("ada");
        attempts.recordFailure("ada");
        assertThat(accounts.require("ada").isLocked(clock.instant())).isTrue();

        clock.advanceBy(Duration.ofMinutes(5));

        assertThat(accounts.require("ada").isLocked(clock.instant())).isFalse();
    }
}
