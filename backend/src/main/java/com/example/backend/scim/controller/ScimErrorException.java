package com.example.backend.scim.controller;

import org.springframework.http.HttpStatus;

/**
 * A refusal a SCIM caller is entitled to see, carrying the three things its error body
 * needs: the status, the standard {@code scimType} where one applies, and a detail
 * safe to render.
 *
 * <p>An exception rather than a returned error value because the refusals happen deep
 * in parsing — an unsupported attribute is discovered while reading a nested object —
 * and threading a failure back out through every reader would mean every reader having
 * a failure path a future one could forget.
 *
 * <p><strong>{@code detail} is written by this service, never by the caller.</strong>
 * Every construction site below passes a literal or an attribute PATH; no site passes a
 * submitted value. That is the rule that keeps a {@code userName}, a password or a
 * filter literal out of an error body — and out of the log line an unhandled exception
 * would produce — and it is a rule about call sites rather than about this class,
 * because a {@code String} parameter cannot express it.
 */
public class ScimErrorException extends RuntimeException {

    private final HttpStatus status;

    private final String scimType;

    private ScimErrorException(HttpStatus status, String scimType, String detail) {
        super(detail);
        this.status = status;
        this.scimType = scimType;
    }

    /** The body was not valid SCIM: unparseable, or missing something required. */
    public static ScimErrorException invalidSyntax(String detail) {
        return new ScimErrorException(HttpStatus.BAD_REQUEST, "invalidSyntax", detail);
    }

    /**
     * A value or an attribute was not acceptable: the wrong JSON type, an attribute this
     * service does not implement, or a projection naming something that is not an
     * attribute.
     */
    public static ScimErrorException invalidValue(String detail) {
        return new ScimErrorException(HttpStatus.BAD_REQUEST, "invalidValue", detail);
    }

    /** A live resource already holds a unique attribute the write asked for. */
    public static ScimErrorException uniqueness(String detail) {
        return new ScimErrorException(HttpStatus.CONFLICT, "uniqueness", detail);
    }

    /**
     * The write targeted something that cannot be changed — a resource this deployment
     * reserves for its own recovery.
     *
     * <p>{@code 400} and not {@code 403}. The caller's token may be a perfectly valid
     * read-write one: it is the TARGET that is refused, not the credential, and answering
     * {@code 403} would send an integrator to re-check a token scope that is fine. SCIM's
     * own {@code scimType} for this condition is {@code mutability}.
     */
    public static ScimErrorException mutability(String detail) {
        return new ScimErrorException(HttpStatus.BAD_REQUEST, "mutability", detail);
    }

    /** A PATCH path that is malformed or names no attribute this service implements. */
    public static ScimErrorException invalidPath(String detail) {
        return new ScimErrorException(HttpStatus.BAD_REQUEST, "invalidPath", detail);
    }

    /** A PATCH filter that selected no value. */
    public static ScimErrorException noTarget(String detail) {
        return new ScimErrorException(HttpStatus.BAD_REQUEST, "noTarget", detail);
    }

    /** A filter using syntax or a comparison this service does not support. */
    public static ScimErrorException invalidFilter(String detail) {
        return new ScimErrorException(HttpStatus.BAD_REQUEST, "invalidFilter", detail);
    }

    /** A write against an existing resource sent no {@code If-Match}. No {@code scimType} exists for it. */
    public static ScimErrorException preconditionRequired(String detail) {
        return new ScimErrorException(HttpStatus.PRECONDITION_REQUIRED, null, detail);
    }

    /** A write's {@code If-Match} named a version that is no longer current. */
    public static ScimErrorException preconditionFailed(String detail) {
        return new ScimErrorException(HttpStatus.PRECONDITION_FAILED, null, detail);
    }

    /** No live resource has this id, or no such discovery document exists. */
    public static ScimErrorException notFound(String detail) {
        return new ScimErrorException(HttpStatus.NOT_FOUND, null, detail);
    }

    /**
     * The request asked for a query capability this service does not implement.
     *
     * <p>{@code 403} and no {@code scimType}, which is RFC 7644 §3.4.2.2's answer for a
     * provider that does not support filtering: the alternative — ignoring the parameter
     * — would return every resource to a caller that asked for a few and believes it
     * received a match. A refusal is the only answer that cannot be misread, and it is
     * consistent with what discovery advertises.
     */
    public static ScimErrorException unsupportedQuery(String detail) {
        return new ScimErrorException(HttpStatus.FORBIDDEN, null, detail);
    }

    /** The request body ran past the namespace's size bound while it was being read. */
    public static ScimErrorException payloadTooLarge(String detail) {
        return new ScimErrorException(HttpStatus.CONTENT_TOO_LARGE, null, detail);
    }

    /**
     * An endpoint RFC 7644 defines and this service deliberately does not implement.
     *
     * <p>{@code 501} rather than {@code 404}: the path is SCIM's and a client probing for it is
     * told that this provider does not offer the operation, which RFC 7644 §3.12 lists as
     * {@code 501}'s meaning, rather than that it named a resource that does not exist.
     */
    public static ScimErrorException notImplemented(String detail) {
        return new ScimErrorException(HttpStatus.NOT_IMPLEMENTED, null, detail);
    }

    /**
     * A failure on this side that the request could not have avoided. {@code 500} and no
     * {@code scimType}, which RFC 7644 §3.12 defines none for.
     */
    public static ScimErrorException serverError(String detail) {
        return new ScimErrorException(HttpStatus.INTERNAL_SERVER_ERROR, null, detail);
    }

    public HttpStatus status() {
        return status;
    }

    /** The standard {@code scimType}, or {@code null} where the RFC defines none. */
    public String scimType() {
        return scimType;
    }

    /** The human-readable detail, safe by construction: never a submitted value. */
    public String detail() {
        return getMessage();
    }
}
