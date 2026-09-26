package com.example.backend.scim.controller;

import com.example.backend.scim.domain.DuplicateUserNameException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Renders every refusal this slice's handlers produce as the one SCIM error document.
 *
 * <p>Scoped to this package by {@code basePackages}, deliberately. The application
 * chain's errors keep their existing shape — a SCIM error body on a browser request
 * would be a change to the SPA's contract — so this advice must not be global. What
 * that scoping costs is that a refusal raised BEFORE a handler is selected (an
 * unsupported method on a mapped path, an unsupported content type) is not rendered
 * here; those are refusals of the dispatcher rather than of these endpoints, and the
 * conformance-fixture ticket owns making the whole namespace's error surface uniform.
 *
 * <p>The body is built as an ordered map rather than a record so the field order is
 * {@code schemas}, {@code status}, {@code scimType}, {@code detail} as RFC 7644's
 * examples have it, and so {@code scimType} can be omitted entirely — rather than
 * rendered as null — where the RFC defines none for the condition.
 */
@RestControllerAdvice(basePackages = "com.example.backend.scim.controller")
class ScimExceptionHandler {

    /** Every refusal this slice raises deliberately. */
    @ExceptionHandler(ScimErrorException.class)
    ResponseEntity<Map<String, Object>> handle(ScimErrorException refusal) {
        return render(refusal);
    }

    /**
     * A {@code userName} already taken, translated at the boundary.
     *
     * <p>The domain exception carries no detail naming the value, and neither does this:
     * the caller knows which {@code userName} it sent, and echoing it back would put it
     * in a response body and in every log that records one.
     */
    @ExceptionHandler(DuplicateUserNameException.class)
    ResponseEntity<Map<String, Object>> handle(DuplicateUserNameException duplicate) {
        return render(ScimErrorException.uniqueness(
                "A User with the requested userName already exists."));
    }

    private static ResponseEntity<Map<String, Object>> render(ScimErrorException refusal) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("schemas", List.of(ScimSchemas.ERROR));
        // A string, not a number: RFC 7644 §3.12 defines status as a string, and a
        // conformance client that parses it as one fails on an integer.
        body.put("status", String.valueOf(refusal.status().value()));
        if (refusal.scimType() != null) {
            body.put("scimType", refusal.scimType());
        }
        body.put("detail", refusal.detail());
        return ResponseEntity.status(refusal.status())
                .header("Content-Type", ScimSchemas.MEDIA_TYPE)
                .body(body);
    }
}
