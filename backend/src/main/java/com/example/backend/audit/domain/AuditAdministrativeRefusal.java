package com.example.backend.audit.domain;

/**
 * Why an administrative change was refused, as a closed set.
 *
 * <p>A closed set for the reason {@link AuditRefusalReason} is one: the reason is a field
 * every later query groups by, and a free-text reason is a place a caller's own value could
 * be written. These are refusals about the ACTION rather than about the caller — the caller
 * is an authenticated administrator in every case — so none of them is an authorization
 * failure and none is recorded as one.
 *
 * <p>Refusals are recorded and not merely logged because each of them is an attempt to
 * remove the deployment's last way back in. A run of them is the signal worth having, and a
 * log line that carries no subject id cannot be correlated to the identity it was about.
 */
public enum AuditAdministrativeRefusal {

    /**
     * An administrator tried to disable their own identity, which would take away the
     * access needed to reverse it.
     */
    SELF_DISABLE,

    /**
     * The target is the Bootstrap Admin, the deployment's recovery identity. Its exemption
     * from lockout is what keeps every other identity's permanent lock recoverable, and a
     * disabled Bootstrap Admin cannot log in — so disabling it while the other
     * administrators are locked out would leave a deployment nothing but direct database
     * access can repair.
     */
    PROTECTED_RESOURCE,

    /**
     * The target is the only administrator still able to act, so disabling it would leave
     * nobody able to enable it again.
     */
    LAST_ENABLED_ADMINISTRATOR
}
