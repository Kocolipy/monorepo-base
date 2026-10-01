package com.example.backend.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.example.backend.ContainerTestConfiguration;
import com.example.backend.InMemorySessionRegistryConfiguration;
import com.example.backend.auth.DormancyTestClockConfiguration;
import com.example.backend.auth.MutableClock;
import com.example.backend.auth.application.DormantAuthorityRevocationService;
import com.example.backend.auth.application.InactivityDeactivationService;
import com.example.backend.observability.RequestIdFilter;
import com.example.backend.scim.ScimConditionalWrites;
import com.example.backend.scim.domain.DormancyPolicy;
import com.example.backend.scim.domain.NormalizedUserName;
import com.example.backend.scim.domain.ReservedResourceName;
import com.example.backend.scim.domain.ScimGroupRepository;
import com.example.backend.scim.domain.ScimUserRepository;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The listing's demo oracle: one action from every ticket that produces audit events, each driven
 * the way it happens in production, then each read back through {@code GET /api/admin/audit-events}
 * — present, correctly attributed, and redacted.
 *
 * <p>Driven over the real surfaces wherever one exists: logins, the Admin's connector, token,
 * Unlock and forced-change operations and the self-service change are real requests through the
 * application chain with a real CSRF token; the User and Group writes and the bulk read are real
 * SCIM requests bearing the token the Admin just minted. The two scheduled jobs have no HTTP
 * surface — a cron trigger is their only entry point — so they are invoked as their scheduler
 * invokes them, under a clock moved past their windows.
 *
 * <p>The redaction assertion is made against the listing's raw response text rather than against
 * chosen fields, because what must not be there must not be ANYWHERE: every password this test
 * submitted, the bearer value and both of its stored forms, every stored password hash, the literal
 * filter value, and every profile value it provisioned. Before searching for their absence it
 * asserts the text it searched holds the events that would have carried them.
 *
 * <p>Shares {@code InactivityGovernanceIntegrationTests}'s context — and so its Postgres and its
 * forward-moving clock — and follows its discipline: every User it asserts about is its own, and
 * the seeded ordinary User its job runs deactivate is put back afterwards.
 */
@SpringBootTest
@Import({
        ContainerTestConfiguration.class,
        InMemorySessionRegistryConfiguration.class,
        DormancyTestClockConfiguration.class})
@TestPropertySource(properties = "app.scim.enabled=true")
class AuditListingEndToEndIntegrationTests {

    private static final String SCIM = "/scim/v2";

    private static final MediaType SCIM_JSON = MediaType.valueOf("application/scim+json");

    private static final String USER_SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:User";

    private static final String PATCH_SCHEMA = "urn:ietf:params:scim:api:messages:2.0:PatchOp";

    private static final String BOOTSTRAP_ADMIN = "test-admin";

    private static final String BOOTSTRAP_PASSWORD = "test-admin-password";

    // Values that must never appear in a listed event.
    private static final String MEMBER_INITIAL = "Initial-Connector-Secret-41";
    private static final String MEMBER_REPLACED = "Replaced-By-Owner-Secret-42";
    private static final String LOCKED_INITIAL = "Locked-Connector-Secret-43";
    private static final String WRONG_PASSWORD = "Wrong-Guessed-Secret-44";
    private static final String FILTER_LITERAL = "audit-e2e-literal-probe-value";
    private static final String DISPLAY_NAME = "Audit Listing Display Name";
    private static final String RENAMED_DISPLAY_NAME = "Audit Listing Renamed Display";

    private final JsonMapper json = JsonMapper.builder().build();

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CsrfTokenRepository csrfTokenRepository;

    @Autowired
    private RequestIdFilter requestIdFilter;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MutableClock clock;

    @Autowired
    private InactivityDeactivationService deactivation;

    @Autowired
    private DormantAuthorityRevocationService authorityRevocation;

    @Autowired
    private ScimUserRepository users;

    @Autowired
    private ScimGroupRepository groups;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private MockMvc mvc;

    private String bearer;

