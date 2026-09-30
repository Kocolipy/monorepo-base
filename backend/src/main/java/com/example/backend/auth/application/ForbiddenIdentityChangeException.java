package com.example.backend.auth.application;

/**
 * An administrative change was refused because of who it targets: the acting Admin's own account,
 * or the Bootstrap Admin by anyone but itself.
 *
 * <p>Rendered as {@code 403}, as the specification plan states for these instance-level policy
 * refusals, unlike {@link UnsafeIdentityChangeException}'s {@code 409}: the request is well formed
 * and the caller is an Admin, but this Admin may not do this to this account in any state it
 * could be in.
 */
public class ForbiddenIdentityChangeException extends RuntimeException {

    public ForbiddenIdentityChangeException(String message) {
        super(message);
    }
}
