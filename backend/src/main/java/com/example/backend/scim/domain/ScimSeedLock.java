package com.example.backend.scim.domain;

/**
 * Serializes directory seeding across every instance sharing the database.
 *
 * <p>Seeding decides what to create by reading what is there. Without this lock, two instances
 * starting together could both read "absent" and both INSERT; the loser's unique-key violation
 * would abort its transaction and fail its startup. A caught violation cannot be recovered from
 * inside the same transaction, so the race is prevented rather than handled.
 */
public interface ScimSeedLock {

    /**
     * Blocks until the calling transaction holds the seeding lock. The lock is released when that
     * transaction commits or rolls back, never earlier, so there is no release to forget.
     *
     * @throws org.springframework.transaction.IllegalTransactionStateException when called outside
     *         a transaction, where the lock would be released the moment it was taken
     */
    void acquire();
}
