package com.example.backend.scim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.example.backend.ContainerTestConfiguration;
import com.example.backend.audit.domain.AuditOperation;
import com.example.backend.auth.InMemoryAccountSessions;
import com.example.backend.auth.domain.Account;
import com.example.backend.auth.domain.AccountRepository;
import com.example.backend.observability.RequestIdFilter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * The whole connector lifecycle driven through the real filter chains against a real
 * Postgres, with the audit rows read back out of the database with SQL.
 *
 * <p>This is the ticket's demo oracle. It is deliberately not a service-level test:
 * what is claimed is that an Admin's browser request and a connector's bearer request
 * travel two DIFFERENT security chains and each gets the right answer — which only
 * exists once the chains are wired, and which a test of the use cases could not
 * observe. The SCIM path called is one with no handler yet, so the signal is the
 * authentication outcome rather than a response body: {@code 401} before a token and
 * after its revocation, and anything else — here a {@code 404} from the absent handler
 * — in between.
 *
 * <p>Audit rows are read with SQL rather than through the repository for the same
 * reason the existing recording test does: what is asserted is the bytes that landed,
 * not a mapping's opinion of them.
 */
@SpringBootTest
@Import(ContainerTestConfiguration.class)
class ScimConnectorLifecycleIntegrationTests {

    /** A SCIM path that exists as a namespace but has no handler yet. */
    private static final String SCIM_PATH = "/scim/v2/Users";

    private static final String ADMIN = "test-admin";

    private static final String CONNECTORS = "/api/admin/connectors";

    /**
     * Events of one operation for ONE connector.
     *
     * <p>Scoped by subject rather than read whole and cleared between tests, which is
     * what lets this class leave the append-only table alone: emptying it would need the
     * retention role, and reaching for that role from a test of something else is the
     * arrangement {@code semgrep/rules/service-security.yml} exists to keep to one
     * place. A per-connector query is also the stronger assertion — "this connector's
     * history is exactly these events" rather than "the table happens to hold one row".
     */
    private static final String EVENTS_BY_OPERATION_AND_SUBJECT =
            "SELECT * FROM audit_events WHERE operation = ? AND subject_id = ?"
            + " ORDER BY occurred_at";

    private static final String ALL_EVENTS = "SELECT * FROM audit_events";

    private static final String ALIAS_COUNT =
            "SELECT count(*) FROM scim_external_ids WHERE connector_id = ?";

    private static final String INSERT_ALIAS =
            "INSERT INTO scim_external_ids (connector_id, resource_id, external_id)"
            + " VALUES (?, ?, ?)";

    private static final String TOKEN_ROWS =
            "SELECT * FROM scim_connector_tokens WHERE connector_id = ?";

    /** The session registry, in memory: this context has no Redis. */
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
    private AccountRepository accounts;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CsrfTokenRepository csrfTokenRepository;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    @Autowired
    private RequestIdFilter requestIdFilter;

