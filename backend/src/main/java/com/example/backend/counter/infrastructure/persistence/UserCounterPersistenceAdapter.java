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
    public Optional<UserCounter> findByUserId(UUID userId) {
        return counters.findById(userId).map(this::toDomain);
    }

    @Override
    public Optional<UserCounter> findByUserIdForUpdate(UUID userId) {
        return counters.findByUserIdForUpdate(userId).map(this::toDomain);
    }

    @Override
    public UserCounter save(UserCounter counter) {
        UserCounterEntity saved = counters.save(
                new UserCounterEntity(counter.getUserId(), counter.getCount()));
        return toDomain(saved);
    }

    private UserCounter toDomain(UserCounterEntity entity) {
        return UserCounter.rehydrate(entity.getUserId(), entity.getCount());
    }
}
