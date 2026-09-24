package com.example.backend.counter.domain;

import java.util.Optional;

/**
 * Outbound port for counter storage. Owned by the domain and implemented by
 * an infrastructure adapter, so the dependency points inward.
 */
public interface UserCounterRepository {

    Optional<UserCounter> findByUsername(String username);

    /**
     * Loads a counter while holding a write lock on it for the duration of the
     * surrounding transaction, so concurrent updates serialise.
     */
    Optional<UserCounter> findByUsernameForUpdate(String username);

    UserCounter save(UserCounter counter);
}
