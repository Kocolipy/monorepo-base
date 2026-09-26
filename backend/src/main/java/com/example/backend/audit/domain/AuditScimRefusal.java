package com.example.backend.audit.domain;

/**
 * Why a SCIM write was refused, as a closed set.
 *
 * <p>A closed set for the same reason {@link AuditRefusalReason} is one: the reason
 * is a field every later query groups by, and a free-text reason is a place a
 * caller's own value — a {@code userName}, a filter, a submitted attribute — could
 * be written. The names are SCIM's own {@code scimType} values, upper-cased, so a
 * recorded refusal and the error body the caller received name the same thing.
 */
public enum AuditScimRefusal {

    /** A live resource already holds the unique attribute the write asked for. */
    UNIQUENESS
}
