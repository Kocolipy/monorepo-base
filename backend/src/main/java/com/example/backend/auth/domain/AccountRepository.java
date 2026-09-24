package com.example.backend.auth.domain;

import java.util.Optional;

/** Persistence port for database-backed login accounts. */
public interface AccountRepository {

    Optional<Account> findByUsername(String username);

    Account save(Account account);
}
