package com.example.backend.auth.infrastructure.persistence;

import com.example.backend.auth.domain.Account;
import com.example.backend.auth.domain.AccountRepository;
import com.example.backend.auth.infrastructure.persistence.entity.AccountEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Maps the account repository port onto JPA entities. */
@Repository
class AccountPersistenceAdapter implements AccountRepository {

    private final AccountJpaRepository accounts;

    AccountPersistenceAdapter(AccountJpaRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public Optional<Account> findByUsername(String username) {
        return accounts.findByUsername(username).map(this::toDomain);
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return accounts.findById(id).map(this::toDomain);
    }

    @Override
    public Account save(Account account) {
        return toDomain(accounts.save(new AccountEntity(
                account.id(),
                account.username(),
                account.passwordHash(),
                account.role(),
                account.failedLoginAttempts(),
                account.lockedAt(),
                account.enabled(),
                account.createdAt())));
    }

    @Override
    public void updateEnabled(Account account) {
        accounts.updateEnabled(account.id(), account.enabled());
    }

    @Override
    public void updateLockout(Account account) {
        accounts.updateLockout(
                account.id(), account.failedLoginAttempts(), account.lockedAt());
    }

    @Override
    public List<Account> findAllOrderedByUsername() {
        return accounts.findAllByOrderByUsernameAsc().stream().map(this::toDomain).toList();
    }

    /**
     * A null {@code enabled} column means the row predates the column, and those
     * rows could authenticate, so they read back as enabled.
     */
    private Account toDomain(AccountEntity entity) {
        return new Account(
                entity.getId(),
                entity.getUsername(),
                entity.getPasswordHash(),
                entity.getRole(),
                entity.getFailedLoginAttempts(),
                entity.getLockedAt(),
                entity.getEnabled() == null || entity.getEnabled(),
                entity.getCreatedAt());
    }
}
