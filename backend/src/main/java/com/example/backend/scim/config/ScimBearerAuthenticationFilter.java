package com.example.backend.scim.config;

import com.example.backend.audit.domain.AuditRequest;
import com.example.backend.audit.domain.AuditTrail;
import com.example.backend.observability.AccessRefusalLog;
import com.example.backend.observability.AccessRefusalLog.Refusal;
import com.example.backend.observability.LogContext;
import com.example.backend.observability.MetricTag;
import com.example.backend.observability.RequestActor;
import com.example.backend.observability.RequestIdFilter;
import com.example.backend.observability.RouteTemplates;
import com.example.backend.scim.application.ConnectorAuthenticationService;
import com.example.backend.scim.domain.AuthenticatedConnector;
import com.example.backend.scim.domain.ScimPermissionRule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates a SCIM request by its bearer token, and refuses a mutating request
 * made with a read-only one.
 *
 * <p><strong>The header, and nothing but the header.</strong> The presented value is
 * read from {@code Authorization} alone. This filter never calls
 * {@code getParameter}, {@code getParameterMap} or {@code getCookies}, so a token in
 * a query string, a form body or a cookie is not rejected by a check that could be
 * forgotten — it is simply never looked for. Which matters because each of those
 * three carries the credential somewhere it outlives the request: a query string
 * into access logs, browser history and {@code Referer} headers; a form body into
 * the same logs when a proxy records bodies; a cookie into every subsequent
 * same-origin request, including ones a page the connector's operator visited caused.
 * {@code semgrep/rules/service-security.yml} holds the property against future code
 * in this package.
 *
 * <p><strong>Permissions are enforced here rather than in a handler.</strong> A token lacking
 * the Permission a request needs is turned away before any handler runs, from the method and the
 * path alone ({@link ScimPermissionRule}). A per-handler check would cover the handlers whose
 * author remembered it; this covers every SCIM path that exists, and refuses every one the rule
 * does not name. The refusal is logged and audited with the connector, the operation and the one
 * generic reason, never the Permission that was missing.
 *
 * <p><strong>Three outcomes, and the missing-credential one is not this filter's.</strong>
 * A request with no {@code Authorization} header is passed along unauthenticated, so
 * the chain's own entry point answers it with the challenge — which keeps "what an
 * unauthenticated SCIM request gets" in the chain configuration beside every other
 * access rule. A malformed, unknown, expired or revoked token is
 * {@code invalid_token} here, because the chain cannot tell those from a missing
 * credential. A valid token lacking the request's Permission is {@code insufficient_scope},
 * RFC 6750's name for exactly that.
 */
class ScimBearerAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final ConnectorAuthenticationService connectors;

    private final AccessRefusalLog refusals;

    private final AuditTrail audit;

    private final RouteTemplates routes;

    ScimBearerAuthenticationFilter(
            ConnectorAuthenticationService connectors,
            AccessRefusalLog refusals,
            AuditTrail audit,
            RouteTemplates routes) {
        this.connectors = connectors;
        this.refusals = refusals;
        this.audit = audit;
        this.routes = routes;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<String> presented = presentedValue(request);
        if (presented.isEmpty()) {
            // No credential of the one accepted shape. The chain's entry point
            // issues the challenge; a token sent any other way arrives here as
            // "no credential" because nothing looked for it.
            chain.doFilter(request, response);
            return;
        }

        Optional<AuthenticatedConnector> connector = connectors.authenticate(presented.get());
        if (connector.isEmpty()) {
            // The reason is a fixed word. Nothing of the presented value — not even a prefix,
            // which is how a token is recognised in a leak — reaches the record.
            refusals.record(request, Refusal.BEARER_INVALID);
            ScimBearerChallenge.invalidToken(response);
            return;
        }

        // From here every record of the request names the connector, by its non-secret id —
        // the refusal just below among them, and the request record written outside the chain.
        RequestActor.connector(request, connector.get().connectorId());
        try (LogContext.Scope scope =
                LogContext.connectorId(connector.get().connectorId().toString())) {
            if (!ScimPermissionRule.permits(
                    request.getMethod(), path(request), connector.get().permissions())) {
                if (refusals.record(request, Refusal.INSUFFICIENT_PERMISSIONS)) {
                    // Only when the log took it, so one exchange is one record of each kind.
                    audit.recordConnectorAccessDenied(
                            connector.get().connectorId(),
                            new AuditRequest(RequestIdFilter.method(request), routes.of(request),
                                    RequestIdFilter.requestId(request)));
                }
                ScimBearerChallenge.insufficientPermissions(response);
                return;
            }
            proceedAs(connector.get(), request, response, chain);
        }
    }

    private static void proceedAs(
            AuthenticatedConnector connector,
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        authenticate(connector);
        // Traffic per connector, by its non-secret id. Recorded here because this is
        // the only place that knows it: the security context is cleared below, before
        // the request metric is taken.
        MetricTag.record(
                request, MetricTag.SCIM_CONNECTOR, connector.connectorId().toString());
        try {
            chain.doFilter(request, response);
        } finally {
            // The chain is stateless and nothing persists this context, so the
            // thread-local is the only place it lives. Clearing it here rather than
            // relying on the container is what keeps one connector's authority from
            // reaching the next request served by a pooled thread.
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * The presented value, or empty when there is no {@code Authorization: Bearer}
     * header. An {@code Authorization} header of another scheme is treated as absent
     * rather than as malformed: it is a client configured for some other service, and
     * the challenge tells it which scheme this one wants.
     */
    private static Optional<String> presentedValue(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        String value = header.substring(BEARER_PREFIX.length()).trim();
        return value.isEmpty() ? Optional.empty() : Optional.of(value);
    }

    private static String path(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    /**
     * Puts the connector in the security context as an already-authenticated
     * principal.
     *
     * <p>{@link PreAuthenticatedAuthenticationToken} because the credential was
     * verified before this point: there is no {@code AuthenticationManager} in this
     * chain to hand it to, and introducing one would mean a second place a SCIM
     * request could be authenticated. Credentials are {@code null} — the presented
     * value is not carried past its verification, so nothing downstream can log or
     * re-present it.
     */
    private static void authenticate(AuthenticatedConnector connector) {
        PreAuthenticatedAuthenticationToken authentication =
                new PreAuthenticatedAuthenticationToken(connector, null, authorities(connector));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    private static List<SimpleGrantedAuthority> authorities(AuthenticatedConnector connector) {
        // The token's Permissions, spelled as a session's are. Nothing on this chain decides on
        // them — the rule above already has — but a principal whose authorities said otherwise
        // would be a second, disagreeing account of what the request may do.
        return connector.permissions().sortedValues().stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }
}
