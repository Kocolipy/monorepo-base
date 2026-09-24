package com.example.backend.auth.domain;

/** A login account as understood by authentication, independent of persistence. */
public record Account(String username, String passwordHash, AccountRole role) {
}
