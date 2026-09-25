package com.example.backend.scim.application;

/**
 * A connector or token id that names nothing, or names something already deleted.
 *
 * <p>One exception for both, because the caller is an authenticated administrator
 * for whom the distinction between "never existed" and "gone" carries no privilege
 * — they can read either from the listing. A connector has no such exception on the
 * SCIM side: a request whose token names a deleted connector is refused as an
 * invalid token, which tells the connector nothing about why.
 */
public class UnknownConnectorException extends RuntimeException {

    public UnknownConnectorException(String message) {
        super(message);
    }
}
