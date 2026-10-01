package com.example.backend.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.auth.InMemoryAccountSessions;
import com.example.backend.auth.infrastructure.session.AccountSessionsAdapter;
import com.example.backend.scim.domain.NormalizedUserName;
import com.example.backend.scim.domain.ReservedResourceName;
import com.example.backend.scim.domain.ScimGroupRepository;
import com.example.backend.scim.domain.ScimUser;
import com.example.backend.scim.domain.ScimUserRepository;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.UUID;
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
import org.springframework.http.MediaType;
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
 *
 * <p>No test here flags or locks a seeded identity: they are shared with every other test
 * in this context, and a change-required flag cannot be cleared without changing the
 * password. The flagging and unlocking paths are driven end to end in
 * {@code PasswordChangeLifecycleIntegrationTests}, on Users that test creates for itself.
 */
@SpringBootTest
@Import(com.example.backend.ContainerTestConfiguration.class)
class AdminAccountEndpointTests {

    /**
     * The session registry, in memory. The deployed one is Redis-backed and this
     * context has no Redis, but the reason to replace it is not only that: a fake
     * can be asked what it holds, so the removed-endpoint test below proves a
     * request to the old disable path revoked nothing rather than merely
     * returning 404.
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
    private ScimGroupRepository groups;

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
     * The configuration the forced-change path depends on, asserted rather
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
     * The derived columns, over the wire: the Bootstrap Admin is flagged so the page can show it
     * with no lockout state, and its direct Groups name the seeded Admin group — the same
     * membership the {@code admin} column is derived from.
     */
    @Test
    void theListingReportsTheBootstrapAdminAndEachUsersDirectGroups() throws Exception {
        String adminGroup = adminGroupName();

        mvc.perform(get("/api/admin/accounts").session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.userName == 'test-admin')].bootstrapAdmin")
                        .value(Matchers.contains(true)))
                .andExpect(jsonPath("$[?(@.userName == 'test-user')].bootstrapAdmin")
                        .value(Matchers.contains(false)))
                .andExpect(jsonPath("$[?(@.userName == 'test-admin')].groups[*].displayName")
                        .value(Matchers.hasItem(adminGroup)))
                .andExpect(jsonPath("$[?(@.userName == 'test-user')].groups[*].displayName")
                        .value(Matchers.not(Matchers.hasItem(adminGroup))));
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
                .containsExactlyInAnyOrder("id", "userName", "displayName", "admin",
                        "bootstrapAdmin", "active", "locked", "hasPassword",
                        "passwordChangeRequired", "lastAuthenticatedAt", "createdAt", "groups"));
    }

    @Test
    void anAdministratorSeesEveryGroupWithItsMemberCountAndTheAdminMarker() throws Exception {
        String adminGroup = adminGroupName();

        String body = mvc.perform(get("/api/admin/groups")
                        .session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.displayName == '" + adminGroup + "')].adminGroup")
                        .value(Matchers.contains(true)))
                .andExpect(jsonPath("$[?(@.displayName == '" + adminGroup + "')].memberCount")
                        .value(Matchers.contains(Matchers.greaterThanOrEqualTo(1))))
                .andReturn().getResponse().getContentAsString();

        JsonNode listing = JsonMapper.builder().build().readTree(body);
        assertThat(listing.size()).isPositive();
        listing.valueStream().forEach(group -> assertThat(group.propertyNames())
                .containsExactlyInAnyOrder("id", "displayName", "memberCount", "adminGroup"));
    }

    @Test
    void anAuthenticatedNonAdministratorIsForbidden() throws Exception {
        mvc.perform(get("/api/admin/accounts").session(authenticatedSession("ROLE_USER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/groups").session(authenticatedSession("ROLE_USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void anUnauthenticatedCallerIsUnauthorized() throws Exception {
        mvc.perform(get("/api/admin/accounts"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/groups"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * The control endpoints are unsafe methods, so the CSRF contract applies to
     * them as it does to login. Without the header the chain answers 403 before
     * authorization is considered — the same status a wrong role earns, for an
     * entirely different reason, which is why the role tests below carry a valid
     * one.
     */
    @Test
    void aControlRequestWithoutACsrfTokenIsRefusedBeforeAuthorization() throws Exception {
        mvc.perform(post("/api/admin/accounts/{id}/unlock", require("test-user").id())
                        .session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void anAuthenticatedNonAdministratorCannotUnlockOrForceAChange() throws Exception {
        UUID id = require("test-user").id();
        mvc.perform(withCsrf(post("/api/admin/accounts/{id}/unlock", id))
                        .session(authenticatedSession("ROLE_USER")))
                .andExpect(status().isForbidden());
        mvc.perform(withCsrf(post("/api/admin/accounts/{id}/force-password-change", id))
                        .session(authenticatedSession("ROLE_USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void anUnknownAccountIsNotFound() throws Exception {
        mvc.perform(withCsrf(post("/api/admin/accounts/{id}/unlock", UUID.randomUUID()))
                        .session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isNotFound());
        mvc.perform(withCsrf(post(
                        "/api/admin/accounts/{id}/force-password-change", UUID.randomUUID()))
                        .session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isNotFound());
    }

    /**
     * The operations are re-targeted to the stable id, so a {@code userName} in the id's place is
     * not a way of naming an account any more: it is a malformed id, and nothing is written.
     */
    @Test
    void aUserNameIsNotAnIdAndActsOnNobody() throws Exception {
        mvc.perform(withCsrf(post("/api/admin/accounts/test-user/unlock"))
                        .session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isBadRequest());
        mvc.perform(withCsrf(post("/api/admin/accounts/test-user/force-password-change"))
                        .session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isBadRequest());
        assertThat(require("test-user").login().isPasswordChangeRequired()).isFalse();
    }

    /**
     * The legacy Disable and Enable endpoints are REMOVED, not hidden: an administrator holding a
     * valid CSRF header gets {@code 404} at the old path, whether it names the account by
     * {@code userName} as it used to or by the stable id, and the account is untouched — its
     * {@code active} flag, its version and the sessions it holds are exactly as they were.
     */
    @Test
    void theLegacyDisableAndEnableEndpointsAreGone() throws Exception {
        ScimUser before = require("test-user");
        sessions.open(before.id(), "live-session");

        try {
            for (String action : List.of("disable", "enable")) {
                for (Object target : List.of("test-user", before.id())) {
                    mvc.perform(withCsrf(post("/api/admin/accounts/{target}/" + action, target))
                                    .session(authenticatedSession("ROLE_ADMIN")))
                            .andExpect(status().isNotFound());
                }
            }

            ScimUser after = require("test-user");
            assertThat(after.profile().active()).isTrue();
            assertThat(after.version()).isEqualTo(before.version());
            assertThat(sessions.sessionsOf(before.id())).containsExactly("live-session");
        } finally {
            sessions.revokeAll(before.id());
        }
    }

    /**
     * The read-only criterion, enforced by the backend and not only by which controls the page
     * renders: every write an Admin could aim at a User or a Group through the administration
     * namespace is refused — {@code 405} where the path exists for reading, {@code 404} where it
     * does not exist at all — and the directory reads back exactly as it was.
     *
     * <p>Each request carries a valid CSRF header and an Admin session, so the refusal is the
     * dispatcher's and not the chain's: a missing header or a wrong role would earn {@code 403}
     * and prove nothing about whether a write handler exists.
     */
    @Test
    void everyWriteToADirectoryOwnedResourceIsRefused() throws Exception {
        ScimUser user = require("test-user");
        String adminGroupId = adminGroupId();
        String listingBefore = listing("/api/admin/accounts");
        String groupsBefore = listing("/api/admin/groups");
        String body = "{\"userName\":\"renamed\",\"active\":false,\"displayName\":\"renamed\","
                + "\"groups\":[],\"members\":[]}";

        List<MockHttpServletRequestBuilder> writes = List.of(
                post("/api/admin/accounts"),
                put("/api/admin/accounts"),
                patch("/api/admin/accounts"),
                delete("/api/admin/accounts"),
                put("/api/admin/accounts/{id}", user.id()),
                patch("/api/admin/accounts/{id}", user.id()),
                delete("/api/admin/accounts/{id}", user.id()),
                post("/api/admin/accounts/{id}", user.id()),
                put("/api/admin/accounts/{id}/groups", user.id()),
                post("/api/admin/accounts/{id}/groups", user.id()),
                post("/api/admin/groups"),
                put("/api/admin/groups"),
                patch("/api/admin/groups"),
                delete("/api/admin/groups"),
                put("/api/admin/groups/{id}", adminGroupId),
                patch("/api/admin/groups/{id}", adminGroupId),
                delete("/api/admin/groups/{id}", adminGroupId),
                post("/api/admin/groups/{id}/members", adminGroupId));

        for (MockHttpServletRequestBuilder write : writes) {
            int status = mvc.perform(withCsrf(write)
                            .session(authenticatedSession("ROLE_ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn().getResponse().getStatus();
            assertThat(status)
                    .as("%s", write.buildRequest(context.getServletContext()).getRequestURI())
                    .isIn(404, 405);
        }

        assertThat(listing("/api/admin/accounts")).isEqualTo(listingBefore);
        assertThat(listing("/api/admin/groups")).isEqualTo(groupsBefore);
        assertThat(require("test-user").version()).isEqualTo(user.version());
    }

    @Test
    void unlockingAnAccountThatIsNotLockedSucceedsAndReportsItUnlocked() throws Exception {
        UUID id = require("test-user").id();

        mvc.perform(withCsrf(post("/api/admin/accounts/{id}/unlock", id))
                        .session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.locked").value(false))
                .andExpect(jsonPath("$.passwordChangeRequired").value(false))
                .andExpect(jsonPath("$.lockedUntil").doesNotExist());
    }

    /**
     * An Admin cannot Unlock their own account, addressed by id: the principal is the seeded
     * Bootstrap Admin's own name and the target is its id. A second Admin's self-targeted forced
     * change is covered at the service; the Bootstrap Admin is the one Admin allowed to flag itself.
     */
    @Test
    void anAdministratorCannotUnlockTheirOwnAccount() throws Exception {
        UUID self = require("test-admin").id();

        mvc.perform(withCsrf(post("/api/admin/accounts/{id}/unlock", self))
                        .session(authenticatedSession("test-admin", "ROLE_ADMIN")))
                .andExpect(status().isForbidden());
    }

    /** Only the Bootstrap Admin may flag its own password; anyone else asking is refused. */
    @Test
    void anotherAdministratorCannotForceTheBootstrapAdminsPasswordChange() throws Exception {
        UUID bootstrap = require("test-admin").id();

        mvc.perform(withCsrf(post("/api/admin/accounts/{id}/force-password-change", bootstrap))
                        .session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isForbidden());
        assertThat(require("test-admin").login().isPasswordChangeRequired()).isFalse();
    }

    private String listing(String path) throws Exception {
        return mvc.perform(get(path).session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String adminGroupName() {
        return groups.findByReservedName(ReservedResourceName.ADMIN_GROUP)
                .orElseThrow().displayName();
    }

    private String adminGroupId() {
        return groups.findByReservedName(ReservedResourceName.ADMIN_GROUP)
                .orElseThrow().id().toString();
    }

    private ScimUser require(String userName) {
        return users.findByNormalizedUserName(NormalizedUserName.of(userName)).orElseThrow();
    }

    /** Echoes a CSRF value the shared repository minted, exactly as the SPA does. */
    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request) {
        CsrfToken csrf = csrfTokenRepository.generateToken(new MockHttpServletRequest());
        return request
                .cookie(new Cookie("XSRF-TOKEN", csrf.getToken()))
                .header("X-XSRF-TOKEN", csrf.getToken());
    }

    private MockHttpSession authenticatedSession(String authority) {
        return authenticatedSession("account", authority);
    }

    private MockHttpSession authenticatedSession(String name, String authority) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new TestingAuthenticationToken(name, null, authority));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                securityContext);
        return session;
    }
}
