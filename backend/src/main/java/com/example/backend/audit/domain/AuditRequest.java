package com.example.backend.audit.domain;

/**
 * The triggering request, as much of it as an audit event may carry.
 *
 * <p>{@code pathTemplate} is the matched route pattern —
 * {@code /api/admin/accounts/{username}/disable} — and never the resolved URI.
 * That distinction is the whole reason this is a value type rather than a raw
 * string pulled from a servlet: the resolved path of every administrative
 * endpoint has the acted-on account's username inside it, so recording it would
 * put a username in an event body through the back door, past every field that
 * was carefully made a {@link java.util.UUID}.
 *
 * @param method       HTTP method, or {@code null} outside a request
 * @param pathTemplate matched route template, or {@code null} when no route
 *                     matched or none was resolved yet
 * @param requestId    correlation id minted for the request, or {@code null}
 *                     outside a request
 */
public record AuditRequest(String method, String pathTemplate, String requestId) {

    /** What a scheduled job or a startup task records: no request at all. */
    public static final AuditRequest NONE = new AuditRequest(null, null, null);
}
