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
 * <p>Both refusal mechanisms are reported, because either one alone would
 * mislead. An account locked out right now looks healthy if only {@code enabled}
 * is shown, and there would be no way to tell which accounts need unlocking.
 *
 * @param enabled     administrative standing: false until someone enables it again
 * @param locked      whether the lockout is in force, as the server evaluated it
 *                    when answering — a client comparing {@code lockedUntil} to
 *                    its own clock would disagree with the server that enforces it
 * @param lockedUntil when the current lockout lifts; null if none was ever
 *                    imposed, and kept after one expires
 * @param email       null only for a row written before the column existed
 * @param createdAt   null for the same reason
 */
public record AccountSummary(
        String username,
        String email,
        AccountRole role,
        boolean enabled,
        boolean locked,
        Instant lockedUntil,
        Instant createdAt) {
}
