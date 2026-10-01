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
 * @param profile    the replacement profile
 * @param password   the submitted password, or {@code null} to keep the stored credential
 * @param externalId the calling connector's alias after the PUT, or {@code null} to remove it
 */
public record ScimUserReplacement(ScimUserProfile profile, String password, String externalId) {

    /** Redacted: the password never appears. */
    @Override
    public String toString() {
        return "ScimUserReplacement[profile=" + profile + ", password="
                + (password == null ? "absent" : "present") + "]";
    }
}
