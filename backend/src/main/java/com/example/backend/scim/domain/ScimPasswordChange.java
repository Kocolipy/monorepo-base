package com.example.backend.scim.domain;

/**
 * What a write does to a User's credential: nothing, set it, or clear it.
 *
 * <p>Three states rather than a nullable string, because "the write did not mention the
 * password" and "the write removed the password" are different requests with different effects
 * — a PUT that omits {@code password} leaves the credential usable (a write-only secret cannot be
 * round-tripped, so omission cannot mean removal), while a PATCH {@code remove} of
 * {@code password} makes the User credentialless. A nullable string could not tell them apart.
 *
 * <p>{@link #toString()} is overridden: a record's generated one would print the plaintext, and
 * this value is exactly the kind of thing that reaches a log line through a debugger's
 * string-concatenation or an exception message.
 *
 * @param kind      what the write does to the credential
 * @param plaintext the submitted password for {@link Kind#SET}, otherwise {@code null}
 */
public record ScimPasswordChange(Kind kind, String plaintext) {

    /** What the write does to the credential. */
    public enum Kind {
        /** The write did not mention the password; the stored credential is kept. */
        UNCHANGED,
        /** The write supplied a new password. */
        SET,
        /** The write removed the password; the User becomes credentialless. */
        CLEAR
    }

    /** The write did not touch the credential. */
    public static final ScimPasswordChange UNCHANGED = new ScimPasswordChange(Kind.UNCHANGED, null);

    /** The write removed the credential. */
    public static final ScimPasswordChange CLEAR = new ScimPasswordChange(Kind.CLEAR, null);

    public ScimPasswordChange {
        if (kind == null) {
            throw new IllegalArgumentException("a password change has a kind");
        }
        if ((kind == Kind.SET) != (plaintext != null)) {
            throw new IllegalArgumentException("only a SET password change carries a password");
        }
    }

    /** The write supplied this password. */
    public static ScimPasswordChange set(String plaintext) {
        return new ScimPasswordChange(Kind.SET, plaintext);
    }

    /** Redacted: the plaintext never appears. */
    @Override
    public String toString() {
        return "ScimPasswordChange[" + kind + "]";
    }
}
