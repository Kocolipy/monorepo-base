package com.example.backend.auth.application;

import com.example.backend.scim.domain.PasswordPolicy;

/**
 * A self-service password change was refused because the new password breaks a rule of the
 * password policy, reuse included.
 *
 * <p>Rendered as {@code 400} naming the rule. It carries the rule and nothing else — neither the
 * current nor the new password reaches this object, so neither can reach its message, a log line or
 * a response built from it.
 */
public class PasswordPolicyViolationException extends RuntimeException {

    private final transient PasswordPolicy.Rule rule;

    public PasswordPolicyViolationException(PasswordPolicy.Rule rule) {
        super(rule.message());
        this.rule = rule;
    }

    /** The rule's stable name, for the response body. */
    public String ruleName() {
        return rule.name();
    }
}
