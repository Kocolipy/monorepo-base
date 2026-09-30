package com.example.backend.auth.application;

/**
 * A self-service password change was refused on the caller's credential or standing: the current
 * password did not verify, a lockout is in force, or the User is inactive or gone.
 *
 * <p>Rendered as an empty {@code 401}, the answer Login gives for every refusal, so the change
 * endpoint tells a caller no more about the account than Login does. A wrong current password has
 * already been counted toward the lockout by the time this is thrown.
 */
public class CurrentPasswordRejectedException extends RuntimeException {

    public CurrentPasswordRejectedException() {
        super("The password change was refused");
    }
}
