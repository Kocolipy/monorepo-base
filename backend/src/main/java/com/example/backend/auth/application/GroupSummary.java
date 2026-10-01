package com.example.backend.auth.application;

import java.util.UUID;

/**
 * What an administrator is shown about a Group: the Groups projection of the Accounts page.
 *
 * <p>Read-only in its entirety. Groups and their membership are the directory's, written by a
 * connector over SCIM, and the administration adapter exposes no operation on them — so there is
 * nothing here an Admin could edit, and no endpoint that would accept the edit if they tried.
 *
 * <p>A count rather than the member list: the question this view answers is "which Groups exist
 * and how big are they", and the per-User answer to "who is in it" is already each User row's
 * {@code groups}.
 *
 * @param id          the SCIM resource id
 * @param displayName the directory's name for it
 * @param memberCount how many Users are its direct members
 * @param adminGroup  whether this is the server-seeded, protected Admin group, whose direct members
 *                    hold administrative authority. Decided by the reservation marker, never by the
 *                    name — a Group a connector calls "Admins" confers nothing
 */
public record GroupSummary(UUID id, String displayName, int memberCount, boolean adminGroup) {
}
