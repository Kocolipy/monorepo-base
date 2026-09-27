package com.example.backend.counter.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * JPA representation of a counter. Kept separate from the domain type so that
 * mapping concerns stay in infrastructure.
 */
@Entity
@Table(name = "user_counters")
public class UserCounterEntity {

    @Id
    private UUID userId;

    private long count;

    protected UserCounterEntity() {
    }

    public UserCounterEntity(UUID userId, long count) {
        this.userId = userId;
        this.count = count;
    }

    public UUID getUserId() {
        return userId;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
