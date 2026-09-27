package com.example.backend.scim.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * The resources this deployment reserves for its own recovery, and the marker that
 * says so.
 *
 * <p>Two resources cannot be provisioned away: the Bootstrap Admin, which is the
 * local identity an administrator recovers the deployment with when external
 * provisioning is unavailable or has removed every SCIM-managed administrator, and
 * the Admin group, whose membership <em>is</em> administrative authority. A SCIM
 * client may read both. No SCIM write may change either, and none may remove the
 * Bootstrap Admin's membership of the Group.
 *
 * <p>Held on the resource row rather than recognised by attribute. Recognising the
 * Admin group by its {@code displayName} or the Bootstrap Admin by its
 * {@code userName} would make the protection depend on a value that, for every
 * other resource, a connector may change — and the failure mode of getting that
 * wrong is a directory whose recovery identity can be renamed out of its own
 * exemption. A marker is set once, by seeding, and no write path changes it.
 *
 * <p>This is also the lockout exemption. {@link #BOOTSTRAP_ADMIN} is the one
 * principal a failure run never locks, because a lock is lifted only by an
 * administrator's Unlock and a locked recovery identity is a deployment nobody can
 * enter. The exemption and the write protection are the same marker deliberately:
 * they exist for the same reason and must never disagree about which User they are
 * about.
 */
public enum ReservedResourceName {

    /** The deployment's local recovery User: read-only, and never locked. */
    BOOTSTRAP_ADMIN("bootstrap-admin"),

    /**
     * The server-seeded Group whose members hold administrative authority. Its
     * stable resource id carries that meaning, so authority derivation resolves the
     * Group through this marker and never through its display name.
     */
    ADMIN_GROUP("admin-group");

    private final String storedValue;

    ReservedResourceName(String storedValue) {
        this.storedValue = storedValue;
    }

    /**
     * The value in {@code scim_resources.reserved_name}.
     *
     * <p>Spelled out rather than derived from {@link #name()}, so renaming a Java
     * constant cannot change what is in the column — and the column's CHECK
     * constraint lists exactly these two strings.
     */
    public String storedValue() {
        return storedValue;
    }

    /**
     * The reservation a stored value names, or empty for an ordinary resource.
     *
     * <p>An unrecognised non-null value throws rather than reading as unreserved: the
     * database constrains the column to these two strings, so a third one means the
     * schema and this enum have diverged, and treating it as "not protected" would
     * silently open the recovery resources to writes.
     */
    public static Optional<ReservedResourceName> ofStoredValue(String storedValue) {
        if (storedValue == null) {
            return Optional.empty();
        }
        return Optional.of(Arrays.stream(values())
                .filter(reserved -> reserved.storedValue.equals(storedValue))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "a resource is reserved under a name this application does not know")));
    }
}
