package com.example.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.example.backend.ContainerTestConfiguration;
import com.example.backend.auth.application.DormancyRun;
import com.example.backend.auth.application.InactivityDeactivationService;
import com.example.backend.observability.RequestIdFilter;
import com.example.backend.scim.ScimConditionalWrites;
import com.example.backend.scim.application.ConnectorAdministrationService;
import com.example.backend.scim.domain.ConnectorTokenScope;
import com.example.backend.scim.domain.DormancyPolicy;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The ticket's three end-to-end paths, over real HTTP through the real filter chain, against a real
 * Postgres and a real, indexed Redis session store — no in-memory session registry, so "every other
 * session is gone" is observed as a cookie that no longer authenticates rather than as a call a fake
 * recorded.
 *
 * <ol>
 *   <li>A connector sets a password; the next login is confined to the change flow, every other
 *       route refused — an administrative one included, for a flagged Admin; a valid change; full
 *       access after a fresh login, every earlier session gone.
 *   <li>The Admin-forced equivalent, and Unlock requiring a change.
 *   <li>The dormancy basis under a simulated clock: confined logins do not move it, the completed
 *       change does.
 * </ol>
 *
 * <p>Sessions travel as the session cookie between requests, exactly as a browser holds them: the
 * Spring Session filter is in the MockMvc chain, so each request's session is read from and written
 * to Redis.
 */
@SpringBootTest
@Import({ContainerTestConfiguration.class, DormancyTestClockConfiguration.class})
@TestPropertySource(properties = "app.scim.enabled=true")
class PasswordChangeLifecycleIntegrationTests {

    private static final String BASE = "/scim/v2";
    private static final String USER_SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:User";
    private static final String PATCH_SCHEMA = "urn:ietf:params:scim:api:messages:2.0:PatchOp";
    private static final MediaType SCIM_JSON = MediaType.valueOf("application/scim+json");

    private static final String BOOTSTRAP_ADMIN = "test-admin";
    private static final String BOOTSTRAP_PASSWORD = "test-admin-password";

    private static final String CONNECTOR_PASSWORD = "connector-chosen-1";
    private static final String NEW_PASSWORD = "self-chosen-passphrase";

    private final JsonMapper json = JsonMapper.builder().build();

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ConnectorAdministrationService connectors;

    @Autowired
    private CsrfTokenRepository csrfTokenRepository;

    @Autowired
    private RequestIdFilter requestIdFilter;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    @Autowired
    @Qualifier("springSessionRepositoryFilter")
    private Filter springSessionRepositoryFilter;

    @Autowired
    private FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    @Autowired
    private InactivityDeactivationService deactivation;

    @Autowired
    private MutableClock clock;

    @Value("${server.servlet.session.cookie.name:SESSION}")
    private String sessionCookieName;

    @Value("${app.auth.lockout.max-attempts}")
    private int maxAttempts;

    private MockMvc mvc;

    private String writeToken;

