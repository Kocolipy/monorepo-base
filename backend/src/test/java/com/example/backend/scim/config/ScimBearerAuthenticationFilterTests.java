package com.example.backend.scim.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.auth.MutableClock;
import com.example.backend.scim.InMemoryScimConnectorRepository;
import com.example.backend.scim.InMemoryScimConnectorTokenRepository;
import com.example.backend.scim.application.ConnectorAuthenticationService;
import com.example.backend.scim.domain.AuthenticatedConnector;
import com.example.backend.scim.domain.ConnectorTokenScope;
import com.example.backend.scim.domain.ConnectorTokenSecret;
import com.example.backend.scim.domain.ScimConnector;
import com.example.backend.scim.domain.ScimConnectorToken;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The SCIM bearer filter, driven with servlet mocks.
 *
 * <p>Two families of claim. What a credential gets — accepted, {@code invalid_token},
 * {@code insufficient_scope}, or passed along as absent — and, more importantly, that a
 * token placed anywhere other than the {@code Authorization} header is not found at
 * all. The second family is asserted by presenting a genuinely valid token through a
 * query string, a form body and a cookie in turn: each one would authenticate if the
 * filter looked there, so a passing test is evidence the filter does not.
 */
class ScimBearerAuthenticationFilterTests {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final InMemoryScimConnectorRepository connectors =
            new InMemoryScimConnectorRepository();

    private final InMemoryScimConnectorTokenRepository tokens =
            new InMemoryScimConnectorTokenRepository();

    private final MutableClock clock = new MutableClock(NOW);

    private ScimBearerAuthenticationFilter filter;

    private ScimConnector connector;

    private MockHttpServletResponse response;

    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new ScimBearerAuthenticationFilter(
                new ConnectorAuthenticationService(connectors, tokens, clock));
        connector = connectors.seed("Okta", NOW);
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void a_valid_write_token_authenticates_and_the_request_proceeds() throws Exception {
        String value = mint(ConnectorTokenScope.READ_WRITE);
        MockHttpServletRequest request = scimRequest("GET", "/scim/v2/Users");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + value);

