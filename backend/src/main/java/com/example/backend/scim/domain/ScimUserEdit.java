package com.example.backend.scim.domain;

/**
 * Everything a connector can write on a User, as one value: the profile, the calling
 * connector's {@code externalId}, and what happens to the credential.
 *
 * <p>This is what a PUT or a PATCH computes. Both start from the stored User as the calling
 * connector sees it and end at the desired state, and the use case then compares the two to
 * decide what — if anything — to write, which versions to advance and which sessions to end.
 * Computing the whole desired state before writing any of it is what makes a PATCH atomic by
 * construction: a sequence whose third operation is refused never wrote the first two.
 *
 * <p>No read-only attribute is here — not {@code id}, {@code meta} or {@code groups} — so no
 * write can be expressed that changes one; there is nowhere to put it.
 *
 * @param profile    the profile attributes, never null
 * @param externalId the calling connector's alias, or {@code null} when it has none. Read-write:
 *                   a PUT or PATCH may set, change or remove it, and only ever the CALLING
 *                   connector's — the use case writes it under that connector's id and no other
 * @param password   what the write does to the credential, never null
 */
public record ScimUserEdit(ScimUserProfile profile, String externalId, ScimPasswordChange password) {

    public ScimUserEdit {
        if (profile == null) {
            throw new IllegalArgumentException("an edit has a profile");
        }
        password = password == null ? ScimPasswordChange.UNCHANGED : password;
    }

    /** The stored User as a starting point: its profile, this alias, and the credential kept. */
    public static ScimUserEdit of(ScimUserProfile profile, String externalId) {
        return new ScimUserEdit(profile, externalId, ScimPasswordChange.UNCHANGED);
    }

    ScimUserEdit withProfile(ScimUserProfile changed) {
        return new ScimUserEdit(changed, externalId, password);
    }

    ScimUserEdit withPassword(ScimPasswordChange changed) {
        return new ScimUserEdit(profile, externalId, changed);
    }

    ScimUserEdit withExternalId(String changed) {
        return new ScimUserEdit(profile, changed, password);
    }
}