    private final List<UUID> created = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(requestIdFilter, springSecurityFilterChain)
                .build();
    }

    /** Removes the Users this test provisioned and restores the shared seeded one. */
    @AfterEach
    void cleanUp() {
        for (UUID id : created) {
            jdbc.update("DELETE FROM scim_resources WHERE id = ? AND reserved_name IS NULL", id);
        }
        UUID seeded = users.findByNormalizedUserName(NormalizedUserName.of("test-user"))
                .orElseThrow().id();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            users.updateActive(seeded, true, clock.instant());
            // Reactivation requires a password change; the shared seeded baseline is unflagged.
            users.completePasswordChange(seeded,
                    users.findById(seeded).orElseThrow().login().passwordHash(), clock.instant());
        });
    }

    @Test
    void everyProducingTicketsEventIsListedCorrectlyAttributedAndRedacted() throws Exception {
        Instant start = clock.instant();
        MockHttpSession admin = logIn(BOOTSTRAP_ADMIN, BOOTSTRAP_PASSWORD);
        UUID adminId = userId(BOOTSTRAP_ADMIN);

        // ---- #12 connector identity and token lifecycle, by the Admin ----
        JsonNode connector = send(post("/api/admin/connectors").session(admin),
                "{\"displayName\":\"Audit Listing Connector\"}", 201);
        UUID connectorId = UUID.fromString(connector.get("id").asText());
        JsonNode issued = send(post("/api/admin/connectors/" + connectorId + "/tokens")
                .session(admin), "{\"scope\":\"READ_WRITE\"}", 201);
        bearer = issued.get("presentedValue").asText();
        UUID tokenId = UUID.fromString(issued.get("tokenId").asText());

        // ---- #13/#15/#16 User create, update and delete; #14 a Group write ----
        UUID member = scimCreate("audit-e2e-member", MEMBER_INITIAL);
        UUID locked = scimCreate("audit-e2e-locked", LOCKED_INITIAL);
        UUID dormant = scimCreate("audit-e2e-dormant", null);
        UUID dormantAdmin = scimCreate("audit-e2e-dormant-admin", null);
        UUID deleted = scimCreate("audit-e2e-deleted", null);
        scim(patch(SCIM + "/Users/" + deleted), """
                {"schemas":["%s"],"Operations":[{"op":"replace","path":"displayName","value":"%s"}]}"""
                .formatted(PATCH_SCHEMA, RENAMED_DISPLAY_NAME), 200);
        scim(delete(SCIM + "/Users/" + deleted), null, 204);
        UUID adminGroup = groups.findByReservedName(ReservedResourceName.ADMIN_GROUP).orElseThrow().id();
        scim(patch(SCIM + "/Groups/" + adminGroup), """
                {"schemas":["%s"],"Operations":[{"op":"add","path":"members","value":[{"value":"%s"}]}]}"""
                .formatted(PATCH_SCHEMA, dormantAdmin), 200);

        // ---- #17 a bulk read whose filter carries a literal ----
        JsonNode probe = scim(get(SCIM + "/Users")
                .queryParam("filter", "userName eq \"" + FILTER_LITERAL + "\""), null, 200);
        assertThat(probe.get("totalResults").asInt()).isZero();

        // ---- #11 login and failed login; #19 self-service change; #19 forced change ----
        MockHttpSession confined = logIn("audit-e2e-member", MEMBER_INITIAL);
        send(post("/api/auth/change-password").session(confined),
                "{\"currentPassword\":\"%s\",\"newPassword\":\"%s\"}"
                        .formatted(MEMBER_INITIAL, MEMBER_REPLACED), 204);
        assertThat(loginStatus("audit-e2e-member", WRONG_PASSWORD)).isEqualTo(401);
        send(post("/api/admin/accounts/" + member + "/force-password-change").session(admin),
                null, 200);

        // ---- #11 lockout set, lifted by Admin Unlock (there is no expiry lift) ----
        for (int attempt = 0; attempt < 5; attempt++) {
            assertThat(loginStatus("audit-e2e-locked", WRONG_PASSWORD)).isEqualTo(401);
        }
        send(post("/api/admin/accounts/" + locked + "/unlock").session(admin), null, 200);

        // Read the stored credential forms now, while they exist, to search for them later.
        List<String> storedSecrets = storedCredentialForms(tokenId, member, locked);

        send(post("/api/admin/connectors/" + connectorId + "/tokens/" + tokenId + "/revoke")
                .session(admin), null, 204);

        // ---- #18 both governance jobs, past both windows ----
        clock.advanceBy(DormancyPolicy.DEFAULT_AUTHORITY_REVOCATION_WINDOW.plusDays(1));
        assertThat(authorityRevocation.revokeDormantAuthority().processed()).contains(dormantAdmin);
        assertThat(deactivation.deactivateDormantUsers().processed()).contains(dormant);

        // ---- read it all back ----
        MockHttpSession reader = logIn(BOOTSTRAP_ADMIN, BOOTSTRAP_PASSWORD);
        StringBuilder listed = new StringBuilder();

        List<JsonNode> connectorEvents = listAll(reader, "resourceId=" + connectorId, listed);
        expect(single(connectorEvents, "CONNECTOR_CREATE", "SUCCESS"),
                adminId, "ScimConnector", connectorId);
        expect(single(connectorEvents, "CONNECTOR_TOKEN_ISSUE", "SUCCESS"),
                adminId, "ScimConnector", connectorId, "scope", "expiresAt");
        expect(single(connectorEvents, "CONNECTOR_TOKEN_REVOKE", "SUCCESS"),
                adminId, "ScimConnector", connectorId, "revokedAt");

        List<JsonNode> deletedEvents = listAll(reader, "resourceId=" + deleted, listed);
        expect(single(deletedEvents, "SCIM_USER_CREATE", "SUCCESS"), connectorId, "User", deleted);
        expect(single(deletedEvents, "SCIM_USER_REPLACE", "SUCCESS"),
                connectorId, "User", deleted, "displayName");
        expect(single(deletedEvents, "SCIM_USER_DELETE", "SUCCESS"), connectorId, "User", deleted);

        List<JsonNode> groupEvents = listAll(reader,
                "resourceId=%s&from=%s".formatted(adminGroup, start), listed);
        expect(single(groupEvents, "SCIM_GROUP_REPLACE", "SUCCESS"),
                connectorId, "Group", adminGroup, "members");

        JsonNode read = single(listAll(reader, "actorId=" + connectorId, listed),
                "SCIM_USER_LIST", "SUCCESS");
        expect(read, connectorId, "User", null);
        assertThat(read.get("resultCount").asInt()).isZero();
        assertThat(read.get("filterShape").asText())
                .as("the filter's shape is listed, its literal is not")
                .isEqualTo("userName eq ?");

        List<JsonNode> memberEvents = listAll(reader, "resourceId=" + member, listed);
        expect(single(memberEvents, "LOGIN_SUCCESS", "SUCCESS"), member, "User", member);
        JsonNode failedLogin = single(memberEvents, "LOGIN_FAILURE", "FAILURE");
        expect(failedLogin, null, "User", member, "failedLoginAttempts");
        assertThat(failedLogin.get("errorCode").asText()).isEqualTo("BAD_CREDENTIALS");
        expect(single(memberEvents, "PASSWORD_CHANGE", "SUCCESS"),
                member, "User", member, "password", "passwordChangeRequiredSince");
        expect(single(memberEvents, "PASSWORD_CHANGE_REQUIRE", "SUCCESS"),
                adminId, "User", member, "passwordChangeRequiredSince");

        List<JsonNode> lockedEvents = listAll(reader, "resourceId=" + locked, listed);
        assertThat(lockedEvents.stream()
                        .filter(event -> event.get("operation").asText().equals("LOGIN_FAILURE")))
                .as("every refused attempt, the one that locked it included")
                .hasSize(5);
        expect(single(lockedEvents, "LOCKOUT_SET", "SUCCESS"),
                null, "User", locked, "failedLoginAttempts", "lockedAt");
        expect(single(lockedEvents, "LOCKOUT_LIFT", "SUCCESS"),
                adminId, "User", locked, "failedLoginAttempts", "lockedAt");

        expect(single(listAll(reader, "resourceId=" + dormantAdmin, listed),
                "DORMANT_AUTHORITY_REVOCATION", "SUCCESS"), null, "User", dormantAdmin, "groups");
        expect(single(listAll(reader, "resourceId=" + dormant, listed),
                "INACTIVITY_DEACTIVATION", "SUCCESS"), null, "User", dormant, "active");

        // Everything this test caused, whatever it names, for the redaction sweep.
        List<JsonNode> everything = listAll(reader, "from=" + start, listed);
        assertThat(everything).as("the sweep must cover the events above")
                .hasSizeGreaterThanOrEqualTo(25);

        // ---- redaction ----
        String text = listed.toString();
        assertThat(text).as("non-vacuous: the text searched holds the events that would leak")
                .contains("SCIM_USER_LIST", "PASSWORD_CHANGE", "CONNECTOR_TOKEN_ISSUE",
                        "LOGIN_FAILURE", connectorId.toString());
        List<String> forbidden = new ArrayList<>(List.of(
                MEMBER_INITIAL, MEMBER_REPLACED, LOCKED_INITIAL, WRONG_PASSWORD, BOOTSTRAP_PASSWORD,
                bearer, bearer.substring(bearer.indexOf('.') + 1),
                FILTER_LITERAL, DISPLAY_NAME, RENAMED_DISPLAY_NAME,
                "audit-e2e-member", "audit-e2e-locked", "audit-e2e-deleted", "{argon2id}"));
        forbidden.addAll(storedSecrets);
        for (String value : forbidden) {
            assertThat(text).as("the listing must not contain a secret or profile value")
                    .doesNotContain(value);
        }
    }

    // ---- listing ------------------------------------------------------------------------

    /** Every event matching {@code query}, across every page, recording the raw text read. */
    private List<JsonNode> listAll(MockHttpSession session, String query, StringBuilder raw)
            throws Exception {
        List<JsonNode> events = new ArrayList<>();
        long pages = 1;
        for (int page = 0; page < pages; page++) {
            MvcResult result = mvc.perform(get("/api/admin/audit-events?%s&size=200&page=%d"
                    .formatted(query, page)).session(session)).andReturn();
            assertThat(result.getResponse().getStatus()).as(query).isEqualTo(200);
            String body = result.getResponse().getContentAsString();
            raw.append(body);
            JsonNode node = json.readTree(body);
            pages = node.get("totalPages").asLong();
            node.get("events").forEach(events::add);
        }
        return events;
    }

    /** The one event of {@code operation} with {@code outcome}; failing if there is none or more. */
    private static JsonNode single(List<JsonNode> events, String operation, String outcome) {
        List<JsonNode> matching = events.stream()
                .filter(event -> event.get("operation").asText().equals(operation))
                .filter(event -> event.get("outcome").asText().equals(outcome))
                .toList();
        assertThat(matching).as("exactly one %s %s", outcome, operation).hasSize(1);
        return matching.get(0);
    }

    /**
     * The event names who acted ({@code null}: nobody — a job, or an unauthenticated attempt), what
     * it acted on ({@code null} resource: none, as for a bulk read), when, and exactly which paths
     * it changed.
     */
    private static void expect(JsonNode event, UUID actor, String resourceType, UUID resource,
            String... paths) {
        String operation = event.get("operation").asText();
        assertThat(nullable(event.get("actorId"))).as("%s actor", operation)
                .isEqualTo(actor == null ? null : actor.toString());
        assertThat(event.get("resourceType").asText()).as("%s resource type", operation)
                .isEqualTo(resourceType);
        assertThat(nullable(event.get("resourceId"))).as("%s resource", operation)
                .isEqualTo(resource == null ? null : resource.toString());
        assertThat(Instant.parse(event.get("occurredAt").asText())).as("%s timestamp", operation)
                .isNotNull();
        assertThat(event.get("changedPaths").valueStream().map(JsonNode::asText).toList())
                .as("%s changed paths", operation)
                .containsExactlyInAnyOrder(paths);
    }

    private static String nullable(JsonNode value) {
        return value == null || value.isNull() ? null : value.asText();
    }

    // ---- credentials, as stored ---------------------------------------------------------

    /** The token's digest in every encoding a renderer could choose, and each stored hash. */
    private List<String> storedCredentialForms(UUID tokenId, UUID... usersWithPasswords) {
        byte[] digest = jdbc.queryForObject(
                "SELECT token_hash FROM scim_connector_tokens WHERE id = ?", byte[].class, tokenId);
        List<String> forms = new ArrayList<>(List.of(
                HexFormat.of().formatHex(digest),
                Base64.getEncoder().encodeToString(digest),
                Base64.getUrlEncoder().withoutPadding().encodeToString(digest)));
        for (UUID user : usersWithPasswords) {
            String hash = jdbc.queryForObject(
                    "SELECT password_hash FROM scim_users WHERE resource_id = ?", String.class, user);
            assertThat(hash).as("the User holds a stored hash to search for").isNotBlank();
            forms.add(hash);
            forms.add(hash.substring(hash.indexOf('}') + 1));
        }
        return forms;
    }

    // ---- HTTP ---------------------------------------------------------------------------

    private UUID scimCreate(String userName, String password) throws Exception {
        String body = password == null
                ? """
                  {"schemas":["%s"],"userName":"%s","displayName":"%s"}"""
                        .formatted(USER_SCHEMA, userName, DISPLAY_NAME)
                : """
                  {"schemas":["%s"],"userName":"%s","displayName":"%s","password":"%s"}"""
                        .formatted(USER_SCHEMA, userName, DISPLAY_NAME, password);
        UUID id = UUID.fromString(scim(post(SCIM + "/Users"), body, 201).get("id").asText());
        created.add(id);
        return id;
    }

    private JsonNode scim(MockHttpServletRequestBuilder request, String body, int expected)
            throws Exception {
        request.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer)
                .with(ScimConditionalWrites.currentVersion(jdbc));
        if (body != null) {
            request.contentType(SCIM_JSON).content(body);
        }
        MvcResult result = mvc.perform(request).andReturn();
        assertThat(result.getResponse().getStatus())
                .as(result.getResponse().getContentAsString())
                .isEqualTo(expected);
        String content = result.getResponse().getContentAsString();
        return content.isEmpty() ? null : json.readTree(content);
    }

    private JsonNode send(MockHttpServletRequestBuilder request, String body, int expected)
            throws Exception {
        withCsrf(request);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        MvcResult result = mvc.perform(request).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(expected);
        String content = result.getResponse().getContentAsString();
        return content.isEmpty() ? null : json.readTree(content);
    }

    private MockHttpSession logIn(String userName, String password) throws Exception {
        MvcResult login = mvc.perform(withCsrf(post("/api/auth/login"))
                        .session(new ClockedSession(context.getServletContext(), clock))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(userName, password)))
                .andReturn();
        assertThat(login.getResponse().getStatus()).as("login as %s", userName).isEqualTo(200);
        return (MockHttpSession) login.getRequest().getSession(false);
    }

    /**
     * A session created at the TEST clock's instant rather than the wall clock's.
     *
     * <p>The absolute-lifetime filter compares a session's creation time with the application
     * clock, and this context's clock is moved months ahead by the dormancy tests sharing it. A
     * plain {@code MockHttpSession} is created at real time, so once the clock has moved every
     * session would read as months old and be refused — depending only on which test class ran
     * first.
     */
    private static final class ClockedSession extends MockHttpSession {

        private final long createdAt;

        ClockedSession(ServletContext servletContext, Clock clock) {
            super(servletContext);
            this.createdAt = clock.millis();
        }

        @Override
        public long getCreationTime() {
            return createdAt;
        }
    }

    private int loginStatus(String userName, String password) throws Exception {
        return mvc.perform(withCsrf(post("/api/auth/login"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(userName, password)))
                .andReturn().getResponse().getStatus();
    }

    private static String credentials(String userName, String password) {
        return "{\"username\":\"%s\",\"password\":\"%s\"}".formatted(userName, password);
    }

    private UUID userId(String userName) {
        return users.findByNormalizedUserName(NormalizedUserName.of(userName)).orElseThrow().id();
    }

    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request) {
        CsrfToken token = csrfTokenRepository.generateToken(new MockHttpServletRequest());
        return request
                .cookie(new Cookie("XSRF-TOKEN", token.getToken()))
                .header("X-XSRF-TOKEN", token.getToken());
    }
}
