package com.example.backend.scim.config;

import com.example.backend.observability.LogEvent;
import com.example.backend.observability.LogEvent.Category;
import com.example.backend.observability.LogEvent.Operation;
import com.example.backend.observability.LogEvent.Type;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes a SCIM error document from inside the namespace's filter chain, where no controller advice
 * runs, and gives the refusal its one log record.
 *
 * <p>The advice in {@code scim.controller} renders every refusal a handler raises. These are the
 * refusals that happen before or instead of a handler — a body over the size bound, a path no
 * endpoint serves, a method or content type an endpoint does not accept — and a SCIM client is owed
 * the same document shape for them, not the servlet container's error page or an empty body.
 *
 * <p>Only a literal written by this class reaches {@code detail}: a refusal at this layer happens
 * before anything a caller sent has been understood, so there is nothing safe to quote, and the
 * messages the dispatcher attaches to its own refusals quote the request.
 *
 * <h2>The record</h2>
 *
 * <p>The same {@code scim.refusal} record the advice writes for a handler's refusal — {@code WARN}
 * for the caller's error, {@code ERROR} for one on this side — so a SCIM refusal has one shape in
 * the log stream as well as on the wire, whichever layer produced it. Its reason is a word from
 * {@link #reason(int)}'s fixed set, chosen by the status alone; nothing the caller sent reaches it.
 * The record carries no resource type: at this layer no handler was matched, so no route names one.
 */
final class ScimErrorDocument {

    private static final String MEDIA_TYPE = "application/scim+json";

    private static final Logger log = LoggerFactory.getLogger(ScimErrorDocument.class);

    private ScimErrorDocument() {
    }

    /**
     * Records the refusal, then writes the document with {@code status}; {@code scimType} is never
     * set at this layer.
     */
    static void write(HttpServletResponse response, int status) throws IOException {
        record(status);
        response.setStatus(status);
        response.setContentType(MEDIA_TYPE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {"schemas":["urn:ietf:params:scim:api:messages:2.0:Error"],\
                "status":"%d",\
                "detail":"%s"}""".formatted(status, detail(status)));
    }

    /** A fixed sentence per status. Never a message carrying request content. */
    static String detail(int status) {
        return switch (status) {
            case HttpServletResponse.SC_NOT_FOUND -> "No SCIM endpoint is served at this path.";
            case HttpServletResponse.SC_METHOD_NOT_ALLOWED ->
                    "This SCIM endpoint does not support that HTTP method.";
            case HttpServletResponse.SC_NOT_ACCEPTABLE ->
                    "This SCIM endpoint responds with application/scim+json or application/json"
                            + " only.";
            case HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE ->
                    "The request body exceeds the 1 MiB limit.";
            case HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE ->
                    "A SCIM request body must be application/scim+json or application/json.";
            default -> "The request could not be served.";
        };
    }

    /**
     * The record's {@code event.reason}: one word per status, spelled as the advice spells the
     * same refusal ({@code notFound}, {@code payloadTooLarge}), so a refusal is searchable by one
     * reason whichever layer answered it.
     */
    static String reason(int status) {
        return switch (status) {
            case HttpServletResponse.SC_NOT_FOUND -> "notFound";
            case HttpServletResponse.SC_METHOD_NOT_ALLOWED -> "methodNotAllowed";
            case HttpServletResponse.SC_NOT_ACCEPTABLE -> "notAcceptable";
            case HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE -> "payloadTooLarge";
            case HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE -> "unsupportedMediaType";
            default -> status >= 500 ? "serverError" : "requestRefused";
        };
    }

    private static void record(int status) {
        boolean fault = status >= 500;
        LogEvent.classify(fault ? log.atError() : log.atWarn(),
                        Operation.SCIM_REFUSAL, Category.PROCESS, fault ? Type.ERROR : Type.DENIED)
                .addKeyValue(LogEvent.OUTCOME, LogEvent.FAILURE)
                .addKeyValue(LogEvent.REASON, reason(status))
                .addKeyValue(LogEvent.HTTP_STATUS_CODE, status)
                .log("SCIM request refused");
    }
}
