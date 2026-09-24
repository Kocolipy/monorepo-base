package com.example.backend.auth.application;

/**
 * Thrown when an administrative action is refused because performing it would
 * leave nobody able to undo it — disabling the last administrator who can enable
 * accounts, or disabling the account making the request.
 *
 * <p>Distinct from an authorization failure: the caller holds the right role. The
 * action itself is the problem.
 */
public class UnsafeAccountChangeException extends RuntimeException {

    public UnsafeAccountChangeException(String reason) {
        super(reason);
    }
}
