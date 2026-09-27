package com.example.backend.scim.domain;

import java.text.Normalizer;
import java.util.Locale;

/**
 * A Group's {@code displayName} reduced to the form uniqueness is decided on.
 *
 * <p>A type rather than a static method, for the reason {@link NormalizedUserName}
 * is one: the normalized form and the form the connector sent are both real and
 * must not be confused — one is rendered back, the other is compared — and a
 * {@code String} parameter would be assignable from either.
 *
 * <p>The normalization is the same mapping {@link NormalizedUserName} applies:
 * Unicode NFKC, then case folding with {@link Locale#ROOT}. Stated as its own type
 * rather than shared with the userName form because the two decide different
 * uniqueness scopes, and a single type would let a Group's normalized name be
 * compared against a User's without a compiler error.
 *
 * <p>RFC 7643 does not require {@code displayName} to be unique. This directory
 * requires it anyway, and the reason is authority: a Group's membership confers
 * application authorization, so two Groups an administrator reads as the same name
 * is how membership of the wrong one gets granted. Case-insensitively, so the two
 * cannot differ only in case.
 */
public record NormalizedDisplayName(String value) {

    public NormalizedDisplayName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("a displayName normalizes to nothing");
        }
    }

    /**
     * The normalized form of a submitted {@code displayName}.
     *
     * @throws IllegalArgumentException when the submitted value is null, empty or
     *                                  whitespace only — a Group with no
     *                                  {@code displayName} is refused before this
     *                                  point, so reaching it with one is a defect
     */
    public static NormalizedDisplayName of(String submitted) {
        if (submitted == null || submitted.isBlank()) {
            throw new IllegalArgumentException("a displayName normalizes to nothing");
        }
        String composed = Normalizer.normalize(submitted, Normalizer.Form.NFKC);
        return new NormalizedDisplayName(composed.toLowerCase(Locale.ROOT));
    }
}
