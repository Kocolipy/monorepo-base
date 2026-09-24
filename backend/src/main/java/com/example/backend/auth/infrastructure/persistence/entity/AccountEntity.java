package com.example.backend.auth.infrastructure.persistence.entity;

import com.example.backend.auth.domain.AccountRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Database representation of an account that may authenticate. */
@Entity
@Table(name = "accounts")
public class AccountEntity {

    @Id
    private String username;

    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private AccountRole role;

    /**
     * Not nullable, so an account that predates the lockout columns reads back as
     * "nothing failed yet" rather than as null and a later increment on it does
     * not have to guess.
     */
    @Column(nullable = false)
    private int failedLoginAttempts;

    /** Null until a lockout has been imposed; kept, not cleared, once it expires. */
    private Instant lockedUntil;

    protected AccountEntity() {
    }

    public AccountEntity(
            String username,
            String passwordHash,
            AccountRole role,
            int failedLoginAttempts,
            Instant lockedUntil) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.failedLoginAttempts = failedLoginAttempts;
        this.lockedUntil = lockedUntil;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public AccountRole getRole() {
        return role;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }
}
