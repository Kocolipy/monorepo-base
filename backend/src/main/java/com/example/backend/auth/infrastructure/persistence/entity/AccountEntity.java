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

    /**
     * Nullable, unlike {@link #failedLoginAttempts}: there is no sane default for
     * an address nobody recorded, and a made-up one in an administrative listing
     * would be worse than an admitted gap. Startup seeding backfills the
     * configured accounts.
     */
    private String email;

    /**
     * Nullable and boxed for that reason: the column was added to a table that
     * already held rows, so a legacy row carries no value. The adapter reads a
     * missing flag as enabled, which is how those rows behaved before the column
     * existed.
     */
    private Boolean enabled;

    /** Nullable for the same reason as {@link #email}. */
    private Instant createdAt;

    protected AccountEntity() {
    }

    public AccountEntity(
            String username,
            String passwordHash,
            AccountRole role,
            int failedLoginAttempts,
            Instant lockedUntil,
            String email,
            Boolean enabled,
            Instant createdAt) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.failedLoginAttempts = failedLoginAttempts;
        this.lockedUntil = lockedUntil;
        this.email = email;
        this.enabled = enabled;
        this.createdAt = createdAt;
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

    public String getEmail() {
        return email;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
