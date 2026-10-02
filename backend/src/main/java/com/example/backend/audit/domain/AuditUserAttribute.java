package com.example.backend.audit.domain;

/**
 * The attributes of a User a SCIM write can move, as a closed set — the User counterpart of
 * {@link AuditGroupAttribute}, closed for the same reason: a caller-assembled path list is a
 * place a submitted value could be written.
 *
 * <p>{@link #PASSWORD} names that the credential changed, never what it changed to. The recorded
 * path is the attribute's name; no value of any attribute ever reaches an event.
 */
public enum AuditUserAttribute {
    USER_NAME,
    NAME,
    DISPLAY_NAME,
    PREFERRED_LANGUAGE,
    LOCALE,
    TIMEZONE,
    ACTIVE,
    PASSWORD,
    EMAILS,
    /**
     * The calling connector's own {@code externalId} alias changed. Named as a path like any
     * other attribute; the alias value itself never reaches an event.
     */
    EXTERNAL_ID,
    /**
     * The User's read-only {@code groups} view. No SCIM write moves it directly; it changes when a
     * membership does, and is named when that change is what ends the User's sessions — the
     * dormant-authority job or a connector's Group write removing its Admin-group membership.
     */
    GROUPS
}
