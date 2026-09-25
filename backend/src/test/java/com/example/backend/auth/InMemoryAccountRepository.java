package com.example.backend.auth;

import com.example.backend.auth.domain.Account;
import com.example.backend.auth.domain.AccountRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Account store for tests, standing in for the JPA adapter. Shared so the
 * lockout can be asserted against one persistence behaviour rather than against
 * a slightly different fake per test class.
 *
 * <p>Keyed by the account's stable id, exactly as the real adapter's table is:
 * {@code username} is a mutable attribute looked up with a linear scan, not the
 * row's identity.
 */
public final class InMemoryAccountRepository implements AccountRepository {

    private final Map<UUID, Account> stored = new LinkedHashMap<>();

    private int saves;

    @Override
    public Optional<Account> findByUsername(String username) {
        return stored.values().stream()
                .filter(account -> account.username().equals(username))
                .findFirst();
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return Optional.ofNullable(stored.get(id));
    }

    @Override
    public Account save(Account account) {
        Account nonNullAccount = Objects.requireNonNull(account);
        stored.put(nonNullAccount.id(), nonNullAccount);
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
        Account current = requireById(account.id());
        stored.put(current.id(), current.withEnabled(account.enabled()));
        saves++;
    }

    /** The lockout columns only, for the same reason as {@link #updateEnabled}. */
    @Override
    public void updateLockout(Account account) {
        Account current = requireById(account.id());
        stored.put(current.id(), new Account(
                current.id(),
                current.username(),
                current.passwordHash(),
                current.role(),
                account.failedLoginAttempts(),
                account.lockedAt(),
                current.enabled(),
                current.createdAt()));
        saves++;
    }

    /** The stored account, failing the calling test when there is none. */
    public Account require(String username) {
        return findByUsername(username).orElseThrow(
                () -> new AssertionError("No account stored for " + username));
    }

    private Account requireById(UUID id) {
        return findById(id).orElseThrow(
                () -> new AssertionError("No account stored with id " + id));
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
