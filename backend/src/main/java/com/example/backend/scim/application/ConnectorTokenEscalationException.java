package com.example.backend.scim.application;

/**
 * A connector token issue or rotation refused because the token would carry a Permission the
 * requesting administrator does not hold itself (ADR 0010's no-escalation rule).
 *
 * <p>Answered {@code 403}: the request is well-formed and the caller may mint tokens, but not
 * this one. Carries no message naming the Permissions — the caller knows what it asked for, and
 * the audit trail records it.
 */
public class ConnectorTokenEscalationException extends RuntimeException {

    public ConnectorTokenEscalationException() {
        super("A token may carry only Permissions its creator holds");
    }
}
