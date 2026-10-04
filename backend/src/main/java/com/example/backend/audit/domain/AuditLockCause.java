package com.example.backend.audit.domain;

/**
 * Why the lock an administrator lifted had been imposed, as a {@code LOCKOUT_LIFT} event records
 * it. The audit trail's own closed set, mirroring the directory's lock causes without depending on
 * the module that owns them.
 */
public enum AuditLockCause {

    /** The failure run reached the lockout policy's limit. */
    FAILURES,

    /** The dormancy job locked the User past the lockout window. */
    DORMANCY
}