        Authentication authentication = authenticationSeenBy(request);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal())
                .isEqualTo(new AuthenticatedConnector(
                        connector.id(), tokens.all().getFirst().id(),
                        ConnectorTokenScope.READ_WRITE));
        assertThat(authorities(authentication))
                .containsExactlyInAnyOrder("SCOPE_scim.read", "SCOPE_scim.write");
    }

    @Test
    void a_read_only_token_authenticates_with_the_read_authority_alone() throws Exception {
        String value = mint(ConnectorTokenScope.READ_ONLY);
        MockHttpServletRequest request = scimRequest("GET", "/scim/v2/Users");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + value);

        assertThat(authorities(authenticationSeenBy(request)))
                .containsExactly("SCOPE_scim.read");
    }

    /** The presented value must not be carried past its verification. */
    @Test
    void the_authentication_carries_no_credentials() throws Exception {
        String value = mint(ConnectorTokenScope.READ_WRITE);
        MockHttpServletRequest request = scimRequest("GET", "/scim/v2/Users");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + value);

        assertThat(authenticationSeenBy(request).getCredentials()).isNull();
    }

    /** The thread-local must not survive, or a pooled thread would carry the authority on. */
    @Test
    void the_security_context_is_cleared_once_the_request_is_served() throws Exception {
        String value = mint(ConnectorTokenScope.READ_WRITE);
        MockHttpServletRequest request = scimRequest("GET", "/scim/v2/Users");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + value);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void no_authorization_header_is_passed_along_for_the_chain_to_challenge() throws Exception {
        filter.doFilter(scimRequest("GET", "/scim/v2/Users"), response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isNull();
    }

    @Test
    void another_authentication_scheme_is_treated_as_no_credential() throws Exception {
        MockHttpServletRequest request = scimRequest("GET", "/scim/v2/Users");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void an_empty_bearer_value_is_treated_as_no_credential() throws Exception {
        MockHttpServletRequest request = scimRequest("GET", "/scim/v2/Users");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer    ");

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void a_malformed_bearer_value_is_an_invalid_token() throws Exception {
        MockHttpServletRequest request = scimRequest("GET", "/scim/v2/Users");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer not-a-token-at-all");

        filter.doFilter(request, response, chain);

        assertInvalidToken();
    }

    @Test
    void an_expired_token_is_an_invalid_token() throws Exception {
        String value = mint(ConnectorTokenScope.READ_WRITE, NOW.plus(Duration.ofDays(1)));
        clock.advanceBy(Duration.ofDays(2));
        MockHttpServletRequest request = scimRequest("GET", "/scim/v2/Users");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + value);

        filter.doFilter(request, response, chain);

        assertInvalidToken();
    }

    @Test
    void a_revoked_token_is_an_invalid_token() throws Exception {
        String value = mint(ConnectorTokenScope.READ_WRITE);
        tokens.save(tokens.all().getFirst().revoked(NOW));
        MockHttpServletRequest request = scimRequest("GET", "/scim/v2/Users");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + value);

        filter.doFilter(request, response, chain);

        assertInvalidToken();
    }

    /** Both refusals carry the same header, so nothing distinguishes the reasons. */
    @Test
    void every_rejected_token_gets_the_same_refusal() throws Exception {
        String revokedValue = mint(ConnectorTokenScope.READ_WRITE);
        tokens.save(tokens.all().getFirst().revoked(NOW));

        MockHttpServletResponse toRevoked = new MockHttpServletResponse();
        MockHttpServletRequest revoked = scimRequest("GET", "/scim/v2/Users");
        revoked.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + revokedValue);
        filter.doFilter(revoked, toRevoked, new MockFilterChain());

        MockHttpServletResponse toUnknown = new MockHttpServletResponse();
        MockHttpServletRequest unknown = scimRequest("GET", "/scim/v2/Users");
        unknown.addHeader(HttpHeaders.AUTHORIZATION, "Bearer nosuch.lookup");
        filter.doFilter(unknown, toUnknown, new MockFilterChain());

        assertThat(toRevoked.getStatus()).isEqualTo(toUnknown.getStatus());
        assertThat(toRevoked.getHeader(HttpHeaders.WWW_AUTHENTICATE))
                .isEqualTo(toUnknown.getHeader(HttpHeaders.WWW_AUTHENTICATE));
    }

    @Test
    void a_read_only_token_mutating_is_refused_for_insufficient_scope() throws Exception {
        String value = mint(ConnectorTokenScope.READ_ONLY);
        MockHttpServletRequest request = scimRequest("POST", "/scim/v2/Users");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + value);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE))
                .isEqualTo("Bearer error=\"insufficient_scope\"");
        assertThat(chain.getRequest()).as("the handler must not run").isNull();
    }

    @Test
    void a_read_only_token_may_call_a_search_post() throws Exception {
        String value = mint(ConnectorTokenScope.READ_ONLY);
        MockHttpServletRequest request = scimRequest("POST", "/scim/v2/Users/.search");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + value);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void a_write_token_may_mutate() throws Exception {
        String value = mint(ConnectorTokenScope.READ_WRITE);
        MockHttpServletRequest request = scimRequest("POST", "/scim/v2/Users");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + value);

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    /**
     * A query-string token is not merely rejected, it is not looked for. Each of the
     * three cases below presents a token that authenticates perfectly well in the
     * {@code Authorization} header, so the request arriving unauthenticated is evidence
     * the filter never read that location.
     */
    @Test
    void a_token_in_the_query_string_is_not_a_credential() throws Exception {
        String value = mint(ConnectorTokenScope.READ_WRITE);
        MockHttpServletRequest request = scimRequest("GET", "/scim/v2/Users");
        request.setQueryString("access_token=" + value);
        request.setParameter("access_token", value);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void a_token_in_a_form_body_is_not_a_credential() throws Exception {
        String value = mint(ConnectorTokenScope.READ_WRITE);
        MockHttpServletRequest request = scimRequest("POST", "/scim/v2/Users");
        request.setContentType("application/x-www-form-urlencoded");
        request.setParameter("access_token", value);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void a_token_in_a_cookie_is_not_a_credential() throws Exception {
        String value = mint(ConnectorTokenScope.READ_WRITE);
        MockHttpServletRequest request = scimRequest("GET", "/scim/v2/Users");
        request.setCookies(new Cookie("access_token", value));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    /**
     * The scope rule sees the path WITHIN the application, not the deployed URI.
     *
     * <p>Every other test here runs at the container root, where stripping the context
     * path is the identity function — so without this case the strip could be deleted
     * and nothing would notice. Deployed under a context path it matters: the rule
     * decides "is this a `.search`" from a path suffix, and a raw
     * {@code /app/scim/v2/Users/.search} still ends in the suffix, but a raw
     * {@code /app} prefix would reach any future rule that reasons about the path's
     * START.
     */
    @Test
    void the_scope_rule_sees_the_path_within_the_application_not_the_deployed_uri()
            throws Exception {
        String value = mint(ConnectorTokenScope.READ_ONLY);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/app/scim/v2/Users");
        request.setContextPath("/app");
        request.setRequestURI("/app/scim/v2/Users");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + value);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(chain.getRequest()).isNull();

        MockHttpServletResponse searchResponse = new MockHttpServletResponse();
        MockFilterChain searchChain = new MockFilterChain();
        MockHttpServletRequest search =
                new MockHttpServletRequest("POST", "/app/scim/v2/Users/.search");
        search.setContextPath("/app");
        search.setRequestURI("/app/scim/v2/Users/.search");
        search.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + value);

        filter.doFilter(search, searchResponse, searchChain);

        assertThat(searchResponse.getStatus()).isEqualTo(200);
        assertThat(searchChain.getRequest()).isNotNull();
    }

    /** Each challenge writes exactly the header RFC 6750 names, and no body. */
    @Test
    void the_three_challenges_are_the_headers_rfc_6750_names() throws IOException {
        MockHttpServletResponse missing = new MockHttpServletResponse();
        ScimBearerChallenge.missingCredential(missing);
        assertThat(missing.getStatus()).isEqualTo(401);
        assertThat(missing.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
        assertThat(missing.getContentAsString()).isEmpty();

        MockHttpServletResponse invalid = new MockHttpServletResponse();
        ScimBearerChallenge.invalidToken(invalid);
        assertThat(invalid.getStatus()).isEqualTo(401);
        assertThat(invalid.getHeader(HttpHeaders.WWW_AUTHENTICATE))
                .isEqualTo("Bearer error=\"invalid_token\"");

        MockHttpServletResponse scope = new MockHttpServletResponse();
        ScimBearerChallenge.insufficientScope(scope);
        assertThat(scope.getStatus()).isEqualTo(403);
        assertThat(scope.getHeader(HttpHeaders.WWW_AUTHENTICATE))
                .isEqualTo("Bearer error=\"insufficient_scope\"");
    }

    private void assertInvalidToken() {
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE))
                .isEqualTo("Bearer error=\"invalid_token\"");
        assertThat(chain.getRequest()).as("the handler must not run").isNull();
    }

    /**
     * The authentication as the FILTERED request saw it, captured from inside the
     * chain. Read there rather than after {@code doFilter} returns, because the filter
     * clears the thread-local on its way out — asserting afterwards would see null and
     * prove nothing.
     */
    private Authentication authenticationSeenBy(MockHttpServletRequest request)
            throws ServletException, IOException {
        Authentication[] captured = new Authentication[1];
        chain = new MockFilterChain() {
            @Override
            public void doFilter(
                    jakarta.servlet.ServletRequest servletRequest,
                    jakarta.servlet.ServletResponse servletResponse)
                    throws IOException, ServletException {
                captured[0] = SecurityContextHolder.getContext().getAuthentication();
                super.doFilter(servletRequest, servletResponse);
            }
        };
        filter.doFilter(request, response, chain);
        return captured[0];
    }

    private static Iterable<String> authorities(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    private static MockHttpServletRequest scimRequest(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRequestURI(path);
        return request;
    }

    private String mint(ConnectorTokenScope scope) {
        return mint(scope, NOW.plus(Duration.ofDays(365)));
    }

    private String mint(ConnectorTokenScope scope, Instant expiresAt) {
        ConnectorTokenSecret.Minted minted =
                ConnectorTokenSecret.mint(new SecureRandom());
        tokens.save(ScimConnectorToken.issue(
                UUID.randomUUID(),
                connector.id(),
                minted.lookupId(),
                minted.digest(),
                scope,
                NOW,
                expiresAt));
        return minted.presentedValue();
    }
}
