package com.example.backend.scim.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

/**
 * Database representation of a User's authentication state: the stored credential, the
 * run of consecutive failed logins, and the instant a lock was imposed.
 *
 * <p>Embedded rather than three loose columns on {@link ScimUserEntity}, mirroring the
 * domain's {@code ScimLoginState}. The grouping is what makes "the login path writes
 * these and only these" visible in the mapping as well as in the port, and it keeps the
 * User entity's constructor from growing a third and fourth parameter that read as
 * profile attributes but are not.
 *
 * <p>None of the three is a SCIM attribute, so writing them changes nothing a client can
 * read — which is why the port's login-state write deliberately does not advance the
 * resource's version or {@code lastModified}.
 */
@Embeddable
public class ScimLoginStateValue {

    /** Nullable: a credentialless User is a supported state, not an incomplete one. */
    @Column(name = "password_hash", length = 256)
    private String passwordHash;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    /**
     * When a lock was imposed, or null when none is.
     *
     * <p>Not when a lock ENDS: it does not end on its own. A lock is lifted by an
     * administrator's Unlock and by nothing else, so the column's presence is the whole
     * state and no clock is consulted to read it.
     */
    @Column(name = "locked_at")
    private Instant lockedAt;

    protected ScimLoginStateValue() {
    }

    public ScimLoginStateValue(String passwordHash, int failedLoginAttempts, Instant lockedAt) {
        this.passwordHash = passwordHash;
        this.failedLoginAttempts = failedLoginAttempts;
        this.lockedAt = lockedAt;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant getLockedAt() {
        return lockedAt;
    }

    /**
     * Replaces the stored credential — the only mutator here, and deliberately only for the hash:
     * the failure run and the lock instant are written by the login path's own narrow statement.
     * Assigning the same value leaves the column clean, so an unchanged credential is not written.
     */
    public void replacePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
