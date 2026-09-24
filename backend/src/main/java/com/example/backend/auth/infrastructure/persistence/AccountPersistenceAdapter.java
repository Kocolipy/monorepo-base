package com.example.backend.auth.infrastructure.persistence;

import com.example.backend.auth.domain.Account;
import com.example.backend.auth.domain.AccountRepository;
import com.example.backend.auth.infrastructure.persistence.entity.AccountEntity;
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
                account.lockedUntil())));
    }

    private Account toDomain(AccountEntity entity) {
        return new Account(
                entity.getUsername(),
                entity.getPasswordHash(),
                entity.getRole(),
                entity.getFailedLoginAttempts(),
                entity.getLockedUntil());
    }
}