    private final List<UUID> created = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(requestIdFilter, springSessionRepositoryFilter, springSecurityFilterChain)
                .build();
        UUID connectorId = connectors.create("lifecycle-connector", BOOTSTRAP_ADMIN).id();
        writeToken = connectors
                .issueToken(connectorId, ConnectorTokenScope.READ_WRITE, null, BOOTSTRAP_ADMIN)
                .presentedValue();
    }

    @AfterEach
    void removeWhatThisTestCreated() {
        for (UUID id : created) {
            jdbc.update("DELETE FROM scim_resources WHERE id = ? AND reserved_name IS NULL", id);
        }
        created.clear();
        jdbc.update("UPDATE scim_users SET password_change_required_since = NULL"
                + " WHERE resource_id = (SELECT id FROM scim_resources WHERE reserved_name IS NOT NULL"
                + " AND resource_type = 'User')");
    }

    // ---- 1. connector-set password ----------------------------------------------------------

    @Test
    void aConnectorSetPasswordConfinesTheNextLoginUntilAValidChangeThenGrantsFullAccess()
            throws Exception {
        String userId = provision("lifecycle-connector-admin", CONNECTOR_PASSWORD);
        addToAdminGroup(userId);
        long versionBefore = scimVersion(userId);

        // Two sessions, so "every session of that User" is more than the one that submits.
        Cookie other = logIn("lifecycle-connector-admin", CONNECTOR_PASSWORD, null, true);
        Cookie confined = logIn("lifecycle-connector-admin", CONNECTOR_PASSWORD, null, true);

        assertConfined(confined);

        // Wrong current password: 401, counted toward the Login lockout.
        assertThat(changePassword(confined, "not-the-current-one", NEW_PASSWORD)
                .getResponse().getStatus()).isEqualTo(401);
        assertThat(failedAttempts(userId)).isEqualTo(1);

        // Policy violation and reuse: 400 naming the rule, echoing neither value.
        MvcResult tooShort = changePassword(confined, CONNECTOR_PASSWORD, "short-one");
        assertThat(tooShort.getResponse().getStatus()).isEqualTo(400);
        String tooShortBody = tooShort.getResponse().getContentAsString();
        assertThat(json.readTree(tooShortBody).get("rule").asText()).isEqualTo("TOO_SHORT");
        assertThat(tooShortBody).doesNotContain("short-one").doesNotContain(CONNECTOR_PASSWORD);

        MvcResult reused = changePassword(confined, CONNECTOR_PASSWORD, CONNECTOR_PASSWORD);
        assertThat(reused.getResponse().getStatus()).isEqualTo(400);
        assertThat(json.readTree(reused.getResponse().getContentAsString()).get("rule").asText())
                .isEqualTo("REUSED");
        assertThat(reused.getResponse().getContentAsString()).doesNotContain(CONNECTOR_PASSWORD);

        // The valid change.
        assertThat(changePassword(confined, CONNECTOR_PASSWORD, NEW_PASSWORD)
                .getResponse().getStatus()).isEqualTo(204);

        // Every session is gone, the submitter's included.
        assertThat(status(get("/api/auth/me"), confined)).isEqualTo(401);
        assertThat(status(get("/api/auth/me"), other)).isEqualTo(401);
        assertThat(sessionRepository.findByPrincipalName(userId)).isEmpty();

        // The stored state: flag cleared, version advanced, a redacted event.
        assertThat(jdbc.queryForObject(
                "SELECT password_change_required_since FROM scim_users WHERE resource_id = ?::uuid",
                Timestamp.class, userId)).isNull();
        assertThat(scimVersion(userId)).isEqualTo(versionBefore + 1);
        assertThat(failedAttempts(userId)).isZero();
        List<Map<String, Object>> events = jdbc.queryForList(
                "SELECT * FROM audit_events WHERE subject_id = ?::uuid AND operation = 'PASSWORD_CHANGE'"
                        + " AND outcome = 'SUCCESS'", userId);
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.get("actor_id").toString()).isEqualTo(userId);
            assertThat(event.get("changed_paths").toString()).contains("password");
            assertThat(event.values()).allSatisfy(value -> assertThat(String.valueOf(value))
                    .doesNotContain(NEW_PASSWORD)
                    .doesNotContain(CONNECTOR_PASSWORD));
        });

        // The old password no longer works; a fresh login with the new one has full access.
        assertThat(logInStatus("lifecycle-connector-admin", CONNECTOR_PASSWORD)).isEqualTo(401);
        Cookie fresh = logIn("lifecycle-connector-admin", NEW_PASSWORD, "ADMIN", false);
        assertThat(status(get("/api/admin/accounts"), fresh)).isEqualTo(200);
        assertThat(status(get("/api/count"), fresh)).isEqualTo(200);
    }

    // ---- 2. Admin-forced change, and Unlock ------------------------------------------------

    @Test
    void anAdminForcedChangeEndsTheUsersSessionsAndConfinesItsNextLoginUntilItChanges()
            throws Exception {
        String forcedId = provisionAndSettle("lifecycle-forced", CONNECTOR_PASSWORD, NEW_PASSWORD);
        Cookie before = logIn("lifecycle-forced", NEW_PASSWORD, "USER", false);
        assertThat(status(get("/api/count"), before)).isEqualTo(200);

        Cookie admin = logIn(BOOTSTRAP_ADMIN, BOOTSTRAP_PASSWORD, "ADMIN", false);
        MvcResult forced = send(post("/api/admin/accounts/{id}/force-password-change", forcedId),
                admin);
        assertThat(forced.getResponse().getStatus()).isEqualTo(200);
        assertThat(json.readTree(forced.getResponse().getContentAsString())
                .get("passwordChangeRequired").booleanValue()).isTrue();

        assertThat(status(get("/api/auth/me"), before))
                .as("forcing a change ends the sessions the User already holds")
                .isEqualTo(401);

        Cookie confined = logIn("lifecycle-forced", NEW_PASSWORD, null, true);
        assertConfined(confined);
        String second = "another-self-chosen-one";
        assertThat(changePassword(confined, NEW_PASSWORD, second).getResponse().getStatus())
                .isEqualTo(204);
        assertThat(status(get("/api/auth/me"), confined)).isEqualTo(401);

        Cookie fresh = logIn("lifecycle-forced", second, "USER", false);
        assertThat(status(get("/api/count"), fresh)).isEqualTo(200);
    }

    @Test
    void theAdminForcedChangeIsRefusedOnTheCallersOwnAccountAndOnTheBootstrapAdmin()
            throws Exception {
        String userId = provisionAndSettle("lifecycle-other-admin", CONNECTOR_PASSWORD, NEW_PASSWORD);
        addToAdminGroup(userId);
        Cookie otherAdmin = logIn("lifecycle-other-admin", NEW_PASSWORD, "ADMIN", false);

        assertThat(status(post("/api/admin/accounts/{id}/force-password-change", userId),
                otherAdmin)).isEqualTo(403);
        assertThat(status(post("/api/admin/accounts/{id}/force-password-change",
                idOf(BOOTSTRAP_ADMIN)), otherAdmin)).isEqualTo(403);
    }

    /**
     * IM8 as-15 / ac-6: Unlock requires a change, because the credential that reached the threshold
     * may be the one being guessed. After Unlock the User authenticates and is confined.
     */
    @Test
    void anUnlockedUserAuthenticatesIntoTheChangeFlow() throws Exception {
        String unlockedId =
                provisionAndSettle("lifecycle-unlocked", CONNECTOR_PASSWORD, NEW_PASSWORD);
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            assertThat(logInStatus("lifecycle-unlocked", "wrong-guess-" + attempt)).isEqualTo(401);
        }
        assertThat(logInStatus("lifecycle-unlocked", NEW_PASSWORD))
                .as("locked: the correct password is refused")
                .isEqualTo(401);

        Cookie admin = logIn(BOOTSTRAP_ADMIN, BOOTSTRAP_PASSWORD, "ADMIN", false);
        MvcResult unlocked = send(post("/api/admin/accounts/{id}/unlock", unlockedId), admin);
        assertThat(unlocked.getResponse().getStatus()).isEqualTo(200);
        assertThat(json.readTree(unlocked.getResponse().getContentAsString())
                .get("passwordChangeRequired").booleanValue()).isTrue();

        Cookie confined = logIn("lifecycle-unlocked", NEW_PASSWORD, null, true);
        assertConfined(confined);
    }

    /** The change counts toward the same lockout; at the threshold both paths are blocked. */
    @Test
    void wrongCurrentPasswordsLockTheUserOutOfBothLoginAndTheChangeFlow() throws Exception {
        String userId = provision("lifecycle-guessed", CONNECTOR_PASSWORD);
        Cookie confined = logIn("lifecycle-guessed", CONNECTOR_PASSWORD, null, true);
        Cookie spare = logIn("lifecycle-guessed", CONNECTOR_PASSWORD, null, true);

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            assertThat(changePassword(spare, "wrong-guess-" + attempt, NEW_PASSWORD)
                    .getResponse().getStatus()).isEqualTo(401);
        }

        assertThat(jdbc.queryForObject(
                "SELECT locked_at IS NOT NULL FROM scim_users WHERE resource_id = ?::uuid",
                Boolean.class, userId)).isTrue();
        assertThat(status(get("/api/auth/me"), confined))
                .as("imposing the lock ended every session")
                .isEqualTo(401);
        assertThat(logInStatus("lifecycle-guessed", CONNECTOR_PASSWORD)).isEqualTo(401);
    }

    // ---- 3. the dormancy basis ---------------------------------------------------------------

    /**
     * A login confined by a required change is not use of the account, so it does not move the
     * dormancy basis: a User that keeps logging in with an imposed credential and never replaces
     * it is still deactivated once the inactivity window has passed since it was created.
     */
    @Test
    void confinedLoginsDoNotMoveTheDormancyBasisSoAnUnchangedCredentialStillDeactivates()
            throws Exception {
        String userId = provision("lifecycle-lapsed", CONNECTOR_PASSWORD);
        Duration elapsed = Duration.ZERO;
        try {
            Duration first = DormancyPolicy.DEFAULT_DEACTIVATION_WINDOW.minusDays(30);
            clock.advanceBy(first);
            elapsed = elapsed.plus(first);
            logIn("lifecycle-lapsed", CONNECTOR_PASSWORD, null, true);
            assertThat(lastAuthenticatedAt(userId))
                    .as("a confined login leaves the dormancy basis where it was")
                    .isNull();

            Duration second = Duration.ofDays(31);
            clock.advanceBy(second);
            elapsed = elapsed.plus(second);
            logIn("lifecycle-lapsed", CONNECTOR_PASSWORD, null, true);
            assertThat(lastAuthenticatedAt(userId)).isNull();

            DormancyRun run = deactivation.deactivateDormantUsers();

            assertThat(run.processed()).contains(UUID.fromString(userId));
            assertThat(jdbc.queryForObject(
                    "SELECT active FROM scim_users WHERE resource_id = ?::uuid",
                    Boolean.class, userId)).isFalse();
            assertThat(sessionRepository.findByPrincipalName(userId))
                    .as("the deactivated User's confined sessions are revoked")
                    .isEmpty();
        } finally {
            clock.advanceBy(elapsed.negated());
        }
    }

    /**
     * The completed change is the first use of the account, so it moves the dormancy basis: a User
     * that changed its password inside the window is not deactivated by the window that runs from
     * its creation.
     *
     * <p>The User's creation is backdated rather than the clock advanced before the change: the
     * change needs a live session, and the absolute session lifetime measures the session's real
     * creation time against the simulated clock, so a session opened after an advance is already
     * expired.
     */
    @Test
    void theCompletedChangeMovesTheDormancyBasis() throws Exception {
        String userId = provision("lifecycle-settled", CONNECTOR_PASSWORD);
        jdbc.update("UPDATE scim_resources SET created_at = ? WHERE id = ?::uuid",
                Timestamp.from(clock.instant()
                        .minus(DormancyPolicy.DEFAULT_DEACTIVATION_WINDOW.minusDays(30))),
                userId);
        Cookie confined = logIn("lifecycle-settled", CONNECTOR_PASSWORD, null, true);
        assertThat(changePassword(confined, CONNECTOR_PASSWORD, NEW_PASSWORD)
                .getResponse().getStatus()).isEqualTo(204);
        assertThat(lastAuthenticatedAt(userId)).isEqualTo(Timestamp.from(clock.instant()));

        Duration elapsed = Duration.ofDays(31);
        clock.advanceBy(elapsed);
        try {
            assertThat(deactivation.deactivateDormantUsers().processed())
                    .as("91 days since creation, but 31 since the change")
                    .doesNotContain(UUID.fromString(userId));
            assertThat(jdbc.queryForObject(
                    "SELECT active FROM scim_users WHERE resource_id = ?::uuid",
                    Boolean.class, userId)).isTrue();

            logIn("lifecycle-settled", NEW_PASSWORD, "USER", false);
            assertThat(lastAuthenticatedAt(userId))
                    .as("an unconfined login moves the basis as before")
                    .isEqualTo(Timestamp.from(clock.instant()));
        } finally {
            clock.advanceBy(elapsed.negated());
        }
    }

    // ---- helpers ----------------------------------------------------------------------------

    /**
     * Every route but the three the confined session holds is refused with 403 — the user
     * endpoints, the administrative interface, and the operational scrape — while the session
     * itself is valid and reports its confinement.
     */
    private String idOf(String userName) {
        return jdbc.queryForObject(
                "SELECT resource_id::text FROM scim_users WHERE normalized_user_name = ?",
                String.class, userName.toLowerCase(java.util.Locale.ROOT));
    }

    private void assertConfined(Cookie session) throws Exception {
        MvcResult me = send(get("/api/auth/me"), session);
        assertThat(me.getResponse().getStatus()).isEqualTo(200);
        JsonNode body = json.readTree(me.getResponse().getContentAsString());
        assertThat(body.get("passwordChangeRequired").booleanValue()).isTrue();
        assertThat(body.get("role").isNull()).isTrue();

        assertThat(status(get("/api/admin/accounts"), session)).isEqualTo(403);
        assertThat(status(post("/api/admin/accounts/{id}/unlock", idOf(BOOTSTRAP_ADMIN)), session))
                .isEqualTo(403);
        assertThat(status(get("/api/admin/connectors"), session)).isEqualTo(403);
        assertThat(status(get("/api/count"), session)).isEqualTo(403);
        assertThat(status(post("/api/count/increment"), session)).isEqualTo(403);
        assertThat(status(get("/api/session"), session)).isEqualTo(403);
        assertThat(status(get("/actuator/prometheus"), session)).isEqualTo(403);
    }

    /** Creates a User over SCIM with a connector-set password, which flags it. */
    private String provision(String userName, String password) throws Exception {
        MvcResult result = mvc.perform(asConnector(post(BASE + "/Users"))
                        .contentType(SCIM_JSON)
                        .content("""
                                {"schemas":["%s"],"userName":"%s","password":"%s"}"""
                                .formatted(USER_SCHEMA, userName, password)))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        String id = json.readTree(result.getResponse().getContentAsString()).get("id").asText();
        created.add(UUID.fromString(id));
        return id;
    }

    /** Provisions, then completes the required change, leaving an unflagged User. */
    private String provisionAndSettle(String userName, String connectorPassword, String own)
            throws Exception {
        String id = provision(userName, connectorPassword);
        Cookie confined = logIn(userName, connectorPassword, null, true);
        assertThat(changePassword(confined, connectorPassword, own).getResponse().getStatus())
                .isEqualTo(204);
        return id;
    }

    private void addToAdminGroup(String userId) throws Exception {
        String adminGroupId = jdbc.queryForObject(
                "SELECT id FROM scim_resources WHERE reserved_name IS NOT NULL AND resource_type = ?",
                UUID.class, "Group").toString();
        MvcResult result = mvc.perform(asConnector(patch(BASE + "/Groups/" + adminGroupId))
                        .contentType(SCIM_JSON)
                        .content("""
                                {"schemas":["%s"],
                                 "Operations":[{"op":"add","path":"members","value":[{"value":"%s"}]}]}"""
                                .formatted(PATCH_SCHEMA, userId)))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
    }

    /** Logs in for real, asserting the reported role and confinement, and returns the cookie. */
    private Cookie logIn(String userName, String password, String role, boolean confined)
            throws Exception {
        MvcResult login = mvc.perform(withCsrf(post("/api/auth/login"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}"
                                .formatted(userName, password)))
                .andReturn();
        assertThat(login.getResponse().getStatus()).isEqualTo(200);
        JsonNode body = json.readTree(login.getResponse().getContentAsString());
        assertThat(body.get("passwordChangeRequired").booleanValue()).isEqualTo(confined);
        if (role == null) {
            assertThat(body.get("role").isNull()).isTrue();
        } else {
            assertThat(body.get("role").asText()).isEqualTo(role);
        }
        Cookie session = login.getResponse().getCookie(sessionCookieName);
        assertThat(session).as("the login issued a session cookie").isNotNull();
        return new Cookie(session.getName(), session.getValue());
    }

    private int logInStatus(String userName, String password) throws Exception {
        return mvc.perform(withCsrf(post("/api/auth/login"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}"
                                .formatted(userName, password)))
                .andReturn().getResponse().getStatus();
    }

    private MvcResult changePassword(Cookie session, String current, String next) throws Exception {
        return mvc.perform(withCsrf(post("/api/auth/change-password"))
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"%s\",\"newPassword\":\"%s\"}"
                                .formatted(current, next)))
                .andReturn();
    }

    private MvcResult send(MockHttpServletRequestBuilder request, Cookie session) throws Exception {
        return mvc.perform(withCsrf(request).cookie(session)).andReturn();
    }

    private int status(MockHttpServletRequestBuilder request, Cookie session) throws Exception {
        return send(request, session).getResponse().getStatus();
    }

    private Timestamp lastAuthenticatedAt(String userId) {
        return jdbc.queryForObject(
                "SELECT last_authenticated_at FROM scim_users WHERE resource_id = ?::uuid",
                Timestamp.class, userId);
    }

    private long scimVersion(String userId) {
        return jdbc.queryForObject(
                "SELECT version FROM scim_resources WHERE id = ?::uuid", Long.class, userId);
    }

    private int failedAttempts(String userId) {
        return jdbc.queryForObject(
                "SELECT failed_login_attempts FROM scim_users WHERE resource_id = ?::uuid",
                Integer.class, userId);
    }

    private MockHttpServletRequestBuilder asConnector(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + writeToken)
                .with(ScimConditionalWrites.currentVersion(jdbc));
    }

    /**
     * A CSRF token on every request, safe or not — the cookie and the header must travel together,
     * and a request carrying the session cookie is otherwise indistinguishable from a browser's.
     */
    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request) {
        CsrfToken token = csrfTokenRepository.generateToken(new MockHttpServletRequest());
        return request
                .cookie(new Cookie("XSRF-TOKEN", token.getToken()))
                .header("X-XSRF-TOKEN", token.getToken());
    }
}
