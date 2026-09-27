package com.example.backend.counter.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for counter storage. Owned by the domain and implemented by
 * an infrastructure adapter, so the dependency points inward.
 */
public interface UserCounterRepository {

    Optional<UserCounter> findByUserId(UUID userId);

    /**
     * Loads a counter while holding a write lock on it for the duration of the
     * surrounding transaction, so concurrent updates serialise.
     */
    Optional<UserCounter> findByUserIdForUpdate(UUID userId);

    UserCounter save(UserCounter counter);
}
