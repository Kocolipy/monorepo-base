package com.example.backend.counter.application;

import com.example.backend.auth.application.AccountService;
import com.example.backend.counter.domain.UserCounter;
import com.example.backend.counter.domain.UserCounterRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Counter use cases. Owns the transaction boundary and depends on the domain,
 * its outbound port, and {@link AccountService} to resolve the caller's stable
 * account id.
 *
 * <p>The counter is stored keyed by that id rather than by the username the web
 * layer still reports as the security principal: a username may be renamed, and
 * this tally must keep pointing at the same account afterward.
 */
@Service
public class UserCounterService {

    private final UserCounterRepository repository;
    private final AccountService accounts;

    public UserCounterService(UserCounterRepository repository, AccountService accounts) {
        this.repository = repository;
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public long getCount(String username) {
        UUID accountId = accounts.resolveAccountId(username);
        return repository.findByAccountId(accountId)
                .map(UserCounter::getCount)
                .orElse(0L);
    }

    @Transactional
    public long increment(String username) {
        UUID accountId = accounts.resolveAccountId(username);
        UserCounter counter = repository.findByAccountIdForUpdate(accountId)
                .orElseGet(() -> UserCounter.createFor(accountId));
        counter.increment();
        return repository.save(counter).getCount();
    }

    @Transactional
    public long reset(String username) {
        UUID accountId = accounts.resolveAccountId(username);
        UserCounter counter = repository.findByAccountIdForUpdate(accountId)
                .orElseGet(() -> UserCounter.createFor(accountId));
        counter.reset();
        return repository.save(counter).getCount();
    }
}
