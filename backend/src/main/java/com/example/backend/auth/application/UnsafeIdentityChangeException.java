package com.example.backend.auth.application;

/**
 * An administrative change was refused because of what it would leave behind.
 *
 * <p>Not an authorization failure: the caller is an authenticated administrator and the action is
 * what is refused. That is why the web adapter renders it as a {@code 409} — neither {@code 403}
 * (the role is fine) nor {@code 400} (the request is well formed) describes it.
 *
 * <p>Each of the refusals it represents would leave the deployment with nobody able to reverse
 * the change: an identity disabling itself, the Bootstrap Admin being disabled, or the last
 * administrator still able to act being disabled.
 */
public class UnsafeIdentityChangeException extends RuntimeException {

    public UnsafeIdentityChangeException(String message) {
        super(message);
    }
}
