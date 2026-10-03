package com.example.backend.scim.domain;

/**
 * The RFC 7643 §2.3 data types this service's attributes have.
 *
 * <p>One closed set for every consumer: discovery renders it, request reading checks values
 * against it, and the query modules compare by it. No attribute here is numeric or binary, so
 * those RFC types are absent rather than declared and unused.
 */
public enum ScimAttributeType {
    STRING,
    BOOLEAN,
    DATE_TIME,
    REFERENCE,
    COMPLEX
}
