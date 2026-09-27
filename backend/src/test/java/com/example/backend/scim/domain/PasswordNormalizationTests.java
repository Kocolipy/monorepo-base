package com.example.backend.scim.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** The OpaqueString mapping a password is hashed and compared in. */
class PasswordNormalizationTests {

    @Test
    void a_decomposed_spelling_normalizes_to_the_composed_one() {
        String composed = "caf\u00e9-correct-horse";
        String decomposed = "cafe\u0301-correct-horse";

        assertThat(decomposed).isNotEqualTo(composed);
        assertThat(PasswordNormalization.normalize(decomposed)).isEqualTo(composed);
        assertThat(PasswordNormalization.normalize(composed)).isEqualTo(composed);
    }

    @Test
    void every_non_ascii_space_becomes_an_ascii_space() {
        assertThat(PasswordNormalization.normalize("a\u00a0b\u2003c\u3000d")).isEqualTo("a b c d");
    }

    /**
     * Nothing else is touched: case, symbols, ordinary spaces and non-space characters survive, so
     * normalization never collapses two genuinely different passwords into one.
     */
    @Test
    void case_symbols_and_ordinary_characters_are_preserved() {
        String password = "Tr0ub4dor & 3  Horse!\u00df\u4e2d";

        assertThat(PasswordNormalization.normalize(password)).isEqualTo(password);
    }
}
