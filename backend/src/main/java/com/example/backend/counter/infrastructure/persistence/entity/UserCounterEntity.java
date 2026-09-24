package com.example.backend.counter.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA representation of a counter. Kept separate from the domain type so that
 * mapping concerns stay in infrastructure.
 */
@Entity
@Table(name = "user_counters")
public class UserCounterEntity {

    @Id
    private String username;

    private long count;

    protected UserCounterEntity() {
    }

    public UserCounterEntity(String username, long count) {
        this.username = username;
        this.count = count;
    }

    public String getUsername() {
        return username;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
