package com.example.backend.scim.application;

import com.example.backend.scim.domain.ScimUserProfile;

/**
 * A PUT's replacement for a User: the complete profile, and a password only when one was sent.
 *
 * <p>The profile is already complete — an optional attribute the document did not mention has
 * become unassigned at the adapter, because that is what replacing a resource means. The password
 * is the exception: omitting it leaves the credential as it is, because a write-only secret can
 * never be read back and resent, so omission cannot be read as removal.
 *
 * <p>The connector's alias is established at creation and a replacement does not change it. A PUT
 * able to change it would let a replacement silently re-key the resource in the caller's own
 * namespace, and one that had to restate it would delete the alias of every connector that
 * omitted the field. So the alias the body carried is kept only to be checked: absent or equal to
 * the stored one is accepted — a client may PUT back what it read — and a different one is refused
 * rather than dropped, which is how PATCH answers the same attempt.
 *
 * @param profile        the replacement profile
 * @param password       the submitted password, or {@code null} to keep the stored credential
 * @param sentExternalId the {@code externalId} the body carried, or {@code null}
 */
public record ScimUserReplacement(ScimUserProfile profile, String password, String sentExternalId) {

    /** Redacted: the password never appears. */
    @Override
    public String toString() {
        return "ScimUserReplacement[profile=" + profile + ", password="
                + (password == null ? "absent" : "present") + "]";
    }
}
