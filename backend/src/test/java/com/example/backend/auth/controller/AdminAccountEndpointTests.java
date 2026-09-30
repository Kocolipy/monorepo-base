package com.example.backend.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.auth.InMemoryAccountSessions;
import com.example.backend.auth.infrastructure.session.AccountSessionsAdapter;
import com.example.backend.scim.domain.NormalizedUserName;
import com.example.backend.scim.domain.ScimUser;
import com.example.backend.scim.domain.ScimUserRepository;
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
import org.springframework.context.annotation.Import;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The endpoint as a caller meets it: the real filter chain, the real controller,
 * the application's own JSON converters, over the SCIM identities startup seeding
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
 *
 * <p>The seeded {@code test-admin} is an administrator because it is the reserved
 * Bootstrap Admin and a member of the reserved Admin group, not because a role
 * column says so — which is what makes the {@code admin} column below an assertion
 * about the derivation rather than about a stored value.
 */
@SpringBootTest
@Import(com.example.backend.ContainerTestConfiguration.class)
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
    private ScimUserRepository users;

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
                .andExpect(jsonPath("$[*].userName")
                        .value(Matchers.hasItems("test-admin", "test-user")))
                .andExpect(jsonPath("$[?(@.userName == 'test-user')].admin")
                        .value(Matchers.contains(false)))
                .andExpect(jsonPath("$[?(@.userName == 'test-admin')].admin")
                        .value(Matchers.contains(true)))
                .andExpect(jsonPath("$[?(@.userName == 'test-user')].active")
                        .value(Matchers.contains(true)))
                // ISO-8601 rather than an epoch number, which is what the SPA and
                // the OpenAPI document both describe.
                .andExpect(jsonPath("$[?(@.userName == 'test-user')].createdAt")
                        .value(Matchers.contains(Matchers.matchesPattern(
                                "\\d{4}-\\d{2}-\\d{2}T.*Z"))));
    }

    /**
     * The acceptance criterion that matters most: no hash on the wire, ever. Asserted as the exact
     * field set of every listed identity, so any added field — a hash under whatever name — fails
     * here; {@code hasPassword} and {@code passwordChangeRequired} are booleans about the
     * credential, not the credential. The hash-format checks catch a value smuggled into an
     * existing field.
     */
    @Test
    void theListingNeverCarriesAPasswordHash() throws Exception {
        String body = mvc.perform(get("/api/admin/accounts")
                        .session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.not(Matchers.containsString("$2a$"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("argon2id"))))
                .andReturn().getResponse().getContentAsString();

        JsonNode listing = JsonMapper.builder().build().readTree(body);
        assertThat(listing.size()).isPositive();
        listing.valueStream().forEach(identity -> assertThat(identity.propertyNames())
                .containsExactlyInAnyOrder("id", "userName", "admin", "active", "locked",
                        "hasPassword", "passwordChangeRequired", "createdAt"));
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
     * the identity was holding, and the updated resource coming back as JSON. The
     * identity is reactivated afterwards, because the seeded identities are shared
     * with every other test in this context.
     */
    @Test
    void anAdministratorDisablesAndReopensAnAccount() throws Exception {
        java.util.UUID testUserId = require("test-user").id();
        sessions.open(testUserId, "live-session");

        try {
            mvc.perform(withCsrf(post("/api/admin/accounts/test-user/disable"))
                            .session(authenticatedSession("ROLE_ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userName").value("test-user"))
                    .andExpect(jsonPath("$.active").value(false))
                    .andExpect(jsonPath("$.passwordHash").doesNotExist());

            assertThat(sessions.sessionsOf(testUserId)).isEmpty();

            mvc.perform(get("/api/admin/accounts").session(authenticatedSession("ROLE_ADMIN")))
                    .andExpect(jsonPath("$[?(@.userName == 'test-user')].active")
                            .value(Matchers.contains(false)));
        } finally {
            mvc.perform(withCsrf(post("/api/admin/accounts/test-user/enable"))
                            .session(authenticatedSession("ROLE_ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(true));
        }
    }

    /**
     * Refusing this is the only thing standing between an administrator and a
     * system nobody can administer. 409 rather than 403: the role is fine, the
     * action is not. It is also the request an administrator is most likely to
     * make by accident, so it must not cost them the session they are working in.
     *
     * <p>Named for the PROTECTED-RESOURCE guard rather than the last-active-
     * administrator one, because that is the guard this request now reaches.
     * {@code test-admin} is the seeded Bootstrap Admin, so the protection check
     * refuses it before the last-active-administrator branch is evaluated at all.
     * The method was previously called
     * {@code disablingTheLastEnabledAdministratorIsRefused}, which a future reader
     * would have trusted over the javadoc correcting it — and HTTP-level coverage
     * of the last-active-administrator refusal is genuinely GONE, since no
     * unprotected second administrator is seeded here. That branch is covered at
     * {@code IdentityAdministrationServiceTests}, which separates the two guards
     * and reaches each on its own.
     *
     * <p>The three outcomes asserted are the ones that mattered before and still
     * do: refused with 409, the working session kept, and the identity still
     * active on re-read.
     */
    @Test
    void disablingTheBootstrapAdminIsRefusedAsAProtectedResource() throws Exception {
        java.util.UUID testAdminId = require("test-admin").id();
        sessions.open(testAdminId, "live-session");

        mvc.perform(withCsrf(post("/api/admin/accounts/test-admin/disable"))
                        .session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isConflict());

        assertThat(sessions.sessionsOf(testAdminId)).containsExactly("live-session");

        mvc.perform(get("/api/admin/accounts").session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(jsonPath("$[?(@.userName == 'test-admin')].active")
                        .value(Matchers.contains(true)));
    }

    @Test
    void unlockingAnAccountThatIsNotLockedSucceedsAndReportsItUnlocked() throws Exception {
        mvc.perform(withCsrf(post("/api/admin/accounts/test-user/unlock"))
                        .session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked").value(false))
                .andExpect(jsonPath("$.lockedUntil").doesNotExist());
    }

    private ScimUser require(String userName) {
        return users.findByNormalizedUserName(NormalizedUserName.of(userName)).orElseThrow();
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
