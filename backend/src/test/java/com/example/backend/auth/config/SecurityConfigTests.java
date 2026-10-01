package com.example.backend.auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@Import(com.example.backend.ContainerTestConfiguration.class)
// Every assertion here reads immutable configuration beans (the filter chain and
// the CSRF / security-context repositories) and builds its own local MockMvc per
// method, so nothing here mutates the application context. The context is
// therefore shared with the other @SpringBootTest classes via the context cache
// rather than rebuilt per method — booting it once instead of eleven times.
class SecurityConfigTests {

    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";

    /**
     * The application chain by name. There are two {@link SecurityFilterChain}
     * beans in the context now — the SCIM chain is ordered ahead of this one, and
     * the release gate that used to keep it out of the context defaults to OPEN —
     * so the type alone no longer names a bean. Which of the two comes first is
     * {@code ScimSecurityChainOrderTests}' subject; this class is about what the
     * application chain itself builds, so it asks for that chain explicitly rather
     * than for whichever one the container happens to hand over.
     */
    @Autowired
    @Qualifier("securityFilterChain")
    private SecurityFilterChain securityFilterChain;

    @Autowired
    private SecurityContextRepository securityContextRepository;

    @Autowired
    private CsrfTokenRepository csrfTokenRepository;

    private MockMvc mvc;

    /**
     * Drives requests through the configured chain alone. The probe controller
     * only exists to give the frontend path a handler, so the assertions can look
     * at what the chain adds to a normal response.
     */
    @BeforeEach
    void setUp() {
        FilterChainProxy proxy = new FilterChainProxy(securityFilterChain);
        proxy.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .addFilter(proxy)
                .build();
    }

    /**
     * The login path writes the authentication through the injected
     * {@link SecurityContextRepository} bean; every subsequent request reads it
     * back through whichever repository the filter chain holds. Those must be
     * the same instance, otherwise the two halves of authentication are only
     * coupled by the incidental fact that both happen to touch the HTTP session.
     */
    @Test
    void filterChainReadsTheSameSecurityContextRepositoryThatLoginWritesTo() {
        SecurityContextHolderFilter filter = securityFilterChain.getFilters().stream()
                .filter(SecurityContextHolderFilter.class::isInstance)
                .map(SecurityContextHolderFilter.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "no SecurityContextHolderFilter in the chain: "
                                + securityFilterChain.getFilters().stream()
                                        .map(Filter::getClass)
                                        .map(Class::getSimpleName)
                                        .toList()));

        Object chainRepository =
                ReflectionTestUtils.getField(filter, "securityContextRepository");

