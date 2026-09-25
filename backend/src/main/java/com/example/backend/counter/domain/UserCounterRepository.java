package com.example.backend.counter.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for counter storage. Owned by the domain and implemented by
 * an infrastructure adapter, so the dependency points inward.
 */
public interface UserCounterRepository {

    Optional<UserCounter> findByAccountId(UUID accountId);

    /**
     * Loads a counter while holding a write lock on it for the duration of the
     * surrounding transaction, so concurrent updates serialise.
     */
    Optional<UserCounter> findByAccountIdForUpdate(UUID accountId);

    UserCounter save(UserCounter counter);
}
