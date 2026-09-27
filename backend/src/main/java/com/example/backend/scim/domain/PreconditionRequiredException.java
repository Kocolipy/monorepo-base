package com.example.backend.scim.domain;

/**
 * A write against an existing resource arrived with no {@code If-Match} at all.
 *
 * <p>Refused rather than applied unconditionally because several connectors may write the
 * same directory, and a write carrying no validator is exactly the lost update the
 * precondition exists to prevent. Rendered as {@code 428 Precondition Required} with a detail
 * telling the client to read the resource and retry with its {@code ETag}.
 */
public class PreconditionRequiredException extends RuntimeException {

    public PreconditionRequiredException() {
        super("a write against an existing resource requires an If-Match precondition");
    }
}
