package com.example.backend.scim.domain;

/**
 * How many of a User's passwords are remembered to refuse their reuse.
 *
 * <p>Three, the current one included: the specification plan's password policy refuses a
 * password matching "any of the User's last 3 passwords". The history is trimmed to this many
 * on every successful change, so it never holds more than the rule needs to consult.
 */
public final class PasswordHistoryPolicy {

    /** The number of most recent password hashes a User's history retains. */
    public static final int RETAINED = 3;

    private PasswordHistoryPolicy() {
    }
}
