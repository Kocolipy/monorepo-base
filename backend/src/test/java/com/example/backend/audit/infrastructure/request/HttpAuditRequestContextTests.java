package com.example.backend.audit.infrastructure.request;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.audit.domain.AuditRequest;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

/**
 * The one adapter that can see a resolved request URI, held to never passing one on.
 *
 * <p>This is where the redaction rule would be lost if it were lost anywhere: every
 * field on an audit event is a UUID or a value from a closed set, except the request
 * path — and the resolved path of every administrative endpoint has the acted-on
 * account's username inside it. So the assertion below is deliberately not "the
 * template is recorded" but "the URI is not", with a request whose URI and template
 * differ in exactly that way.
 */
class HttpAuditRequestContextTests {

    private static final String REQUEST_ID_ATTRIBUTE =
            "com.example.backend.observability.RequestIdFilter.requestId";

    private final HttpAuditRequestContext requests = new HttpAuditRequestContext();

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void theRouteTemplateIsRecordedAndTheResolvedUriIsNot() {
        bind(request("POST", "/api/admin/accounts/ada/disable",
                "/api/admin/accounts/{username}/disable", "req-7"));

        AuditRequest current = requests.current();

        assertThat(current.method()).isEqualTo("POST");
        assertThat(current.pathTemplate()).isEqualTo("/api/admin/accounts/{username}/disable");
        assertThat(current.requestId()).isEqualTo("req-7");
        // The username was in the URI and is in nothing this returned.
        assertThat(current.pathTemplate()).doesNotContain("ada");
        assertThat(String.valueOf(current)).doesNotContain("ada");
    }

    /**
     * No template resolved — a request refused by the filter chain before any
     * handler matched. The path is left empty rather than filled from the URI: a
     * missing correlation detail is a smaller loss than a retained identifier.
     */
    @Test
    void anUnmatchedRequestRecordsNoPathRatherThanTheUri() {
        bind(request("POST", "/api/admin/accounts/ada/disable", null, "req-8"));

        AuditRequest current = requests.current();

        assertThat(current.method()).isEqualTo("POST");
        assertThat(current.pathTemplate()).isNull();
        assertThat(current.requestId()).isEqualTo("req-8");
    }

    @Test
    void outsideARequestNothingIsRecorded() {
        assertThat(requests.current()).isEqualTo(AuditRequest.NONE);
        assertThat(AuditRequest.NONE.method()).isNull();
        assertThat(AuditRequest.NONE.pathTemplate()).isNull();
        assertThat(AuditRequest.NONE.requestId()).isNull();
    }

    @Test
    void aRequestWithNoCorrelationIdRecordsNone() {
        bind(request("GET", "/api/auth/me", "/api/auth/me", null));

        assertThat(requests.current().requestId()).isNull();
    }

    /**
     * An attribute that is not a string is treated as absent rather than coerced.
     * Spring puts a {@code PathPattern} rather than a {@code String} under the
     * best-matching-pattern key in some configurations, and a blind cast there would
     * fail the request that was only being audited.
     */
    @Test
    void anAttributeThatIsNotAStringIsTreatedAsAbsent() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, 42);
        request.setAttribute(REQUEST_ID_ATTRIBUTE, List.of("not-a-string"));
        bind(request);

        AuditRequest current = requests.current();

        assertThat(current.pathTemplate()).isNull();
        assertThat(current.requestId()).isNull();
        assertThat(current.method()).isEqualTo("GET");
    }

    private static MockHttpServletRequest request(
            String method, String uri, String template, String requestId) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        if (template != null) {
            request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, template);
        }
        if (requestId != null) {
            request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        }
        return request;
    }

    private static void bind(MockHttpServletRequest request) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
