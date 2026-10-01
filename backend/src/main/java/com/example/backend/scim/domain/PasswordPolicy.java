package com.example.backend.scim.domain;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * The rules a new password must satisfy on every path that sets one — SCIM {@code password} on
 * create, replace and PATCH, and the self-service change.
 *
 * <p>Reuse needs the User's history and an encoder, which this class does not hold, so a caller
 * supplies it as a predicate to {@link #violation(String, String, Predicate)}. That method is the
 * one place the ORDER is decided: the intrinsic rules first, in the order of {@link Rule}, and
 * reuse only for a candidate that passed them — so every path names the same rule for the same
 * candidate, and a sub-policy value is never hashed-and-compared against the history at all.
 *
 * <p>Each rule is named by a closed-set member so a refusal can say WHICH rule was unmet without
 * echoing the value that failed it. The candidate is normalized first
 * ({@link PasswordNormalization}), so a length is counted in code points of the form that would be
 * stored, and one code point is one character.
 *
 * <p>The specification plan's policy also names a deployment-configurable blocklist. It is not
 * here: no corpus is shipped yet, and a rule with nothing to check against would be a rule that
 * always passes.
 */
public final class PasswordPolicy {

    /** Fewest characters a password may have. */
    public static final int MIN_LENGTH = 12;

    /** Most characters a password may have. */
    public static final int MAX_LENGTH = 256;

    /** A rule a candidate password failed. */
    public enum Rule {
        /** Fewer than {@link #MIN_LENGTH} characters. */
        TOO_SHORT("The new password must be at least " + MIN_LENGTH + " characters long"),
        /** More than {@link #MAX_LENGTH} characters. */
        TOO_LONG("The new password must be at most " + MAX_LENGTH + " characters long"),
        /** Equals or contains the User's {@code userName}, case-insensitively. */
        CONTAINS_USER_NAME("The new password must not contain the user name"),
        /** Matches the current password or one of the retained previous ones. */
        REUSED("The new password must differ from the current password and the "
                + PasswordHistoryPolicy.RETAINED + " most recent ones");

        private final String message;

        Rule(String message) {
            this.message = message;
        }

        /** What a caller is told: the rule, never the value. */
        public String message() {
            return message;
        }
    }

    private PasswordPolicy() {
    }

    /** The first rule {@code candidate} fails for this {@code userName}, or empty when it passes. */
    public static Optional<Rule> violation(String candidate, String userName) {
        String normalized = PasswordNormalization.normalize(candidate);
        int length = normalized.codePointCount(0, normalized.length());
        if (length < MIN_LENGTH) {
            return Optional.of(Rule.TOO_SHORT);
        }
        if (length > MAX_LENGTH) {
            return Optional.of(Rule.TOO_LONG);
        }
        if (normalized.toLowerCase(Locale.ROOT).contains(userName.toLowerCase(Locale.ROOT))) {
            return Optional.of(Rule.CONTAINS_USER_NAME);
        }
        return Optional.empty();
    }

    /**
     * The first rule {@code candidate} fails, reuse included: the intrinsic rules of
     * {@link #violation(String, String)} first, then {@code reused}, which is consulted only when
     * they all pass.
     *
     * @param userName the {@code userName} the User will hold once the password is set
     * @param reused   whether the candidate matches the current credential or a retained one
     */
    public static Optional<Rule> violation(
            String candidate, String userName, Predicate<String> reused) {
        Optional<Rule> intrinsic = violation(candidate, userName);
        if (intrinsic.isPresent()) {
            return intrinsic;
        }
        return reused.test(candidate) ? Optional.of(Rule.REUSED) : Optional.empty();
    }
}
