package com.example.backend.auth.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence port for database-backed login accounts. */
public interface AccountRepository {

    Optional<Account> findByUsername(String username);

    /**
     * Looks up an account by its stable, non-reassignable id rather than its
     * mutable {@code username}. Anything that must keep resolving to the same
     * account across a username change — the disable/enable/unlock path acting
     * on a caller's own id, a stable-id-keyed lookup elsewhere — reaches the
     * account through this method instead of {@link #findByUsername}.
     */
    Optional<Account> findById(UUID id);

    /**
     * Writes the whole account. For creating one, and for seeding's backfill of a
     * row that is missing columns — cases that own every field they write.
     */
    Account save(Account account);

    /**
     * Writes only the {@code enabled} column of an account that already exists.
     *
     * <p>Narrow on purpose. The login path writes the same row whenever an
     * attempt is rejected, and it writes it whole, from an account it read at the
     * start of its own transaction. A full-row write from here would race that
     * one and could revert a lockout imposed in between; this cannot, because it
     * touches no column the login path cares about.
     */
    void updateEnabled(Account account);

    /**
     * Writes only the lockout columns of an account that already exists, for the
     * same reason {@link #updateEnabled} is narrow: an administrator lifting a
     * lockout must not revert an administrative decision made in between.
     */
    void updateLockout(Account account);

    /**
     * Every account, ordered by username so the administrative listing is
     * stable across calls. Ordering is the port's promise rather than the
     * caller's sort, because only the adapter can push it into the query.
     */
    List<Account> findAllOrderedByUsername();
}
