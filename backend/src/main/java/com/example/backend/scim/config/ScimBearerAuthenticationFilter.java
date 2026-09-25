package com.example.backend.scim.config;

import com.example.backend.scim.application.ConnectorAuthenticationService;
import com.example.backend.scim.domain.AuthenticatedConnector;
import com.example.backend.scim.domain.ScimWriteScopeRule;
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
 * <p><strong>Scope is enforced here rather than in a handler.</strong> A read-only
 * token attempting a mutation is turned away before any handler runs, from the method
 * and the path alone ({@link ScimWriteScopeRule}). A per-handler check would cover
 * the handlers whose author remembered it; this covers every SCIM path that exists
 * and every one that will, including the mutating endpoints later tickets add.
 *
 * <p><strong>Three outcomes, and the missing-credential one is not this filter's.</strong>
 * A request with no {@code Authorization} header is passed along unauthenticated, so
 * the chain's own entry point answers it with the challenge — which keeps "what an
 * unauthenticated SCIM request gets" in the chain configuration beside every other
 * access rule. A malformed, unknown, expired or revoked token is
 * {@code invalid_token} here, because the chain cannot tell those from a missing
 * credential. A read-only token on a mutating path is {@code insufficient_scope}.
 */
class ScimBearerAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    /** Authority granted to every authenticated connector. Write implies read. */
    private static final String READ_AUTHORITY = "SCOPE_scim.read";

    private static final String WRITE_AUTHORITY = "SCOPE_scim.write";

    private final ConnectorAuthenticationService connectors;

    ScimBearerAuthenticationFilter(ConnectorAuthenticationService connectors) {
        this.connectors = connectors;
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
            ScimBearerChallenge.invalidToken(response);
            return;
        }

        if (ScimWriteScopeRule.requiresWriteScope(request.getMethod(), path(request))
                && !connector.get().scope().permitsWrite()) {
            ScimBearerChallenge.insufficientScope(response);
            return;
        }

        authenticate(connector.get());
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
        return connector.scope().permitsWrite()
                ? List.of(
                        new SimpleGrantedAuthority(READ_AUTHORITY),
                        new SimpleGrantedAuthority(WRITE_AUTHORITY))
                : List.of(new SimpleGrantedAuthority(READ_AUTHORITY));
    }
}
