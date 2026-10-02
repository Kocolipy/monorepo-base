package com.example.backend.scim.domain;

/**
 * Port to the deployment's password encoder: the one thing that can turn a password into its
 * stored form and compare a candidate with a stored salted hash.
 *
 * <p>The domain names the two capabilities and nothing else, so {@link PasswordAcceptance} stays
 * framework-free; the adapter normalizes before hashing and before comparing, which is what makes
 * a differently-composed Unicode spelling match the hash of the same password.
 */
public interface PasswordHasher {

    /** The stored form of {@code password}: a fresh salted hash. */
    String hash(String password);

    /** Whether {@code candidate} is the password {@code storedHash} was made from. */
    boolean matches(String candidate, String storedHash);
}
