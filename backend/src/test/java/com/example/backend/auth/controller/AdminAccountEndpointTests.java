package com.example.backend.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.auth.InMemoryAccountSessions;
import com.example.backend.auth.infrastructure.session.AccountSessionsAdapter;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * The endpoint as a caller meets it: the real filter chain, the real controller,
 * the application's own JSON converters, over the accounts startup seeding
 * created.
 *
 * <p>{@code AdminAccountControllerTests} covers what the controller returns and
 * {@code SecurityConfigTests} covers which paths the chain guards. What neither
 * can see is the two composed — a response that is correct but reachable by the
 * wrong caller, or authorized but carrying the wrong JSON.
 *
 * <p>The whole web application context is wired in rather than a standalone
 * controller precisely so the wire format is the deployed one: a hand-built
 * MockMvc would render {@code createdAt} as a number, and these assertions would
 * then describe a format the running service does not produce.
 */
@SpringBootTest
class AdminAccountEndpointTests {

    /**
     * The session registry, in memory. The deployed one is Redis-backed and this
     * context has no Redis, but the reason to replace it is not only that: a fake
     * can be asked what it ended, so the disable below proves the revocation
     * reached the port rather than merely returning 200.
     *
     * <p>The real {@code AccountSessionsAdapter} bean is still built beside it —
     * {@link #theContextWiresTheIndexedSessionRepositoryTheDeployedServiceNeeds()}
     * is what holds that.
     */
    @TestConfiguration
    static class SessionRegistryConfiguration {

        @Bean
        @Primary
        InMemoryAccountSessions inMemoryAccountSessions() {
            return new InMemoryAccountSessions();
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private InMemoryAccountSessions sessions;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    @Autowired
    private CsrfTokenRepository csrfTokenRepository;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    /**
     * The configuration this endpoint's disable path depends on, asserted rather
     * than assumed: a plain session repository cannot be searched by principal, so
     * {@code AccountSessionsAdapter} has nothing to inject and the deployed service
     * does not start. The fake registry above is {@code @Primary}, so it would hide
     * the adapter's absence from every other test here.
     */
    @Test
    void theContextWiresTheIndexedSessionRepositoryTheDeployedServiceNeeds() {
        assertThat(context.getBean(FindByIndexNameSessionRepository.class)).isNotNull();
        assertThat(context.getBean(AccountSessionsAdapter.class)).isNotNull();
    }

    @Test
    void anAdministratorSeesEveryAccountWithItsRoleStatusAndCreationDate() throws Exception {
        mvc.perform(get("/api/admin/accounts").session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username")
                        .value(Matchers.hasItems("test-admin", "test-user")))
                .andExpect(jsonPath("$[?(@.username == 'test-user')].role")
                        .value(Matchers.contains("USER")))
                .andExpect(jsonPath("$[?(@.username == 'test-admin')].role")
                        .value(Matchers.contains("ADMIN")))
                .andExpect(jsonPath("$[?(@.username == 'test-user')].enabled")
                        .value(Matchers.contains(true)))
                // ISO-8601 rather than an epoch number, which is what the SPA and
                // the OpenAPI document both describe.
                .andExpect(jsonPath("$[?(@.username == 'test-user')].createdAt")
                        .value(Matchers.contains(Matchers.matchesPattern(
                                "\\d{4}-\\d{2}-\\d{2}T.*Z"))));
    }

    /** The acceptance criterion that matters most: no hash on the wire, ever. */
    @Test
    void theListingNeverCarriesAPasswordHash() throws Exception {
        mvc.perform(get("/api/admin/accounts").session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.not(Matchers.containsString("password"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("$2a$"))))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    void anAuthenticatedNonAdministratorIsForbidden() throws Exception {
        mvc.perform(get("/api/admin/accounts").session(authenticatedSession("ROLE_USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void anUnauthenticatedCallerIsUnauthorized() throws Exception {
        mvc.perform(get("/api/admin/accounts"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * The control endpoints are unsafe methods, so the CSRF contract applies to
     * them as it does to login. Without the token the chain answers 403 before
     * authorization is considered — the same status a wrong role earns, for an
     * entirely different reason, which is why the role tests below carry a valid
     * token.
     */
    @Test
    void aControlRequestWithoutACsrfTokenIsRefusedBeforeAuthorization() throws Exception {
        mvc.perform(post("/api/admin/accounts/test-user/disable")
                        .session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void anAuthenticatedNonAdministratorCannotDisableAnAccount() throws Exception {
        mvc.perform(withCsrf(post("/api/admin/accounts/test-user/disable"))
                        .session(authenticatedSession("ROLE_USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void anUnknownAccountIsNotFound() throws Exception {
        mvc.perform(withCsrf(post("/api/admin/accounts/nobody/unlock"))
                        .session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isNotFound());
    }

    /**
     * Driven through the chain rather than against the service so the whole
     * round trip is covered: token, role, path variable, the write, the sessions
     * the account was holding, and the updated row coming back as JSON. The
     * account is enabled again afterwards, because the seeded accounts are shared
     * with every other test in this context.
     */
    @Test
    void anAdministratorDisablesAndReopensAnAccount() throws Exception {
        sessions.open("test-user", "live-session");

        try {
            mvc.perform(withCsrf(post("/api/admin/accounts/test-user/disable"))
                            .session(authenticatedSession("ROLE_ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("test-user"))
                    .andExpect(jsonPath("$.enabled").value(false))
                    .andExpect(jsonPath("$.passwordHash").doesNotExist());

            assertThat(sessions.sessionsOf("test-user")).isEmpty();

            mvc.perform(get("/api/admin/accounts").session(authenticatedSession("ROLE_ADMIN")))
                    .andExpect(jsonPath("$[?(@.username == 'test-user')].enabled")
                            .value(Matchers.contains(false)));
        } finally {
            mvc.perform(withCsrf(post("/api/admin/accounts/test-user/enable"))
                            .session(authenticatedSession("ROLE_ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(true));
        }
    }

    /**
     * Refusing this is the only thing standing between an administrator and a
     * system nobody can administer. 409 rather than 403: the role is fine, the
     * action is not. It is also the request an administrator is most likely to
     * make by accident, so it must not cost them the session they are working in.
     */
    @Test
    void disablingTheLastEnabledAdministratorIsRefused() throws Exception {
        sessions.open("test-admin", "live-session");

        mvc.perform(withCsrf(post("/api/admin/accounts/test-admin/disable"))
                        .session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isConflict());

        assertThat(sessions.sessionsOf("test-admin")).containsExactly("live-session");

        mvc.perform(get("/api/admin/accounts").session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(jsonPath("$[?(@.username == 'test-admin')].enabled")
                        .value(Matchers.contains(true)));
    }

    @Test
    void unlockingAnAccountThatIsNotLockedSucceedsAndReportsItUnlocked() throws Exception {
        mvc.perform(withCsrf(post("/api/admin/accounts/test-user/unlock"))
                        .session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked").value(false))
                .andExpect(jsonPath("$.lockedUntil").value(Matchers.nullValue()));
    }

    /** Echoes a token the shared repository minted, exactly as the SPA does. */
    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request) {
        CsrfToken token = csrfTokenRepository.generateToken(new MockHttpServletRequest());
        return request
                .cookie(new Cookie("XSRF-TOKEN", token.getToken()))
                .header("X-XSRF-TOKEN", token.getToken());
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
}
