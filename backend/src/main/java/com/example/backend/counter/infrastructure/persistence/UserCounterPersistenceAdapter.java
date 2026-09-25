package com.example.backend.counter.infrastructure.persistence;

import com.example.backend.counter.domain.UserCounter;
import com.example.backend.counter.domain.UserCounterRepository;
import com.example.backend.counter.infrastructure.persistence.entity.UserCounterEntity;
import java.util.Optional;
import java.util.UUID;
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
    public Optional<UserCounter> findByAccountId(UUID accountId) {
        return counters.findById(accountId).map(this::toDomain);
    }

    @Override
    public Optional<UserCounter> findByAccountIdForUpdate(UUID accountId) {
        return counters.findByAccountIdForUpdate(accountId).map(this::toDomain);
    }

    @Override
    public UserCounter save(UserCounter counter) {
        UserCounterEntity saved = counters.save(
                new UserCounterEntity(counter.getAccountId(), counter.getCount()));
        return toDomain(saved);
    }

    private UserCounter toDomain(UserCounterEntity entity) {
        return UserCounter.rehydrate(entity.getAccountId(), entity.getCount());
    }
}
