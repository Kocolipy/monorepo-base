package com.example.backend.auth.application;

import java.time.Instant;
import java.util.UUID;

/**
 * What an administrator is allowed to know about a login identity.
 *
 * <p>This exists so that the password hash cannot be exposed by accident rather than by
 * discipline: the web adapter is handed summaries, and a summary has no field the hash could be
 * written into. A future component that must stay private is added to
 * {@link com.example.backend.scim.domain.ScimUser} and simply not mirrored here.
 *
 * <p>It is also the shape the administrative endpoints put on the wire, serialised as-is by
 * {@code AdminAccountController} rather than copied into a response record of the same fields. So
 * a field added here is published: the openapi {@code AccountSummary} schema is this record's
 * documented counterpart.
 *
 * <p>{@code id} is present because the identity now has a stable one worth reporting: it is the
 * SCIM resource id, so an administrator reading this listing and a connector reading the SCIM
 * interface are looking at the same resource under the same name. {@code userName} is a mutable
 * attribute, and the operational endpoints still address by it because that is what an
 * administrator reads — but the id is what the audit trail records.
 *
 * <p>{@code admin} replaces the former role field. It is DERIVED from direct membership of the
 * Admin group rather than stored, so a listing showing it is reporting the same fact the login
 * path derives, and there is no column the two could disagree about.
 *
 * <p>Both refusal mechanisms are reported, because either one alone would mislead. An identity
 * locked out right now looks healthy if only {@code active} is shown, and there would be no way
 * to tell which ones need unlocking.
 *
 * <p>There is no field for when a lockout lifts, because a lockout does not lift on its own: it
 * ends when an administrator unlocks the identity. {@code locked} is therefore the whole of the
 * lock state, and the only remaining question about it is whose action will end it.
 *
 * @param id           the SCIM resource id, stable and non-reassignable
 * @param userName     the login and display attribute, mutable
 * @param admin        whether the Admin group confers administrative authority on this identity
 * @param active       administrative standing: false until someone reactivates it
 * @param locked       whether a lockout is in force, which stands until Unlock
 * @param hasPassword  whether a credential is set at all; a credentialless User exists and
 *                     cannot log in, which is otherwise indistinguishable from a forgotten
 *                     password
 * @param createdAt    when the resource was created
 */
public record IdentitySummary(
        UUID id,
        String userName,
        boolean admin,
        boolean active,
        boolean locked,
        boolean hasPassword,
        Instant createdAt) {
}
