package com.example.backend.scim.domain;

/**
 * A requested token lifetime that is not positive or exceeds
 * {@link ConnectorTokenPolicy#MAX_LIFETIME}.
 *
 * <p>Raised by the policy rather than clamped, unlike the rotation overlap. An
 * overlap longer than the token has left is a reasonable thing to ask for and has
 * an obvious answer; a lifetime of two years is a request the ceiling exists to
 * refuse, and silently issuing a one-year token would leave the Admin believing
 * something untrue about when their integration breaks.
 */
public class InvalidConnectorTokenLifetimeException extends RuntimeException {

    public InvalidConnectorTokenLifetimeException(String message) {
        super(message);
    }
}
