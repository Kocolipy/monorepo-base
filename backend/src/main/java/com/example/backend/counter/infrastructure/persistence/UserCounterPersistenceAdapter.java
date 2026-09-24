package com.example.backend.counter.infrastructure.persistence;

import com.example.backend.counter.domain.UserCounter;
import com.example.backend.counter.domain.UserCounterRepository;
import com.example.backend.counter.infrastructure.persistence.entity.UserCounterEntity;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Adapts the domain's {@link UserCounterRepository} port onto JPA, translating
 * between the domain type and the persistence entity.
 */
@Repository
class UserCounterPersistenceAdapter implements UserCounterRepository {

    private final UserCounterJpaRepository counters;

    UserCounterPersistenceAdapter(UserCounterJpaRepository counters) {
        this.counters = counters;
    }

    @Override
    public Optional<UserCounter> findByUsername(String username) {
        return counters.findById(username).map(this::toDomain);
    }

    @Override
    public Optional<UserCounter> findByUsernameForUpdate(String username) {
        return counters.findByUsernameForUpdate(username).map(this::toDomain);
    }

    @Override
    public UserCounter save(UserCounter counter) {
        UserCounterEntity saved = counters.save(
                new UserCounterEntity(counter.getUsername(), counter.getCount()));
        return toDomain(saved);
    }

    private UserCounter toDomain(UserCounterEntity entity) {
        return UserCounter.rehydrate(entity.getUsername(), entity.getCount());
    }
}
