package com.example.backend.auth.application;

/**
 * No live SCIM User carries the submitted {@code userName}.
 *
 * <p>Raised by the administrative use cases, which address an identity by the name an
 * administrator reads rather than by its id. The web adapter renders it as a {@code 404}, and
 * naming what is missing reveals nothing the caller could not read from the listing they are
 * already entitled to.
 *
 * <p>Carries no message naming the value, for the reason
 * {@link com.example.backend.scim.domain.DuplicateUserNameException} does not: an exception
 * message is the shortest path into a log line, and a {@code userName} is half a credential — on
 * a mistyped request it is very often the other half.
 */
public class UnknownIdentityException extends RuntimeException {

    public UnknownIdentityException() {
        super("no live SCIM User carries this userName");
    }
}
