package com.example.backend.counter.application;

import com.example.backend.auth.application.LoginIdentityService;
import com.example.backend.counter.domain.UserCounter;
import com.example.backend.counter.domain.UserCounterRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Counter use cases. Owns the transaction boundary and depends on the domain,
 * its outbound port, and {@link LoginIdentityService} to resolve the caller's stable
 * SCIM User id.
 *
 * <p>The counter is stored keyed by that id rather than by the userName the web
 * layer still reports as the security principal: a userName is mutable, and
 * this tally must keep pointing at the same identity afterward.
 */
@Service
public class UserCounterService {

    private final UserCounterRepository repository;
    private final LoginIdentityService identities;

    public UserCounterService(UserCounterRepository repository, LoginIdentityService identities) {
        this.repository = repository;
        this.identities = identities;
    }

    @Transactional(readOnly = true)
    public long getCount(String username) {
        UUID userId = identities.resolveUserId(username);
        return repository.findByUserId(userId)
                .map(UserCounter::getCount)
                .orElse(0L);
    }

    @Transactional
    public long increment(String username) {
        UUID userId = identities.resolveUserId(username);
        UserCounter counter = repository.findByUserIdForUpdate(userId)
                .orElseGet(() -> UserCounter.createFor(userId));
        counter.increment();
        return repository.save(counter).getCount();
    }

    @Transactional
    public long reset(String username) {
        UUID userId = identities.resolveUserId(username);
        UserCounter counter = repository.findByUserIdForUpdate(userId)
                .orElseGet(() -> UserCounter.createFor(userId));
        counter.reset();
        return repository.save(counter).getCount();
    }
}
