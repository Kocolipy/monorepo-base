package com.example.backend.counter.application;

import com.example.backend.counter.domain.UserCounter;
import com.example.backend.counter.domain.UserCounterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Counter use cases. Owns the transaction boundary and depends only on the
 * domain and its outbound port.
 */
@Service
public class UserCounterService {

    private final UserCounterRepository repository;

    public UserCounterService(UserCounterRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public long getCount(String username) {
        return repository.findByUsername(username)
                .map(UserCounter::getCount)
                .orElse(0L);
    }

    @Transactional
    public long increment(String username) {
        UserCounter counter = repository.findByUsernameForUpdate(username)
                .orElseGet(() -> UserCounter.createFor(username));
        counter.increment();
        return repository.save(counter).getCount();
    }

    @Transactional
    public long reset(String username) {
        UserCounter counter = repository.findByUsernameForUpdate(username)
                .orElseGet(() -> UserCounter.createFor(username));
        counter.reset();
        return repository.save(counter).getCount();
    }
}
