package com.example.backend.scim.domain;

import java.text.Normalizer;

/**
 * The form a password is hashed and compared in: the mapping half of PRECIS's OpaqueString
 * profile (RFC 8265 §4.2).
 *
 * <p>Two rules, and only two. Every non-ASCII space (Unicode {@code Zs}) becomes U+0020, and the
 * result is put in Unicode Normalization Form C. So a password typed with a composed {@code é} and
 * the same password arriving decomposed ({@code e} + U+0301) are one password: they hash to the
 * same credential, they both log in, and one cannot slip past the history as a "new" value when it
 * is the old one spelled differently. Nothing is stripped, lower-cased or rejected here — case and
 * every printable character are significant in a password.
 *
 * <p>Applied inside the password encoder, so it cannot be applied on one path and forgotten on
 * another: setting a password through SCIM, logging in, and checking a candidate against the
 * history all pass through the same encoder.
 */
public final class PasswordNormalization {

    private PasswordNormalization() {
    }

    /** The normalized form of a submitted password. */
    public static String normalize(CharSequence password) {
        StringBuilder mapped = new StringBuilder(password.length());
        password.codePoints().forEach(codePoint -> mapped.appendCodePoint(
                codePoint != ' ' && Character.getType(codePoint) == Character.SPACE_SEPARATOR
                        ? ' '
                        : codePoint));
        return Normalizer.normalize(mapped, Normalizer.Form.NFC);
    }
}
