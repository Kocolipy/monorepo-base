package com.example.backend.scim.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Whether a new password is accepted for a User, and what is stored and remembered when it is: the
 * one owner of password acceptance for every path that sets one — SCIM {@code password} on create,
 * replace and PATCH, and the self-service change.
 *
 * <p>Acceptance is two steps, and the split is the point:
 *
 * <ol>
 *   <li>{@link #acceptFor} or {@link #acceptForNewUser} decides. The {@link PasswordPolicy}'s
 *       intrinsic rules come first, in its order; only a candidate that passes them is compared,
 *       one hash at a time through the {@link PasswordHasher}, with the User's current credential
 *       and its retained history. An accepted candidate is encoded once and handed back as an
 *       {@link Accepted}. Deciding writes nothing: a refused candidate, or an accepted one whose
 *       write is never applied, leaves the history exactly as it was.
 *   <li>{@link #remember} records the accepted hash as the User's newest password. The caller calls
 *       it after the credential is written, inside the same transaction, so a rollback takes the
 *       history entry with it; an {@link Accepted} can be remembered only once.
 * </ol>
 *
 * <p>Everything else a path does around a password — verifying the current one, refusing a
 * protected resource or a stale version, auditing, flagging a required change, ending sessions —
 * belongs to that path and stays there.
 */
public final class PasswordAcceptance {

    private final ScimPasswordHistoryRepository history;
    private final PasswordHasher hasher;

    public PasswordAcceptance(ScimPasswordHistoryRepository history, PasswordHasher hasher) {
        this.history = Objects.requireNonNull(history);
        this.hasher = Objects.requireNonNull(hasher);
    }

    /** What deciding on a candidate produced. */
    public sealed interface Decision permits Accepted, Refused {
    }

    /** The candidate broke {@code rule}; nothing was encoded and nothing was written. */
    public record Refused(PasswordPolicy.Rule rule) implements Decision {

        public Refused {
            Objects.requireNonNull(rule);
        }
    }

    /**
     * An accepted candidate in its stored form. Holds the hash, never the password, and does not
     * print the hash.
     */
    public static final class Accepted implements Decision {

        private final String passwordHash;
        private boolean remembered;

        private Accepted(String passwordHash) {
            this.passwordHash = passwordHash;
        }

        /** The encoded credential to store. */
        public String passwordHash() {
            return passwordHash;
        }

        @Override
        public String toString() {
            return "Accepted[passwordHash=<redacted>]";
        }
    }

    /**
     * Decides on the first password of a User being created. Such a User has no credential and no
     * history, so only the intrinsic rules apply and nothing is looked up.
     */
    public Decision acceptForNewUser(String candidate, String userName) {
        return decide(PasswordPolicy.violation(candidate, userName), candidate);
    }

    /**
     * Decides on a new password for an existing User: the intrinsic rules, then reuse of the
     * current credential or a retained one.
     *
     * <p>The current credential is compared explicitly as well as through the history: a User
     * whose credential predates the history — the seeded Bootstrap Admin — would otherwise be able
     * to "change" to the password it already has. A User without a credential is compared with its
     * history alone.
     *
     * @param resultingUserName the {@code userName} the User holds once the password is set, so a
     *                          write renaming the User is checked against the new name
     */
    public Decision acceptFor(ScimUser user, String candidate, String resultingUserName) {
        return decide(
                PasswordPolicy.violation(candidate, resultingUserName, reused -> isReused(user, reused)),
                candidate);
    }

    /**
     * Records {@code accepted} as the User's newest password, trimming the history to what the
     * policy reads. Call it once the credential has been written, in the same transaction.
     *
     * @throws IllegalStateException when {@code accepted} has already been remembered
     */
    public void remember(UUID userId, Accepted accepted, Instant setAt) {
        if (accepted.remembered) {
            throw new IllegalStateException("an accepted password is remembered once");
        }
        history.record(userId, accepted.passwordHash, setAt);
        accepted.remembered = true;
    }

    private Decision decide(Optional<PasswordPolicy.Rule> violation, String candidate) {
        if (violation.isPresent()) {
            return new Refused(violation.get());
        }
        return new Accepted(hasher.hash(candidate));
    }

    /**
     * Whether the candidate matches the current credential or a retained one — matched through the
     * hasher, one hash at a time, because a salted hash can only be compared that way.
     */
    private boolean isReused(ScimUser user, String candidate) {
        List<String> remembered = new ArrayList<>(history.findRecentHashes(user.id()));
        if (user.login().hasPassword()) {
            remembered.add(user.login().passwordHash());
        }
        return remembered.stream().anyMatch(hash -> hasher.matches(candidate, hash));
    }
}
