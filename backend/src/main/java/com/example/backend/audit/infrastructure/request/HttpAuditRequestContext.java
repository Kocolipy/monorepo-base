package com.example.backend.audit.infrastructure.request;

import com.example.backend.audit.domain.AuditRequest;
import com.example.backend.audit.domain.AuditRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Reads the request an audit event is being recorded under out of the servlet
 * container.
 *
 * <p>The one decision worth reading here is that the recorded path is the matched
 * route <strong>template</strong> —
 * {@code /api/admin/accounts/{username}/disable} — and never
 * {@code request.getRequestURI()}. The resolved URI of every administrative
 * endpoint carries the acted-on account's username inside it, so recording it
 * would put a username in an event body past every field that was deliberately
 * made a UUID. When no template is available the field is left empty rather than
 * filled from the URI: a missing correlation detail is a smaller loss than a
 * retained identifier.
 *
 * <p>The correlation id is read from the attribute {@code RequestIdFilter} stashes
 * it on rather than from the logging context, so this adapter needs no access to
 * MDC — which only {@code LogContext} is allowed to touch.
 */
@Component
class HttpAuditRequestContext implements AuditRequestContext {

    /**
     * Where {@code RequestIdFilter} keeps the id it minted. Spelled out rather
     * than imported because the filter's own constant is package-private, and
     * widening it so one adapter can read it would widen it for everything.
     */
    private static final String REQUEST_ID_ATTRIBUTE =
            "com.example.backend.observability.RequestIdFilter.requestId";

    @Override
    public AuditRequest current() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return AuditRequest.NONE;
        }
        return new AuditRequest(
                request.getMethod(),
                attribute(request, HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE),
                attribute(request, REQUEST_ID_ATTRIBUTE));
    }

    private static HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes()
                        instanceof ServletRequestAttributes servlet
                ? servlet.getRequest()
                : null;
    }

    private static String attribute(HttpServletRequest request, String name) {
        return request.getAttribute(name) instanceof String value ? value : null;
    }
}
