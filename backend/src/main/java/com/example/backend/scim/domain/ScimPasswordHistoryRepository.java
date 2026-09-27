package com.example.backend.scim.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Persistence port for a User's password history: the hashes of its most recent passwords, kept
 * only to refuse their reuse.
 *
 * <p>Holds hashes, never a password, and has no operation that returns anything but hashes — a
 * candidate is checked by the caller matching it against each one through the password encoder,
 * which is the only thing that can compare a plaintext with an Argon2id hash. The history is
 * deleted with the User by the database's cascade, so there is no delete operation here to
 * forget to call.
 */
public interface ScimPasswordHistoryRepository {

    /** The User's retained password hashes, newest first; at most {@link PasswordHistoryPolicy#RETAINED}. */
    List<String> findRecentHashes(UUID userId);

    /**
     * Appends this hash as the User's newest password and trims the history to the newest
     * {@link PasswordHistoryPolicy#RETAINED}, in one call so the history can never be left longer
     * than the rule reads.
     */
    void record(UUID userId, String passwordHash, Instant setAt);
}
