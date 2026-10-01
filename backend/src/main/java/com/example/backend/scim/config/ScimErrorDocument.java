package com.example.backend.scim.config;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Writes a SCIM error document from inside the namespace's filter chain, where no controller advice
 * runs.
 *
 * <p>The advice in {@code scim.controller} renders every refusal a handler raises. These are the
 * refusals that happen before or instead of a handler — a body over the size bound, a path no
 * endpoint serves, a method or content type an endpoint does not accept — and a SCIM client is owed
 * the same document shape for them, not the servlet container's error page or an empty body.
 *
 * <p>Only a literal written by this class reaches {@code detail}: a refusal at this layer happens
 * before anything a caller sent has been understood, so there is nothing safe to quote, and the
 * messages the dispatcher attaches to its own refusals quote the request.
 */
final class ScimErrorDocument {

    private static final String MEDIA_TYPE = "application/scim+json";

    private ScimErrorDocument() {
    }

    /** Writes the document with {@code status}; {@code scimType} is never set at this layer. */
    static void write(HttpServletResponse response, int status) throws IOException {
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
}
