package com.example.backend.auth.infrastructure.persistence.entity;

import com.example.backend.auth.domain.AccountRole;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Database representation of an account that may authenticate. */
@Entity
@Table(name = "accounts")
public class AccountEntity {

    @Id
    private String username;

    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private AccountRole role;

    protected AccountEntity() {
    }

    public AccountEntity(String username, String passwordHash, AccountRole role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
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
}
