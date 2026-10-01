package com.example.backend.scim.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * A User's authentication state, and the transitions the login path drives it through.
 *
 * <p>These are the rules the deleted account aggregate's tests held, moved to where the state now
 * lives. The substantive one is that a lock has no duration: nothing here reads a clock to decide
 * whether a lock is in force, so no caller has to agree with the server about the time to agree
 * about the state.
 */
class ScimLoginStateTests {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private static final LockoutPolicy AFTER_THREE = new LockoutPolicy(3);

    /**
     * The dormancy basis survives every failure-run transition. A rejected login or an Unlock is
     * not an authentication, and a transition that dropped the basis would make a User who once
     * logged in look as if it never had.
     */
    @Test
    void every_failure_run_transition_carries_the_dormancy_basis_unchanged() {
        Instant last = NOW.minusSeconds(3_600);
        ScimLoginState running = new ScimLoginState("hash", 2, null, last);
        ScimLoginState locked = new ScimLoginState("hash", 3, NOW, last);

        assertThat(running.withFailureRecorded(AFTER_THREE, NOW).lastAuthenticatedAt())
                .as("the failure that locks").isEqualTo(last);
        assertThat(new ScimLoginState("hash", 0, null, last)
                .withFailureRecorded(AFTER_THREE, NOW).lastAuthenticatedAt())
                .as("a failure that does not lock").isEqualTo(last);
        assertThat(running.withFailureCounted().lastAuthenticatedAt()).isEqualTo(last);
        assertThat(locked.withFailureRunCleared().lastAuthenticatedAt()).isEqualTo(last);
    }

    /**
     * The change-required flag survives every failure-run transition: a rejected login, a
     * counted failure and an Unlock's clearing all leave it standing. Only a completed change
     * clears it, and that is a port operation, not a transition here.
     */
    @Test
    void every_failure_run_transition_carries_the_change_required_flag_unchanged() {
        ScimLoginState running = new ScimLoginState("hash", 2, null, null, NOW);
        ScimLoginState locked = new ScimLoginState("hash", 3, NOW, null, NOW);

        assertThat(running.withFailureRecorded(AFTER_THREE, NOW).passwordChangeRequiredSince())
                .as("the failure that locks").isEqualTo(NOW);
        assertThat(new ScimLoginState("hash", 0, null, null, NOW)
                .withFailureRecorded(AFTER_THREE, NOW).passwordChangeRequiredSince())
                .as("a failure that does not lock").isEqualTo(NOW);
        assertThat(running.withFailureCounted().passwordChangeRequiredSince()).isEqualTo(NOW);
        assertThat(locked.withFailureRunCleared().passwordChangeRequiredSince()).isEqualTo(NOW);
    }

