package com.example.backend.scim.domain;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

/**
 * Where a connector token's opaque value comes from, and the only place one is
 * taken apart.
 *
 * <p>The value is {@code <lookupId>.<secret>} — a non-secret 128-bit handle, a
 * dot, and 256 bits of {@link SecureRandom} material. One opaque string to the
 * connector, which never needs to know it has structure; two halves to this
 * service, because the halves answer different questions. The lookup id is what
 * a row is FOUND by, so it can be indexed and compared as an ordinary string.
 * The secret is what makes the value unguessable, and it is never stored, never
 * logged and never compared directly — only the digest of the whole value is,
 * through {@link ConnectorTokenDigest}.
 *
 * <p>Why not look the row up by digest and drop the lookup id: it would work, and
 * it would make every presented value a query key. A crafted value would then
 * probe the index directly, and a failed lookup's cost would vary with the digest
 * space. Splitting the value keeps the secret half out of every {@code WHERE}
 * clause.
 *
 * <p>Base64 URL-safe without padding, so a value survives a header, a query
 * string (which this service refuses to read one from) and a shell transcript
 * unchanged, and carries no {@code =} for a careless consumer to strip.
 */
public final class ConnectorTokenSecret {

    /** 256 bits, as the credential policy requires of a connector token. */
    private static final int SECRET_BYTES = 32;

    /** 128 bits of handle: enough that a lookup id never collides in practice. */
    private static final int LOOKUP_BYTES = 16;

    private static final char SEPARATOR = '.';

    /**
     * A freshly minted token: the value to hand the Admin exactly once, and the
     * two things that get stored.
     *
     * @param lookupId       the non-secret handle the row is found by
     * @param presentedValue the complete opaque value — the only place plaintext
     *                       exists, and it exists for one response
     * @param digest         what is persisted in the token's place
     */
    public record Minted(String lookupId, String presentedValue, ConnectorTokenDigest digest) {
    }

    /** A value a connector presented, split far enough to look the row up. */
    public record Presented(String lookupId, ConnectorTokenDigest digest) {
    }

    private ConnectorTokenSecret() {
    }

    /** Mints a new token value. */
    public static Minted mint(SecureRandom random) {
        String lookupId = randomUrlSafe(random, LOOKUP_BYTES);
        String secret = randomUrlSafe(random, SECRET_BYTES);
        String presentedValue = lookupId + SEPARATOR + secret;
        return new Minted(lookupId, presentedValue, ConnectorTokenDigest.of(presentedValue));
    }

    /**
     * Splits a presented value, or reports that it is not one of this service's
     * tokens at all.
     *
     * <p>Empty for a value with no separator, an empty half on either side, or more
     * than one separator. A malformed value is refused here rather than being sent
     * to the database as a lookup id: a caller's arbitrary string is not a query
     * key, and the refusal is the same {@code invalid_token} a wrong secret gets,
     * so nothing distinguishes "not a token" from "not your token".
     */
    public static Optional<Presented> parse(String presentedValue) {
        if (presentedValue == null) {
            return Optional.empty();
        }
        int separator = presentedValue.indexOf(SEPARATOR);
        if (separator <= 0 || separator == presentedValue.length() - 1) {
            return Optional.empty();
        }
        if (presentedValue.indexOf(SEPARATOR, separator + 1) >= 0) {
            return Optional.empty();
        }
        return Optional.of(new Presented(
                presentedValue.substring(0, separator),
                ConnectorTokenDigest.of(presentedValue)));
    }

    private static String randomUrlSafe(SecureRandom random, int byteCount) {
        byte[] bytes = new byte[byteCount];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
