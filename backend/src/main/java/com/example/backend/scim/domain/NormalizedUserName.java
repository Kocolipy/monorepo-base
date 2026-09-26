package com.example.backend.scim.domain;

import java.text.Normalizer;
import java.util.Locale;

/**
 * A {@code userName} reduced to the form uniqueness is decided on.
 *
 * <p>A type rather than a static method, because the normalized form and the form
 * the connector sent are both real and must not be confused: one is rendered back
 * and the other is compared. A {@code String} parameter named
 * {@code normalizedUserName} would be assignable from the wrong one; this is not.
 *
 * <h2>What the normalization is</h2>
 *
 * <p>Unicode NFKC, then case folding through {@link String#toLowerCase(Locale)}
 * with {@link Locale#ROOT}. That is the mapping half of PRECIS
 * {@code UsernameCaseMapped} (RFC 8265): width-folding and compatibility
 * composition, so a full-width or decomposed spelling of a name cannot be
 * registered twice, plus case-insensitivity, which RFC 7643 requires of
 * {@code userName}.
 *
 * <p>{@link Locale#ROOT} and not the default locale: under a Turkish default,
 * {@code toLowerCase} maps {@code I} to a dotless {@code ı}, so the same submitted
 * name would normalize differently depending on the server's locale — and a
 * uniqueness rule that depends on the host's configuration is not a rule.
 *
 * <p><strong>What it is not:</strong> PRECIS's disallowed-code-point rules are not
 * applied. A username containing a control character or an unassigned code point is
 * normalized rather than refused. That check belongs with the rest of the
 * attribute's validation and is a deliberate omission here rather than an oversight;
 * it is recorded as an open item in the specification plan's query and write
 * contract work.
 */
public record NormalizedUserName(String value) {

    public NormalizedUserName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("a userName normalizes to nothing");
        }
    }

    /**
     * The normalized form of a submitted {@code userName}.
     *
     * @throws IllegalArgumentException when the submitted value is null, empty or
     *                                  whitespace only — a resource with no
     *                                  {@code userName} is refused before this
     *                                  point, so reaching it with one is a defect
     */
    public static NormalizedUserName of(String submitted) {
        if (submitted == null || submitted.isBlank()) {
            throw new IllegalArgumentException("a userName normalizes to nothing");
        }
        String composed = Normalizer.normalize(submitted, Normalizer.Form.NFKC);
        return new NormalizedUserName(composed.toLowerCase(Locale.ROOT));
    }
}
