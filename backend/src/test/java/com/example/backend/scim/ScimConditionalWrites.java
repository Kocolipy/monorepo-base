package com.example.backend.scim;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Test support for driving SCIM writes the way a well-behaved connector does: read the
 * resource's current version, then write with it in {@code If-Match}.
 *
 * <p>Exists so the tests of what a PUT, PATCH or DELETE DOES exercise the conditional path a
 * well-behaved connector takes — they are about the write, not the precondition, which is
 * optional. The tests of the precondition itself do not use this; they set, omit and corrupt
 * the header deliberately.
 */
public final class ScimConditionalWrites {

    private static final List<String> CONDITIONAL = List.of("PUT", "PATCH", "DELETE");

    private ScimConditionalWrites() {
    }

    /**
     * Adds {@code If-Match} with the targeted resource's stored version to a PUT, PATCH or DELETE
     * on {@code .../{id}}, unless the request already carries one. Any other request, or an id the
     * database does not hold, is passed through unchanged, so the write is refused exactly as it
     * would have been.
     */
    public static RequestPostProcessor currentVersion(JdbcTemplate jdbc) {
        return request -> {
            if (!CONDITIONAL.contains(request.getMethod())
                    || request.getHeader(HttpHeaders.IF_MATCH) != null) {
                return request;
            }
            String uri = request.getRequestURI();
            String last = uri.substring(uri.lastIndexOf('/') + 1);
            UUID id;
            try {
                id = UUID.fromString(last);
            } catch (IllegalArgumentException notAnId) {
                return request;
            }
            List<Long> versions = jdbc.queryForList(
                    "SELECT version FROM scim_resources WHERE id = ?", Long.class, id);
            if (!versions.isEmpty()) {
                request.addHeader(HttpHeaders.IF_MATCH, "\"" + versions.get(0) + "\"");
            }
            return request;
        };
    }
}
