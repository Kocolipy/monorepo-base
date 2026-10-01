package com.example.backend.scim.domain;

/**
 * A submitted attribute value is longer than the column that stores it, so it is refused before
 * persistence rather than left to the database.
 *
 * <p>Rendered as {@code 400 invalidValue} naming the attribute and its limit. It carries the
 * attribute path and the limit and nothing else: the value never reaches this object, so it
 * cannot reach its message, a log line or a response built from it.
 */
public class ScimValueTooLongException extends RuntimeException {

    private final String attribute;

    private final int limit;

    public ScimValueTooLongException(String attribute, int limit) {
        super(attribute + " is longer than " + limit + " characters.");
        this.attribute = attribute;
        this.limit = limit;
    }

    /** The SCIM attribute path the value was submitted for, e.g. {@code emails.type}. */
    public String attribute() {
        return attribute;
    }

    /** The longest value the attribute accepts, in Unicode code points. */
    public int limit() {
        return limit;
    }
}
