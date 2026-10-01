package com.example.backend.scim.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The {@code If-Match} contract: absent is an unconditional write that any version satisfies,
 * anything but one entity tag is {@code 400}, a well-formed tag that is not the current version is
 * {@code 412}, and only the current version's exact tag lets a conditional write through.
 */
class ScimVersionPreconditionTests {

    private static final long CURRENT = 7L;

    /** RFC 7644 §3.14 makes {@code If-Match} optional: a write without one is unconditional. */
    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, CURRENT, Long.MAX_VALUE})
    void no_header_at_all_is_unconditional_and_satisfied_by_any_version(long version) {
        for (ScimVersionPrecondition absent : List.of(
                ScimVersionPrecondition.ofIfMatch(List.of()),
                ScimVersionPrecondition.ofIfMatch(null))) {
            assertThat(absent.isConditional()).isFalse();
            assertThatCode(() -> absent.requireSatisfiedBy(version)).doesNotThrowAnyException();
        }
    }

    /** Any header value at all — even one that is then refused — makes the write conditional. */
    @ParameterizedTest
    @ValueSource(strings = {"\"7\"", "*", ""})
    void any_header_value_makes_the_write_conditional(String header) {
        assertThat(ScimVersionPrecondition.ofIfMatch(List.of(header)).isConditional()).isTrue();
    }

    /** A wildcard would switch the check off, so it is refused rather than honoured. */
    @ParameterizedTest
    @ValueSource(strings = {"*", "7", "\"7", "7\"", "W/7", "", "\"7\" junk", "\"a\"b\""})
    void anything_that_is_not_one_entity_tag_is_invalid(String header) {
        assertThatThrownBy(() -> ScimVersionPrecondition.ofIfMatch(List.of(header))
                        .requireSatisfiedBy(CURRENT))
                .isInstanceOf(InvalidPreconditionException.class);
    }

    /**
     * A list is "any of these", which is not an exact match — even when the current version is
     * one of them. Spring delivers a comma-separated header as several values, so this is also
     * what {@code If-Match: "6", "7"} arrives as.
     */
    @Test
    void more_than_one_tag_is_invalid_even_when_one_of_them_is_current() {
        assertThatThrownBy(() -> ScimVersionPrecondition.ofIfMatch(List.of("\"6\"", "\"7\""))
                        .requireSatisfiedBy(CURRENT))
                .isInstanceOf(InvalidPreconditionException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"6\"", "\"8\"", "\"07\"", "\"\"", "\"abc\""})
    void a_well_formed_tag_that_is_not_the_current_version_fails(String header) {
        assertThatThrownBy(() -> ScimVersionPrecondition.ofIfMatch(List.of(header))
                        .requireSatisfiedBy(CURRENT))
                .isInstanceOf(PreconditionFailedException.class);
    }

    /** {@code If-Match} compares strongly, so a weak tag never matches — not even the current one. */
    @Test
    void a_weak_tag_for_the_current_version_fails_because_the_comparison_is_strong() {
        assertThatThrownBy(() -> ScimVersionPrecondition.ofIfMatch(List.of("W/\"7\""))
                        .requireSatisfiedBy(CURRENT))
                .isInstanceOf(PreconditionFailedException.class);
    }

    @Test
    void the_current_versions_exact_tag_is_satisfied_with_surrounding_whitespace_ignored() {
        assertThatCode(() -> ScimVersionPrecondition.ofIfMatch(List.of("\"7\""))
                        .requireSatisfiedBy(CURRENT))
                .doesNotThrowAnyException();
        assertThatCode(() -> ScimVersionPrecondition.ofIfMatch(List.of("  \"7\" "))
                        .requireSatisfiedBy(CURRENT))
                .doesNotThrowAnyException();
    }
}
