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
 * <p>It is also the shape the administrative endpoints put on the wire, serialised
 * as-is by {@code AdminAccountController} rather than copied into a response
 * record of the same five fields. So a field added here is published: the openapi
 * {@code AccountSummary} schema is this record's documented counterpart.
 *
 * <p>Both refusal mechanisms are reported, because either one alone would
 * mislead. An account locked out right now looks healthy if only {@code enabled}
 * is shown, and there would be no way to tell which accounts need unlocking.
 *
 * <p>There is no field for when a lockout lifts, because a lockout does not lift
 * on its own: it ends when an administrator unlocks the account. {@code locked} is
 * therefore the whole of the lock state, and the only remaining question about it
 * is whose action will end it.
 *
 * @param enabled   administrative standing: false until someone enables it again
 * @param locked    whether a lockout is in force, which stands until Unlock
 * @param createdAt null only for a row written before the column existed
 */
public record AccountSummary(
        String username,
        AccountRole role,
        boolean enabled,
        boolean locked,
        Instant createdAt) {
}
