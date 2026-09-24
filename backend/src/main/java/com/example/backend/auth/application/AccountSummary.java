package com.example.backend.auth.application;

import com.example.backend.auth.domain.AccountRole;
import java.time.Instant;

/**
 * What an administrator is allowed to know about an account.
 *
 * <p>This exists so that the password hash cannot be exposed by accident rather
 * than by discipline: the web adapter is handed summaries, and a summary has no
 * field the hash could be written into. A future field that must stay private is
 * added to {@link com.example.backend.auth.domain.Account} and simply not
 * mirrored here.
 *
 * @param email     null only for a row written before the column existed
 * @param createdAt null for the same reason
 */
public record AccountSummary(
        String username,
        String email,
        AccountRole role,
        boolean enabled,
        Instant createdAt) {
}
