package com.example.backend.scim.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The {@code If-Match} contract: absent is {@code 428}, anything but one entity tag is
 * {@code 400}, a well-formed tag that is not the current version is {@code 412}, and only the
 * current version's exact tag lets the write through.
 */
class ScimVersionPreconditionTests {

    private static final long CURRENT = 7L;

    @Test
    void no_header_at_all_is_a_required_precondition() {
        assertThatThrownBy(() -> ScimVersionPrecondition.ofIfMatch(List.of())
                        .requireSatisfiedBy(CURRENT))
                .isInstanceOf(PreconditionRequiredException.class);
        assertThatThrownBy(() -> ScimVersionPrecondition.ofIfMatch(null)
                        .requireSatisfiedBy(CURRENT))
                .isInstanceOf(PreconditionRequiredException.class);
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