    private final JsonMapper json = JsonMapper.builder().build();

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(requestIdFilter, springSecurityFilterChain)
                .build();
    }

    /**
     * The ticket's end-to-end criterion, as one walk: create, issue, call as the
     * connector, revoke, call again, and confirm each step left its event.
     */
    @Test
    void an_admin_creates_a_connector_issues_a_token_and_a_revocation_ends_its_access()
            throws Exception {
        // Before any credential exists, the namespace challenges.
        mvc.perform(get(SCIM_PATH))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(401));

        UUID connectorId = createConnector("Okta");
        String presentedValue = issueToken(connectorId, "READ_WRITE");

        // As that connector: past authentication, so no longer a 401. The path has no
        // handler yet, which is what a 404 here means — and a 404 is only reachable
        // once the bearer filter has accepted the credential.
        mvc.perform(get(SCIM_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + presentedValue))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .as("an authenticated connector is past the 401")
                        .isNotEqualTo(401));

        revokeFirstToken(connectorId);

        mvc.perform(get(SCIM_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + presentedValue))
                .andExpect(result -> {
                    assertThat(result.getResponse().getStatus()).isEqualTo(401);
                    assertThat(result.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE))
                            .isEqualTo("Bearer error=\"invalid_token\"");
                });

        assertOneEvent(AuditOperation.CONNECTOR_CREATE, connectorId);
        assertOneEvent(AuditOperation.CONNECTOR_TOKEN_ISSUE, connectorId);
        assertOneEvent(AuditOperation.CONNECTOR_TOKEN_REVOKE, connectorId);
    }

    /** No credential at all gets a bare challenge naming the scheme. */
    @Test
    void a_scim_request_with_no_credential_is_challenged_for_bearer() throws Exception {
        mvc.perform(get(SCIM_PATH))
                .andExpect(result -> {
                    assertThat(result.getResponse().getStatus()).isEqualTo(401);
                    assertThat(result.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE))
                            .isEqualTo("Bearer");
                });
    }

    @Test
    void a_malformed_token_is_refused_as_invalid_rather_than_challenged() throws Exception {
        mvc.perform(get(SCIM_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer garbage"))
                .andExpect(result -> {
                    assertThat(result.getResponse().getStatus()).isEqualTo(401);
                    assertThat(result.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE))
                            .isEqualTo("Bearer error=\"invalid_token\"");
                });
    }

    /**
     * The same value that authenticates in the header must not authenticate anywhere
     * else. Each of the three is presented over the real chain, so this is the
     * end-to-end form of the filter's unit claim.
     */
    @Test
    void a_token_is_never_accepted_from_a_query_string_a_form_body_or_a_cookie()
            throws Exception {
        UUID connectorId = createConnector("Okta");
        String presentedValue = issueToken(connectorId, "READ_WRITE");

        mvc.perform(get(SCIM_PATH).param("access_token", presentedValue))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(401));

        mvc.perform(post(SCIM_PATH)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("access_token", presentedValue))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(401));

        mvc.perform(get(SCIM_PATH)
                        .cookie(new Cookie("access_token", presentedValue)))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(401));
    }

    /** The plaintext is disclosed once, on a response a cache may not keep. */
    @Test
    void the_issue_response_discloses_the_plaintext_and_forbids_storing_it() throws Exception {
        UUID connectorId = createConnector("Okta");

        MvcResult issued = mvc.perform(asAdmin(post(CONNECTORS + "/" + connectorId + "/tokens"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scope\":\"READ_ONLY\"}"))
                .andReturn();

        assertThat(issued.getResponse().getStatus()).isEqualTo(201);
        assertThat(issued.getResponse().getHeader(HttpHeaders.CACHE_CONTROL))
                .contains("no-store");
        assertThat(body(issued).get("presentedValue").asText()).isNotBlank();
    }

    /** And it is never disclosed again: the listing carries no value for it. */
    @Test
    void the_listing_never_discloses_a_token_value() throws Exception {
        UUID connectorId = createConnector("Okta");
        String presentedValue = issueToken(connectorId, "READ_ONLY");

        MvcResult listed = mvc.perform(asAdmin(get(CONNECTORS))).andReturn();

        String rendered = listed.getResponse().getContentAsString();
        assertThat(rendered).doesNotContain(presentedValue);
        assertThat(rendered).doesNotContain("presentedValue");
        assertThat(rendered).contains(connectorId.toString());
    }

    /** The criterion: tokens and aliases go together, and the aliases genuinely existed. */
    @Test
    void deleting_a_connector_revokes_its_tokens_and_removes_its_aliases() throws Exception {
        UUID connectorId = createConnector("Okta");
        issueToken(connectorId, "READ_ONLY");
        issueToken(connectorId, "READ_WRITE");
        jdbc.update(INSERT_ALIAS, connectorId, UUID.randomUUID(), "okta-user-1");
        jdbc.update(INSERT_ALIAS, connectorId, UUID.randomUUID(), "okta-user-2");

        // The search-for-absence below is worth nothing unless there was something to
        // find first.
        assertThat(jdbc.queryForObject(ALIAS_COUNT, Integer.class, connectorId)).isEqualTo(2);
        assertThat(jdbc.queryForList(TOKEN_ROWS, connectorId)).hasSize(2);

        mvc.perform(asAdmin(delete(CONNECTORS + "/" + connectorId)))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(204));

        assertThat(jdbc.queryForObject(ALIAS_COUNT, Integer.class, connectorId)).isZero();
        assertThat(jdbc.queryForList(TOKEN_ROWS, connectorId))
                .hasSize(2)
                .allSatisfy(row -> assertThat(row.get("revoked_at")).isNotNull());
    }

    /**
     * Every connector and token event that landed, checked for shape: named by the
     * connector, carrying the acting Admin's stable id, and with no column anywhere in
     * the row holding the plaintext.
     */
    @Test
    void every_lifecycle_event_is_shaped_correctly_and_holds_no_token_value() throws Exception {
        UUID connectorId = createConnector("Okta");
        String presentedValue = issueToken(connectorId, "READ_WRITE");
        rotateFirstToken(connectorId);
        revokeFirstToken(connectorId);
        mvc.perform(asAdmin(delete(CONNECTORS + "/" + connectorId)));

        UUID adminId = accounts.findByUsername(ADMIN).map(Account::id).orElseThrow();
        List<AuditOperation> adminAttributed = List.of(
                AuditOperation.CONNECTOR_CREATE,
                AuditOperation.CONNECTOR_TOKEN_ISSUE,
                AuditOperation.CONNECTOR_TOKEN_ROTATE,
                AuditOperation.CONNECTOR_DELETE);

        for (AuditOperation operation : adminAttributed) {
            List<Map<String, Object>> rows = eventsOf(operation, connectorId);
            assertThat(rows).as("%s", operation).isNotEmpty();
            assertThat(rows).allSatisfy(row -> {
                assertThat(row.get("resource_type")).isEqualTo("ScimConnector");
                assertThat(row.get("subject_id")).hasToString(connectorId.toString());
                assertThat(row.get("resource_id")).hasToString(connectorId.toString());
                assertThat(row.get("actor_id")).hasToString(adminId.toString());
                assertThat(row.get("outcome")).isEqualTo("SUCCESS");
                assertThat(row.get("status_class")).isEqualTo("ok");
                assertThat(row.get("request_id")).isNotNull();
                assertThat(row.get("http_path")).asString().startsWith("/api/admin/connectors");
            });
        }

        // No row anywhere holds the credential, whichever column somebody reached for.
        assertThat(jdbc.queryForList(ALL_EVENTS)).isNotEmpty()
                .allSatisfy(row -> row.values().forEach(value ->
                        assertThat(String.valueOf(value)).doesNotContain(presentedValue)));
    }

    /** A rotation event exists and the old token's expiry only moved earlier. */
    @Test
    void rotation_records_its_event_and_never_lengthens_the_old_token() throws Exception {
        UUID connectorId = createConnector("Okta");
        issueToken(connectorId, "READ_WRITE");

        rotateFirstToken(connectorId);

        assertOneEvent(AuditOperation.CONNECTOR_TOKEN_ROTATE, connectorId);
        assertThat(jdbc.queryForList(TOKEN_ROWS, connectorId)).hasSize(2).allSatisfy(row ->
                assertThat((java.sql.Timestamp) row.get("expires_at"))
                        .isBeforeOrEqualTo((java.sql.Timestamp) row.get("original_expires_at")));
    }

    /** Discovery is readable without a credential; the resource endpoints are not. */
    @Test
    void discovery_is_public_while_a_resource_path_is_not() throws Exception {
        mvc.perform(get("/scim/v2/ServiceProviderConfig"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .as("public, so not a 401; no handler yet, so a 404")
                        .isNotEqualTo(401));

        mvc.perform(get(SCIM_PATH))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(401));
    }

    /**
     * An unknown SCIM path must not become the SPA shell. Without {@code /scim} being a
     * reserved server path this would be a 200 carrying HTML, which a provisioning
     * client would read as a successful empty response.
     */
    @Test
    void an_unknown_scim_path_is_not_answered_with_the_single_page_application() throws Exception {
        mvc.perform(get("/scim/v2/NotAResourceType"))
                .andExpect(result -> {
                    assertThat(result.getResponse().getStatus()).isEqualTo(401);
                    assertThat(result.getResponse().getContentAsString())
                            .doesNotContain("<html");
                });
    }

    /** Connector administration is an Admin capability on the session chain. */
    @Test
    void connector_administration_refuses_a_caller_who_is_not_an_admin() throws Exception {
        mvc.perform(get(CONNECTORS))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(401));

        mvc.perform(asRole(get(CONNECTORS), "ROLE_USER"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(403));
    }

    /** A bearer token buys nothing on the administration API. */
    @Test
    void a_connector_token_does_not_authenticate_the_administration_api() throws Exception {
        UUID connectorId = createConnector("Okta");
        String presentedValue = issueToken(connectorId, "READ_WRITE");

        mvc.perform(get(CONNECTORS).header(HttpHeaders.AUTHORIZATION, "Bearer " + presentedValue))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(401));
    }

    @Test
    void a_lifetime_past_the_ceiling_is_a_bad_request() throws Exception {
        UUID connectorId = createConnector("Okta");

        mvc.perform(asAdmin(post(CONNECTORS + "/" + connectorId + "/tokens"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scope\":\"READ_ONLY\",\"lifetimeDays\":400}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(400));
    }

    private UUID createConnector(String displayName) throws Exception {
        MvcResult created = mvc.perform(asAdmin(post(CONNECTORS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"" + displayName + "\"}"))
                .andReturn();
        assertThat(created.getResponse().getStatus()).isEqualTo(201);
        return UUID.fromString(body(created).get("id").asText());
    }

    private String issueToken(UUID connectorId, String scope) throws Exception {
        MvcResult issued = mvc.perform(asAdmin(post(CONNECTORS + "/" + connectorId + "/tokens"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scope\":\"" + scope + "\"}"))
                .andReturn();
        assertThat(issued.getResponse().getStatus()).isEqualTo(201);
        return body(issued).get("presentedValue").asText();
    }

    private void revokeFirstToken(UUID connectorId) throws Exception {
        UUID tokenId = firstTokenId(connectorId);
        mvc.perform(asAdmin(post(
                        CONNECTORS + "/" + connectorId + "/tokens/" + tokenId + "/revoke")))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(204));
    }

    private void rotateFirstToken(UUID connectorId) throws Exception {
        UUID tokenId = firstTokenId(connectorId);
        mvc.perform(asAdmin(post(
                                CONNECTORS + "/" + connectorId + "/tokens/" + tokenId + "/rotate"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"overlapDays\":7}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(201));
    }

    private UUID firstTokenId(UUID connectorId) throws Exception {
        MvcResult listed = mvc.perform(asAdmin(get(CONNECTORS))).andReturn();
        JsonNode connectors = json.readTree(listed.getResponse().getContentAsString());
        for (JsonNode connector : connectors) {
            if (connector.get("id").asText().equals(connectorId.toString())) {
                return UUID.fromString(connector.get("tokens").get(0).get("id").asText());
            }
        }
        throw new AssertionError("No listed connector with id " + connectorId);
    }

    private JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    private List<Map<String, Object>> eventsOf(AuditOperation operation, UUID connectorId) {
        return jdbc.queryForList(
                EVENTS_BY_OPERATION_AND_SUBJECT, operation.name(), connectorId);
    }

    private void assertOneEvent(AuditOperation operation, UUID connectorId) {
        assertThat(eventsOf(operation, connectorId)).as("%s", operation).singleElement()
                .satisfies(row -> {
                    assertThat(row.get("resource_type")).isEqualTo("ScimConnector");
                    assertThat(row.get("subject_id")).hasToString(connectorId.toString());
                });
    }

    /**
     * The SCIM chain is registered BEFORE the catch-all application chain.
     *
     * <p>Worth its own assertion because getting it wrong is easy and the symptom is
     * remote from the cause: the {@code @Order} has to sit on the {@code @Bean} method,
     * since a class-level one leaves the chain at lowest precedence. Spring Security
     * happens to refuse to start in that case, which is a good failure — but it refuses
     * because the catch-all becomes unreachable, and a future edit that gave the
     * application chain a {@code securityMatcher} would remove that protection while
     * leaving the inverted order in place.
     */
    @Test
    void the_scim_chain_is_registered_ahead_of_the_application_chain() {
        List<SecurityFilterChain> chains =
                ((FilterChainProxy) springSecurityFilterChain).getFilterChains();

        MockHttpServletRequest scim = new MockHttpServletRequest("GET", SCIM_PATH);
        scim.setRequestURI(SCIM_PATH);

        assertThat(chains).hasSizeGreaterThanOrEqualTo(2);
        assertThat(chains.getFirst().matches(scim))
                .as("the first chain owns the SCIM namespace")
                .isTrue();
        assertThat(chains.getLast().matches(new MockHttpServletRequest("GET", "/anything")))
                .as("the last chain is the catch-all")
                .isTrue();
    }

    /** An authenticated Admin request, with a session and a matching CSRF token. */
    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return asRole(request, "ROLE_ADMIN");
    }

    private MockHttpServletRequestBuilder asRole(
            MockHttpServletRequestBuilder request, String authority) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(
                new TestingAuthenticationToken(ADMIN, null, authority));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                securityContext);

        CsrfToken csrfToken = csrfTokenRepository.generateToken(new MockHttpServletRequest());
        return request.session(session)
                .cookie(new Cookie("XSRF-TOKEN", csrfToken.getToken()))
                .header("X-XSRF-TOKEN", csrfToken.getToken());
    }
}
