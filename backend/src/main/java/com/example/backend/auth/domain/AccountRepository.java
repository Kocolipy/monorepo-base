package com.example.backend.auth.domain;

import java.util.List;
import java.util.Optional;

/** Persistence port for database-backed login accounts. */
public interface AccountRepository {

    Optional<Account> findByUsername(String username);

    Account save(Account account);

    /**
     * Every account, ordered by username so the administrative listing is
     * stable across calls. Ordering is the port's promise rather than the
     * caller's sort, because only the adapter can push it into the query.
     */
    List<Account> findAllOrderedByUsername();
}