        assertThat(chainRepository).isSameAs(securityContextRepository);
    }

    /**
     * An unsafe request carrying no CSRF token is rejected before authentication
     * is even considered, which is what distinguishes 403 here from the 401 the
     * same request earns once the token is present.
     */
    @Test
    void unsafeRequestWithoutACsrfTokenIsForbidden() throws Exception {
        mvc.perform(post("/api/session"))
                .andExpect(status().isForbidden());
    }

    /**
     * The token the shared repository mints is the token the chain accepts: the
     * raw cookie value is echoed back in the header, and the request gets as far
     * as the authorization check, which turns it away with a 401 instead.
     */
    @Test
    void unsafeRequestWithAMatchingCsrfTokenPassesCsrfAndReachesAuthorization() throws Exception {
        CsrfToken token = csrfTokenRepository.generateToken(new MockHttpServletRequest());

        mvc.perform(post("/api/session")
                        .cookie(new Cookie(CSRF_COOKIE, token.getToken()))
                        .header(CSRF_HEADER, token.getToken()))
                .andExpect(status().isUnauthorized());
    }

    /**
     * A single-page application cannot read an HttpOnly cookie, and it has no
     * server-rendered page to take a token from, so an anonymous GET has to be
     * enough to obtain one.
     */
    @Test
    void anonymousFrontendRequestIsHandedACsrfTokenCookie() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(CSRF_COOKIE))
                .andExpect(cookie().httpOnly(CSRF_COOKIE, false));
    }

    /**
     * The frontend allowance is a deny-list over everything that is not an API or
     * actuator path, so the actuator base path itself has to stay behind
     * authentication rather than fall through to it.
     */
    @Test
    void actuatorBasePathIsNotTreatedAsAFrontendRoute() throws Exception {
        mvc.perform(get("/actuator"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Whether a path is reserved for the server is decided inside the context path: deployed
     * under {@code /app}, {@code /app/api/count} is still an API path and stays behind
     * authentication, while {@code /app/} is still the frontend and stays public. Judged on the
     * raw URI instead, the first would read as a frontend route and be let through. An API path
     * that no earlier rule names is used on purpose, so the frontend allowance is what decides
     * it rather than a matcher ahead of it.
     */
    @Test
    void theFrontendAllowanceIsJudgedInsideTheContextPath() throws Exception {
        mvc.perform(get("/app/api/count").contextPath("/app"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/app/").contextPath("/app"))
                .andExpect(status().isOk());
    }

    /**
     * Only a GET of a frontend path skips authentication. An unsafe request to the same path —
     * past CSRF, so the chain's authorization decision is what answers — is refused with a 401
     * rather than reaching a handler.
     */
    @Test
    void aFrontendPathIsPublicForGetOnly() throws Exception {
        CsrfToken token = csrfTokenRepository.generateToken(new MockHttpServletRequest());

        mvc.perform(post("/")
                        .cookie(new Cookie(CSRF_COOKIE, token.getToken()))
                        .header(CSRF_HEADER, token.getToken()))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Account administration is one namespace, {@code /api/admin/**}, and these
     * three cover the whole rule: refused for a non-admin, allowed for an admin,
     * and — because the chain answers before any handler — unauthorized rather
     * than forbidden for a caller with no session at all. The path asserted is
     * the one {@link com.example.backend.auth.controller.AdminAccountController}
     * really maps, so the rule is proven against the endpoint it protects.
     */
    @Test
    void userRoleCannotReachTheAdminNamespace() throws Exception {
        mvc.perform(get("/api/admin/accounts").session(authenticatedSession("ROLE_USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRoleCanReachTheAdminNamespace() throws Exception {
        mvc.perform(get("/api/admin/accounts").session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousCallerCannotReachTheAdminNamespace() throws Exception {
        mvc.perform(get("/api/admin/accounts"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Every actuator endpoint but health is Admin-only: the telemetry scrape describes the
     * whole service's traffic. Proven here against the chain alone;
     * {@code OperationalTelemetryIntegrationTests} proves it against the real scrape.
     */
    @Test
    void userRoleCannotReachActuatorEndpointsOtherThanHealth() throws Exception {
        mvc.perform(get("/actuator/info").session(authenticatedSession("ROLE_USER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/actuator/prometheus").session(authenticatedSession("ROLE_USER")))
                .andExpect(status().isForbidden());
    }

    /**
     * Admitted, which in this class reads as {@code 404}: the chain is driven standalone in
     * front of a probe controller that maps no actuator handler, so a request the chain lets
     * through finds nothing — where a refused one never gets that far (401 or 403 above).
     */
    @Test
    void adminRoleCanReachActuatorEndpoints() throws Exception {
        mvc.perform(get("/actuator/info").session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/actuator/prometheus").session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void healthStaysPublicWhileTheRestOfActuatorIsAdminOnly() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/actuator/info"))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpSession authenticatedSession(String authority) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new TestingAuthenticationToken("account", null, authority));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context);
        return session;
    }

    @Test
    void responsesCarryTheContentSecurityPolicy() throws Exception {
        mvc.perform(get("/"))
                .andExpect(header().string("Content-Security-Policy",
                        containsString("default-src 'self'")))
                .andExpect(header().string("Content-Security-Policy",
                        containsString("frame-ancestors 'none'")))
                .andExpect(header().string("Content-Security-Policy",
                        containsString("object-src 'none'")));
    }

    @Test
    void responsesCarryTheReferrerPolicy() throws Exception {
        mvc.perform(get("/"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
    }

    @Test
    void responsesCarryThePermissionsPolicy() throws Exception {
        mvc.perform(get("/"))
                .andExpect(header().string("Permissions-Policy", containsString("camera=()")));
    }

    /**
     * Spring Security sets these two by default. They are asserted so that a
     * later {@code defaultsDisabled()} or a hand-rolled header block cannot drop
     * them unnoticed.
     */
    @Test
    void responsesKeepTheDefaultFramingAndSniffingHeaders() throws Exception {
        mvc.perform(get("/"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    /**
     * Gives the frontend path and the administration endpoint a handler, so the
     * chain's own answer is what each assertion observes.
     */
    @RestController
    static class ProbeController {

        @GetMapping({"/", "/api/admin/accounts"})
        String index() {
            return "index";
        }
    }
}
