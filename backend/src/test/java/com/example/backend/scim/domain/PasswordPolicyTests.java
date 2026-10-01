package com.example.backend.scim.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** The password policy's own rules. */
class PasswordPolicyTests {

    @Test
    void aPasswordAtTheMinimumPasses() {
        assertThat(PasswordPolicy.violation("x".repeat(PasswordPolicy.MIN_LENGTH), "ada")).isEmpty();
    }

    @Test
    void oneShortOfTheMinimumIsTooShort() {
        assertThat(PasswordPolicy.violation("x".repeat(PasswordPolicy.MIN_LENGTH - 1), "ada"))
                .contains(PasswordPolicy.Rule.TOO_SHORT);
    }

    @Test
    void aPasswordAtTheMaximumPassesAndOneMoreIsTooLong() {
        assertThat(PasswordPolicy.violation("x".repeat(PasswordPolicy.MAX_LENGTH), "ada")).isEmpty();
        assertThat(PasswordPolicy.violation("x".repeat(PasswordPolicy.MAX_LENGTH + 1), "ada"))
                .contains(PasswordPolicy.Rule.TOO_LONG);
    }

    /** One code point is one character: twelve emoji are twelve characters, not twenty-four. */
    @Test
    void lengthIsCountedInCodePoints() {
        String twelveEmoji = "\uD83D\uDE00".repeat(PasswordPolicy.MIN_LENGTH);
        assertThat(PasswordPolicy.violation(twelveEmoji, "ada")).isEmpty();
        assertThat(PasswordPolicy.violation("\uD83D\uDE00".repeat(PasswordPolicy.MIN_LENGTH - 1), "ada"))
                .contains(PasswordPolicy.Rule.TOO_SHORT);
    }

    /** Normalized first: a decomposed é counts as the one character it will be stored as. */
    @Test
    void lengthIsCountedOnTheNormalizedForm() {
        String decomposed = "e\u0301".repeat(PasswordPolicy.MIN_LENGTH - 1) + "x";
        assertThat(PasswordPolicy.violation(decomposed, "ada")).isEmpty();
        String elevenComposed = "e\u0301".repeat(PasswordPolicy.MIN_LENGTH - 1);
        assertThat(PasswordPolicy.violation(elevenComposed, "ada"))
                .contains(PasswordPolicy.Rule.TOO_SHORT);
    }

    @Test
    void aPasswordContainingTheUserNameCaseInsensitivelyIsRefused() {
        assertThat(PasswordPolicy.violation("prefix-GrAcE-suffix", "grace"))
                .contains(PasswordPolicy.Rule.CONTAINS_USER_NAME);
        assertThat(PasswordPolicy.violation("grace-hopper-1906", "Grace-Hopper"))
                .contains(PasswordPolicy.Rule.CONTAINS_USER_NAME);
        assertThat(PasswordPolicy.violation("an-unrelated-passphrase", "grace")).isEmpty();
    }

    @Test
    void everyRuleMessageNamesTheRuleWithoutAPlaceForAValue() {
        for (PasswordPolicy.Rule rule : PasswordPolicy.Rule.values()) {
            assertThat(rule.message()).startsWith("The new password must");
        }
        assertThat(PasswordPolicy.Rule.TOO_SHORT.message()).contains("12");
        assertThat(PasswordPolicy.Rule.TOO_LONG.message()).contains("256");
        assertThat(PasswordPolicy.Rule.REUSED.message()).contains("3");
    }
}
