package com.example.backend.auth.infrastructure.persistence;

import com.example.backend.auth.domain.Account;
import com.example.backend.auth.domain.AccountRepository;
import com.example.backend.auth.infrastructure.persistence.entity.AccountEntity;
import java.util.List;
import java.util.Optional;
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
        return accounts.findById(username).map(this::toDomain);
    }

    @Override
    public Account save(Account account) {
        return toDomain(accounts.save(new AccountEntity(
                account.username(),
                account.passwordHash(),
                account.role(),
                account.failedLoginAttempts(),
                account.lockedUntil(),
                account.enabled(),
                account.createdAt())));
    }

    @Override
    public void updateEnabled(Account account) {
        accounts.updateEnabled(account.username(), account.enabled());
    }

    @Override
    public void updateLockout(Account account) {
        accounts.updateLockout(
                account.username(), account.failedLoginAttempts(), account.lockedUntil());
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
                entity.getUsername(),
                entity.getPasswordHash(),
                entity.getRole(),
                entity.getFailedLoginAttempts(),
                entity.getLockedUntil(),
                entity.getEnabled() == null || entity.getEnabled(),
                entity.getCreatedAt());
    }
}