    @Test
    void requiring_a_change_dates_the_flag_and_keeps_everything_else() {
        Instant last = NOW.minusSeconds(60);
        ScimLoginState state = new ScimLoginState("hash", 2, NOW, last);
        Instant later = NOW.plusSeconds(10);

        ScimLoginState flagged = state.withPasswordChangeRequired(later);

        assertThat(state.isPasswordChangeRequired()).isFalse();
        assertThat(flagged).isEqualTo(new ScimLoginState("hash", 2, NOW, last, later));
        assertThat(flagged.isPasswordChangeRequired()).isTrue();
        assertThat(flagged.withPasswordChangeRequired(later.plusSeconds(5))
                .passwordChangeRequiredSince())
                .as("a newly imposed credential re-dates the flag")
                .isEqualTo(later.plusSeconds(5));
        assertThatThrownBy(() -> state.withPasswordChangeRequired(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void a_state_built_without_an_authentication_has_none() {
        assertThat(new ScimLoginState("hash", 0, null).lastAuthenticatedAt()).isNull();
        assertThat(ScimLoginState.of("hash").lastAuthenticatedAt()).isNull();
        assertThat(ScimLoginState.CREDENTIALLESS.lastAuthenticatedAt()).isNull();
    }

    @Test
    void a_negative_failure_run_is_refused() {
        assertThatThrownBy(() -> new ScimLoginState("hash", -1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be negative");
    }

    @Test
    void a_credentialless_state_has_no_password_and_no_history() {
        assertThat(ScimLoginState.CREDENTIALLESS.hasPassword()).isFalse();
        assertThat(ScimLoginState.CREDENTIALLESS.failedLoginAttempts()).isZero();
        assertThat(ScimLoginState.CREDENTIALLESS.isLocked()).isFalse();
    }

    @Test
    void a_null_hash_yields_the_credentialless_state_rather_than_a_state_holding_null() {
        assertThat(ScimLoginState.of(null)).isSameAs(ScimLoginState.CREDENTIALLESS);
    }

    @Test
    void a_state_built_from_a_hash_has_a_password_and_no_history() {
        ScimLoginState state = ScimLoginState.of("hash");

        assertThat(state.hasPassword()).isTrue();
        assertThat(state.passwordHash()).isEqualTo("hash");
        assertThat(state.failedLoginAttempts()).isZero();
        assertThat(state.isLocked()).isFalse();
    }

    @Test
    void a_recorded_failure_below_the_limit_counts_but_does_not_lock() {
        ScimLoginState after = ScimLoginState.of("hash").withFailureRecorded(AFTER_THREE, NOW);

        assertThat(after.failedLoginAttempts()).isEqualTo(1);
        assertThat(after.isLocked()).isFalse();
        assertThat(after.lockedAt()).isNull();
    }

    @Test
    void reaching_the_limit_locks_as_of_the_instant_given() {
        ScimLoginState after = new ScimLoginState("hash", 2, null)
                .withFailureRecorded(AFTER_THREE, NOW);

        assertThat(after.failedLoginAttempts()).isEqualTo(3);
        assertThat(after.isLocked()).isTrue();
        assertThat(after.lockedAt()).isEqualTo(NOW);
    }

    @Test
    void a_locked_state_is_returned_unchanged_so_attempts_neither_count_nor_deepen_the_lock() {
        ScimLoginState locked = new ScimLoginState("hash", 3, NOW);

        ScimLoginState after = locked.withFailureRecorded(AFTER_THREE, NOW.plusSeconds(600));

        assertThat(after).isSameAs(locked);
        assertThat(after.failedLoginAttempts()).isEqualTo(3);
        assertThat(after.lockedAt()).isEqualTo(NOW);
    }

    /**
     * The whole of "a lockout has no duration": the state is read from the recorded instant's
     * PRESENCE, so an arbitrarily old lock is still a lock. A rule with an expiry would make this
     * assertion fail as written, which is what makes it worth writing.
     */
    @Test
    void a_lock_does_not_expire_however_long_ago_it_was_imposed() {
        ScimLoginState ancient = new ScimLoginState(
                "hash", 3, Instant.parse("1999-01-01T00:00:00Z"));

        assertThat(ancient.isLocked()).isTrue();
    }

    @Test
    void a_counted_failure_lengthens_the_run_and_never_locks_whatever_the_limit_says() {
        ScimLoginState after = new ScimLoginState("hash", 99, null).withFailureCounted();

        assertThat(after.failedLoginAttempts()).isEqualTo(100);
        assertThat(after.isLocked()).isFalse();
    }

    @Test
    void a_counted_failure_leaves_an_existing_lock_exactly_as_it_stood() {
        ScimLoginState after = new ScimLoginState("hash", 3, NOW).withFailureCounted();

        assertThat(after.lockedAt()).isEqualTo(NOW);
        assertThat(after.failedLoginAttempts()).isEqualTo(4);
    }

    @Test
    void clearing_the_run_drops_both_the_count_and_the_lock() {
        ScimLoginState after = new ScimLoginState("hash", 3, NOW).withFailureRunCleared();

        assertThat(after.failedLoginAttempts()).isZero();
        assertThat(after.isLocked()).isFalse();
        assertThat(after.passwordHash()).isEqualTo("hash");
    }

    /**
     * Returned by identity so a caller can skip a pointless write. That is the only observable
     * difference between skipping the write and performing a redundant one, which is why the
     * assertion is on identity rather than on equality.
     */
    @Test
    void clearing_a_state_with_nothing_to_clear_returns_the_same_value() {
        ScimLoginState clean = ScimLoginState.of("hash");

        assertThat(clean.withFailureRunCleared()).isSameAs(clean);
    }

    @Test
    void clearing_preserves_a_credentialless_state_rather_than_inventing_a_hash() {
        ScimLoginState after = new ScimLoginState(null, 2, null).withFailureRunCleared();

        assertThat(after.hasPassword()).isFalse();
        assertThat(after.passwordHash()).isNull();
    }
}
