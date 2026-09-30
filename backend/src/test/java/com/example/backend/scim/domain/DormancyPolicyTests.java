package com.example.backend.scim.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * The two inactivity windows: what a deployment that configures nothing gets, what a configured
 * value becomes, and which values stop startup. The startup half — that a refusal here actually
 * fails the context — is {@code DormancyPolicyStartupTests}.
 */
class DormancyPolicyTests {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");

    @Test
    void anUnconfiguredPolicyDeactivatesAtNinetyDaysAndRevokesAuthorityAtOneHundredEighty() {
        DormancyPolicy policy = new DormancyPolicy(null, null);

        assertThat(policy.deactivationWindow()).isEqualTo(Duration.ofDays(90));
        assertThat(policy.authorityRevocationWindow()).isEqualTo(Duration.ofDays(180));
        assertThat(DormancyPolicy.defaults()).isEqualTo(policy);
    }

    /**
     * The constants are what the README and {@code .env.example} quote, so a change to either is
     * a change to the docs in the same commit.
     */
    @Test
    void theDocumentedDefaultsAreTheOnesTheRuleUses() {
        assertThat(DormancyPolicy.DEFAULT_DEACTIVATION_WINDOW).isEqualTo(Duration.ofDays(90));
        assertThat(DormancyPolicy.DEFAULT_AUTHORITY_REVOCATION_WINDOW)
                .isEqualTo(Duration.ofDays(180));
    }

    /** Asserted against literals: comparing two constructions would pass if both defaulted. */
    @Test
    void configuredWindowsAreKeptAsGivenAndIndependently() {
        DormancyPolicy onlyDeactivation = new DormancyPolicy(Duration.ofDays(30), null);
        assertThat(onlyDeactivation.deactivationWindow()).isEqualTo(Duration.ofDays(30));
        assertThat(onlyDeactivation.authorityRevocationWindow()).isEqualTo(Duration.ofDays(180));

        DormancyPolicy onlyAuthority = new DormancyPolicy(null, Duration.ofDays(45));
        assertThat(onlyAuthority.deactivationWindow()).isEqualTo(Duration.ofDays(90));
        assertThat(onlyAuthority.authorityRevocationWindow()).isEqualTo(Duration.ofDays(45));
    }

    @Test
    void eachCutoffIsNowMinusItsOwnWindow() {
        DormancyPolicy policy = new DormancyPolicy(Duration.ofDays(10), Duration.ofDays(20));

        assertThat(policy.deactivationCutoff(NOW)).isEqualTo(NOW.minus(Duration.ofDays(10)));
        assertThat(policy.authorityRevocationCutoff(NOW)).isEqualTo(NOW.minus(Duration.ofDays(20)));
    }

    /**
     * Zero or negative would make every User dormant on the next run, so it is refused, naming the
     * stage and the value. The smallest positive window is allowed — there is no floor.
     */
    @Test
    void aZeroOrNegativeWindowIsRefusedAndTheSmallestPositiveOneIsNot() {
        assertThatThrownBy(() -> new DormancyPolicy(Duration.ZERO, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deactivation")
                .hasMessageContaining(Duration.ZERO.toString());
        assertThatThrownBy(() -> new DormancyPolicy(null, Duration.ofDays(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("authority revocation")
                .hasMessageContaining(Duration.ofDays(-1).toString());
        assertThatThrownBy(() -> new DormancyPolicy(null, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(new DormancyPolicy(Duration.ofNanos(1), Duration.ofNanos(1))
                .deactivationWindow()).isEqualTo(Duration.ofNanos(1));
    }
}
