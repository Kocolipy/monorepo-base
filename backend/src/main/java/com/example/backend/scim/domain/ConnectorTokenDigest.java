package com.example.backend.scim.domain;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * The stored form of a connector token: a SHA-256 digest of the complete
 * presented value, and the constant-time comparison that is the only thing
 * anything does with it.
 *
 * <p>A value type rather than a {@code byte[]} field on the token record, for
 * three properties a raw array cannot have. It compares in constant time, so a
 * presented value cannot be recovered a byte at a time from how long a rejection
 * took. It cannot be printed: {@link #toString()} names the type and nothing
 * else, so a digest cannot reach a log line through a record's generated
 * {@code toString}. And it is a distinct type, which is what lets
 * {@code ArchitectureTest} state that no web adapter depends on it — a
 * {@code byte[]} is unnameable in a rule.
 *
 * <p><strong>Unstretched SHA-256 is correct here and only here.</strong> A
 * connector token is 256 bits of {@link java.security.SecureRandom} material, so
 * there is no dictionary to walk and no work factor to buy; the digest exists so
 * a database leak does not hand over usable credentials. A password is the
 * opposite case in every respect and goes through Argon2id — see
 * {@code SecurityConfig.passwordEncoder}. Nothing here is a precedent for that.
 */
public final class ConnectorTokenDigest {

    private static final String ALGORITHM = "SHA-256";

    private final byte[] digest;

    private ConnectorTokenDigest(byte[] digest) {
        this.digest = digest;
    }

    /** Digests the complete presented bearer value, never the secret half alone. */
    public static ConnectorTokenDigest of(String presentedValue) {
        return new ConnectorTokenDigest(
                digest().digest(presentedValue.getBytes(StandardCharsets.UTF_8)));
    }

    /** Reconstitutes a digest read back out of storage. */
    public static ConnectorTokenDigest ofStoredBytes(byte[] stored) {
        return new ConnectorTokenDigest(stored.clone());
    }

    /**
     * Whether the other digest is this one, compared in constant time.
     *
     * <p>{@link MessageDigest#isEqual} rather than {@link Arrays#equals}: the
     * latter returns at the first differing byte, which is a timing oracle over
     * the digest of a value an attacker chose.
     */
    public boolean matches(ConnectorTokenDigest other) {
        return MessageDigest.isEqual(digest, other.digest);
    }

    /** The bytes to store. A copy, so the stored digest cannot be mutated in place. */
    public byte[] toStoredBytes() {
        return digest.clone();
    }

    /**
     * The type name and nothing else.
     *
     * <p>A digest is not a secret in the sense a token value is, but it is the one
     * field of a token record that a generated {@code toString} would render as
     * hex into whatever consumed it. Refusing to render it means a token record
     * can be interpolated into a message — which the logging rules forbid anyway —
     * without that being how a hash reaches a log store.
     */
    @Override
    public String toString() {
        return "ConnectorTokenDigest[redacted]";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ConnectorTokenDigest digestValue && matches(digestValue);
    }

    /**
     * Zero for every digest, so equal digests hash equally while nothing about the
     * bytes is exposed through a hash code. Token records are never used as map
     * keys — they are found by lookup id — so there is no collision cost to pay.
     */
    @Override
    public int hashCode() {
        return 0;
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException unavailable) {
            // SHA-256 is required of every Java platform implementation, so this
            // is a broken JRE rather than a condition a caller could handle.
            throw new IllegalStateException(ALGORITHM + " is unavailable", unavailable);
        }
    }
}
