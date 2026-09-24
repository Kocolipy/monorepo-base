package com.example.backend.auth;

import com.example.backend.auth.domain.Account;
import com.example.backend.auth.domain.AccountRepository;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Account store for tests, standing in for the JPA adapter. Shared so the
 * lockout can be asserted against one persistence behaviour rather than against
 * a slightly different fake per test class.
 */
public final class InMemoryAccountRepository implements AccountRepository {

    private final Map<String, Account> stored = new HashMap<>();

    private int saves;

    @Override
    public Optional<Account> findByUsername(String username) {
        return Optional.ofNullable(stored.get(username));
    }

    @Override
    public Account save(Account account) {
        Account nonNullAccount = Objects.requireNonNull(account);
        stored.put(nonNullAccount.username(), nonNullAccount);
        saves++;
        return nonNullAccount;
    }

    @Override
    public List<Account> findAllOrderedByUsername() {
        return stored.values().stream()
                .sorted(Comparator.comparing(Account::username))
                .toList();
    }

    /**
     * Writes the one column the real adapter writes, leaving the rest of the
     * stored row as it stands. Narrow on purpose: a fake that replaced the whole
     * account here would hide exactly the lost update the narrow port exists to
     * prevent.
     */
    @Override
    public void updateEnabled(Account account) {
        Account current = require(account.username());
        stored.put(current.username(), current.withEnabled(account.enabled()));
        saves++;
    }

    /** The lockout columns only, for the same reason as {@link #updateEnabled}. */
    @Override
    public void updateLockout(Account account) {
        Account current = require(account.username());
        stored.put(current.username(), new Account(
                current.username(),
                current.passwordHash(),
                current.role(),
                account.failedLoginAttempts(),
                account.lockedUntil(),
                current.email(),
                current.enabled(),
                current.createdAt()));
        saves++;
    }

    /** The stored account, failing the calling test when there is none. */
    public Account require(String username) {
        return findByUsername(username).orElseThrow(
                () -> new AssertionError("No account stored for " + username));
    }

    /**
     * How many writes this store has taken. Lets a test assert that a login with
     * nothing to clear writes nothing, which is the only observable difference
     * between skipping the write and performing a redundant one.
     */
    public int saves() {
        return saves;
    }
}
