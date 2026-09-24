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
import org.springframework.boot.test.context.SpringBootTest;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
// Every assertion here is about what this configuration builds, and the chain is
// built once per context. Evicting the cached context before each method makes
// each of them observe a real startup rather than wiring inherited from whichever
// test happened to run first, which is also what keeps each assertion answerable
// for the chain line it covers.
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
class SecurityConfigTests {

    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";

    @Autowired
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

    @Test
    void userRoleCannotReachAccountAdministration() throws Exception {
        mvc.perform(get("/api/accounts").session(authenticatedSession("ROLE_USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRoleCanReachAccountAdministration() throws Exception {
        mvc.perform(get("/api/accounts").session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isOk());
    }

    /**
     * {@code /api/admin/**} is a second administrative namespace rather than a
     * path beneath the first, so the accounts rule above says nothing about it.
     * These three cover the whole rule: refused for a non-admin, allowed for an
     * admin, and — because the chain answers before any handler — unauthorized
     * rather than forbidden for a caller with no session at all.
     */
    @Test
    void userRoleCannotReachTheAdminNamespace() throws Exception {
        mvc.perform(get("/api/admin/users").session(authenticatedSession("ROLE_USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRoleCanReachTheAdminNamespace() throws Exception {
        mvc.perform(get("/api/admin/users").session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousCallerCannotReachTheAdminNamespace() throws Exception {
        mvc.perform(get("/api/admin/users"))
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

    @RestController
    static class ProbeController {

        @GetMapping({"/", "/api/accounts", "/api/admin/users"})
        String index() {
            return "index";
        }
    }
}
