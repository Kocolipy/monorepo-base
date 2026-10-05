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
 * <p>{@code externalId} is read-write and replaced like any other optional attribute: the
 * submitted value becomes the calling connector's alias, and an omitted one removes it, because
 * RFC 7644 §3.5.1 makes an omitted read-write attribute unassigned on replace. Only the calling
 * connector's alias is affected — aliases are keyed by connector, so another connector's name for
 * the same User is neither read nor written.
 *
 * <p>{@code active} is the second exception. A PUT that does not assert it keeps the stored value
 * rather than reading the omission as the create default: {@code active} has no unassigned state,
 * and reading "omitted" as {@code true} would let a connector that simply does not map the
 * attribute reactivate a User an earlier write deactivated. Only an explicit
 * {@code active=true} reactivates.
 *
 * @param profile        the replacement profile; its {@code active} is meaningful only when
 *                       {@code activeAsserted}
 * @param password       the submitted password, or {@code null} to keep the stored credential
 * @param externalId     the calling connector's alias after the PUT, or {@code null} to remove it
 * @param activeAsserted whether the body carried a non-null {@code active}
 */
public record ScimUserReplacement(
        ScimUserProfile profile, String password, String externalId, boolean activeAsserted) {

    /** The profile to store over {@code stored}: this one, keeping the stored {@code active} unless asserted. */
    public ScimUserProfile profileOver(ScimUserProfile stored) {
        if (activeAsserted) {
            return profile;
        }
        return new ScimUserProfile(profile.userName(), profile.name(), profile.displayName(),
                profile.preferredLanguage(), profile.locale(), profile.timezone(),
                stored.active(), profile.emails());
    }

    /** Redacted: the password never appears. */
    @Override
    public String toString() {
        return "ScimUserReplacement[profile=" + profile + ", password="
                + (password == null ? "absent" : "present") + "]";
    }
}
