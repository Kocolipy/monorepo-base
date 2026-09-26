package com.example.backend.scim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.example.backend.ContainerTestConfiguration;
import com.example.backend.InMemorySessionRegistryConfiguration;
import com.example.backend.audit.domain.AuditOperation;
import com.example.backend.observability.RequestIdFilter;
import com.example.backend.scim.application.ConnectorAdministrationService;
import com.example.backend.scim.domain.ConnectorTokenScope;
import com.example.backend.scim.domain.ScimExternalIdRepository;
import jakarta.servlet.Filter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The ticket's demo oracle: a connector drives creation and retrieval over the real filter
 * chain against a real Postgres, and the stored rows and audit rows are read back with SQL.
 *
 * <p>SQL rather than a repository for the state assertions, for the reason the connector
 * lifecycle test gives: what is claimed about a password is a claim about the BYTES that
 * landed, and a mapping's opinion of them is not the same evidence.
 */
@SpringBootTest
@Import({ContainerTestConfiguration.class, InMemorySessionRegistryConfiguration.class})
@TestPropertySource(properties = "app.scim.enabled=true")
class ScimUserProvisioningIntegrationTests {

    private static final String USERS = "/scim/v2/Users";

    private static final String USER_SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:User";

    private static final MediaType SCIM_JSON = MediaType.valueOf("application/scim+json");

    private static final String PASSWORD = "correct horse battery staple";

    private static final String USER_EVENTS_BY_ACTOR =
            "SELECT * FROM audit_events WHERE operation = ? AND actor_id = ? ORDER BY occurred_at";

    private static final String HASH_OF =
            "SELECT password_hash FROM scim_users WHERE resource_id = ?";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ConnectorAdministrationService connectors;

    @Autowired
    private ScimExternalIdRepository aliases;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RequestIdFilter requestIdFilter;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    private final JsonMapper json = JsonMapper.builder().build();

    private MockMvc mvc;

    private UUID connectorId;

    private String writeToken;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(requestIdFilter, springSecurityFilterChain)
                .build();
        connectorId = connectors.create("Okta", "test-admin").id();
        writeToken = issueTokenFor(connectorId, ConnectorTokenScope.READ_WRITE);
    }

    /**
     * A create with a password: the canonical resource, its location and its validator, and
     * no trace of the credential anywhere in the response.
     */
    @Test
    void a_user_is_created_with_a_password_that_never_appears_in_the_response()
            throws Exception {
        MvcResult created = create("""
                {"schemas":["%s"],
                 "userName":"bjensen",
                 "externalId":"701984",
                 "name":{"givenName":"Barbara","familyName":"Jensen"},
                 "displayName":"Babs Jensen",
                 "emails":[{"value":"bjensen@example.com","type":"work","primary":true}],
                 "password":"%s"}""".formatted(USER_SCHEMA, PASSWORD));

        assertThat(created.getResponse().getStatus()).isEqualTo(201);
        JsonNode body = body(created);
        UUID id = UUID.fromString(body.get("id").asText());

        assertThat(created.getResponse().getHeader(HttpHeaders.LOCATION))
                .endsWith(USERS + "/" + id);
        assertThat(created.getResponse().getHeader(HttpHeaders.ETAG))
                .isEqualTo("\"1\"")
                .isEqualTo(body.get("meta").get("version").asText());
        assertThat(body.get("schemas").get(0).asText()).isEqualTo(USER_SCHEMA);
        assertThat(body.get("userName").asText()).isEqualTo("bjensen");
        assertThat(body.get("externalId").asText()).isEqualTo("701984");
        assertThat(body.get("active").booleanValue())
                .as("active defaults to true on create")
                .isTrue();
        assertThat(body.get("meta").get("resourceType").asText()).isEqualTo("User");
        assertThat(body.get("meta").get("created").asText()).endsWith("Z");

        // The whole response body, not only the attributes looked at above: neither the
        // plaintext nor the stored hash is anywhere in it.
        String rendered = created.getResponse().getContentAsString();
        assertThat(rendered).doesNotContain(PASSWORD);
        assertThat(rendered).doesNotContain("password");
        assertThat(rendered).doesNotContain(storedHash(id));

        // Hashed immediately: the stored value verifies the password and is not it.
        String hash = storedHash(id);
        assertThat(hash).isNotEqualTo(PASSWORD).startsWith("{argon2id}");
        assertThat(passwordEncoder.matches(PASSWORD, hash)).isTrue();
    }

    /** A credentialless User is a supported state, not a rejected one. */
    @Test
    void a_user_is_created_without_a_password_and_stores_no_hash() throws Exception {
        MvcResult created = create("""
                {"schemas":["%s"],"userName":"credentialless"}""".formatted(USER_SCHEMA));

        assertThat(created.getResponse().getStatus()).isEqualTo(201);
        UUID id = UUID.fromString(body(created).get("id").asText());
        assertThat(storedHash(id)).isNull();
    }

    /**
     * Read-only attributes in the body are ignored rather than refused, so a client may
     * round-trip a resource — and the server's own id wins, which is what stops a connector
     * choosing its resource ids.
     */
    @Test
    void read_only_attributes_in_the_request_are_ignored() throws Exception {
        UUID chosenByClient = UUID.fromString("8a5c1f4e-0000-4000-8000-0000000000ff");

        MvcResult created = create("""
                {"schemas":["%s"],
                 "userName":"read-only-ignored",
                 "id":"%s",
                 "groups":[{"value":"whatever"}],
                 "meta":{"resourceType":"Group","version":"\\"99\\""}}"""
                .formatted(USER_SCHEMA, chosenByClient));

        assertThat(created.getResponse().getStatus()).isEqualTo(201);
        JsonNode body = body(created);
        assertThat(UUID.fromString(body.get("id").asText())).isNotEqualTo(chosenByClient);
        assertThat(body.get("meta").get("resourceType").asText()).isEqualTo("User");
        assertThat(body.get("meta").get("version").asText()).isEqualTo("\"1\"");
    }

    /** A live {@code userName} is taken, case-insensitively, and the refusal is a conflict. */
    @Test
    void a_live_user_name_conflict_is_refused_as_a_uniqueness_conflict() throws Exception {
        create("""
                {"schemas":["%s"],"userName":"taken"}""".formatted(USER_SCHEMA));
        long before = users();

        MvcResult refused = create("""
                {"schemas":["%s"],"userName":"TAKEN"}""".formatted(USER_SCHEMA));

        assertThat(refused.getResponse().getStatus()).isEqualTo(409);
        JsonNode error = body(refused);
        assertThat(error.get("schemas").get(0).asText())
                .isEqualTo("urn:ietf:params:scim:api:messages:2.0:Error");
        assertThat(error.get("status").asText()).isEqualTo("409");
        assertThat(error.get("scimType").asText()).isEqualTo("uniqueness");
        assertThat(users()).as("the refused create stored nothing").isEqualTo(before);

        // The refusal is recorded, so a connector retrying against a name it will never get
        // is visible in the trail rather than silent.
        assertThat(eventsOf(AuditOperation.SCIM_USER_CREATE))
                .anySatisfy(event -> {
                    assertThat(event.get("outcome")).isEqualTo("FAILURE");
                    assertThat(event.get("error_code")).isEqualTo("UNIQUENESS");
                    assertThat(event.get("subject_id")).isNull();
                });
    }

    /**
     * The read-only-token scope check, deferred from the connector ticket, now against a real
     * mutating endpoint: refused before the handler, and nothing created.
     */
    @Test
    void a_read_only_token_cannot_create_and_creates_nothing() throws Exception {
        String readOnly = issueTokenFor(connectorId, ConnectorTokenScope.READ_ONLY);
        long before = users();

        MvcResult refused = mvc.perform(post(USERS)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + readOnly)
                        .contentType(SCIM_JSON)
                        .content("""
                                {"schemas":["%s"],"userName":"by-read-only"}"""
                                .formatted(USER_SCHEMA)))
                .andReturn();

        assertThat(refused.getResponse().getStatus()).isEqualTo(403);
        assertThat(refused.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE))
                .isEqualTo("Bearer error=\"insufficient_scope\"");
        assertThat(users()).isEqualTo(before);

        // And the same token reads perfectly well, so the refusal was about the mutation and
        // not about the credential.
        assertThat(mvc.perform(get(USERS).header(HttpHeaders.AUTHORIZATION, "Bearer " + readOnly))
                        .andReturn().getResponse().getStatus())
                .isEqualTo(200);
    }

    /**
     * The criterion: two connectors hold distinct aliases for one User and each sees only
     * its own.
     *
     * <p>The second connector's alias is written through the port rather than over HTTP
     * because the only connector-facing write that sets an alias on an EXISTING resource is
     * PUT, which arrives with the conditional-write ticket. The port is the production write
     * path either way; what this test drives over HTTP is the read, which is where a leak
     * between namespaces would show.
     */
    @Test
    void two_connectors_hold_distinct_aliases_for_one_user_and_see_only_their_own()
            throws Exception {
        MvcResult created = create("""
                {"schemas":["%s"],"userName":"shared","externalId":"okta-1"}"""
                .formatted(USER_SCHEMA));
        UUID id = UUID.fromString(body(created).get("id").asText());

        UUID otherConnector = connectors.create("Entra", "test-admin").id();
        String otherToken = issueTokenFor(otherConnector, ConnectorTokenScope.READ_WRITE);
        aliases.put(otherConnector, id, "entra-1");

        assertThat(readAs(writeToken, id).get("externalId").asText()).isEqualTo("okta-1");
        assertThat(readAs(otherToken, id).get("externalId").asText()).isEqualTo("entra-1");

        // Neither response carries the other's value at all, not merely in that attribute.
        assertThat(rawAs(writeToken, id)).doesNotContain("entra-1");
        assertThat(rawAs(otherToken, id)).doesNotContain("okta-1");

        // A third connector set none, and sees none rather than inheriting one.
        UUID thirdConnector = connectors.create("Ping", "test-admin").id();
        String thirdToken = issueTokenFor(thirdConnector, ConnectorTokenScope.READ_WRITE);
        assertThat(readAs(thirdToken, id).get("externalId")).isNull();
    }

    @Test
    void a_single_resource_read_returns_the_canonical_resource_with_its_validator()
            throws Exception {
        UUID id = UUID.fromString(body(create("""
                {"schemas":["%s"],"userName":"readable"}""".formatted(USER_SCHEMA)))
                .get("id").asText());

        MvcResult read = mvc.perform(asConnector(get(USERS + "/" + id))).andReturn();

        assertThat(read.getResponse().getStatus()).isEqualTo(200);
        assertThat(read.getResponse().getContentType()).startsWith("application/scim+json");
        assertThat(read.getResponse().getHeader(HttpHeaders.ETAG))
                .isEqualTo(body(read).get("meta").get("version").asText());
    }

    @Test
    void an_unknown_id_and_a_malformed_id_are_both_not_found() throws Exception {
        MvcResult unknown = mvc.perform(asConnector(
                        get(USERS + "/8a5c1f4e-0000-4000-8000-00000000dead")))
                .andReturn();

        assertThat(unknown.getResponse().getStatus()).isEqualTo(404);
        assertThat(body(unknown).get("status").asText()).isEqualTo("404");

        assertThat(mvc.perform(asConnector(get(USERS + "/not-a-uuid")))
                        .andReturn().getResponse().getStatus())
                .as("telling a caller its id was the wrong shape says what shape the real ones are")
                .isEqualTo(404);
    }

    /**
     * The collection: a {@code ListResponse} whose total is the directory's and whose page is
     * the default one, since the request asked for no paging at all.
     */
    @Test
    void the_collection_reports_the_total_and_defaults_to_the_standard_page() throws Exception {
        long existing = users();
        create("""
                {"schemas":["%s"],"userName":"page-a"}""".formatted(USER_SCHEMA));
        create("""
                {"schemas":["%s"],"userName":"page-b"}""".formatted(USER_SCHEMA));
        create("""
                {"schemas":["%s"],"userName":"page-c"}""".formatted(USER_SCHEMA));

        JsonNode listing = body(mvc.perform(asConnector(get(USERS))).andReturn());

        assertThat(listing.get("schemas").get(0).asText())
                .isEqualTo("urn:ietf:params:scim:api:messages:2.0:ListResponse");
        assertThat(listing.get("totalResults").asLong()).isEqualTo(existing + 3);
        assertThat(listing.get("startIndex").asInt()).isEqualTo(1);
        assertThat(listing.get("itemsPerPage").asInt()).isEqualTo((int) (existing + 3));
        assertThat(listing.get("Resources").size()).isEqualTo((int) (existing + 3));
    }

    /** Paging: a count bounds the page without changing the total, and a start index skips. */
    @Test
    void a_count_bounds_the_page_and_the_total_stays_the_whole_directory() throws Exception {
        create("""
                {"schemas":["%s"],"userName":"count-a"}""".formatted(USER_SCHEMA));
        create("""
                {"schemas":["%s"],"userName":"count-b"}""".formatted(USER_SCHEMA));
        long total = users();

        JsonNode firstPage = body(mvc.perform(asConnector(get(USERS).param("count", "1")))
                .andReturn());

        assertThat(firstPage.get("totalResults").asLong()).isEqualTo(total);
        assertThat(firstPage.get("itemsPerPage").asInt()).isEqualTo(1);
        assertThat(firstPage.get("Resources").size()).isEqualTo(1);

        // count=0 is a request for the total with no resources, not an omission.
        JsonNode countZero = body(mvc.perform(asConnector(get(USERS).param("count", "0")))
                .andReturn());
        assertThat(countZero.get("totalResults").asLong()).isEqualTo(total);
        assertThat(countZero.get("itemsPerPage").asInt()).isZero();
        assertThat(countZero.get("Resources")).isNull();

        // A start index past the end is an empty page, not an error.
        JsonNode pastTheEnd = body(mvc.perform(asConnector(
                        get(USERS).param("startIndex", String.valueOf(total + 50))))
                .andReturn());
        assertThat(pastTheEnd.get("totalResults").asLong()).isEqualTo(total);
        assertThat(pastTheEnd.get("itemsPerPage").asInt()).isZero();
    }

    /**
     * The criterion: asking for the password omits it silently, on both reads, and never
     * errors.
     */
    @Test
    void asking_for_the_password_omits_it_without_erroring() throws Exception {
        UUID id = UUID.fromString(body(create("""
                {"schemas":["%s"],"userName":"projected","password":"%s"}"""
                .formatted(USER_SCHEMA, PASSWORD)))
                .get("id").asText());

        MvcResult single = mvc.perform(asConnector(
                        get(USERS + "/" + id).param("attributes", "password")))
                .andReturn();
        assertThat(single.getResponse().getStatus()).isEqualTo(200);
        assertThat(single.getResponse().getContentAsString())
                .doesNotContain("password")
                .doesNotContain(PASSWORD);
        assertThat(body(single).get("id").asText()).isEqualTo(id.toString());

        MvcResult collection = mvc.perform(asConnector(
                        get(USERS).param("attributes", "password")))
                .andReturn();
        assertThat(collection.getResponse().getStatus()).isEqualTo(200);
        assertThat(collection.getResponse().getContentAsString())
                .doesNotContain("password")
                .doesNotContain(PASSWORD);

        // And excluding it is equally uneventful.
        assertThat(mvc.perform(asConnector(
                        get(USERS + "/" + id).param("excludedAttributes", "password")))
                        .andReturn().getResponse().getStatus())
                .isEqualTo(200);
    }

    /** A malformed body, a wrong schema and a missing required attribute: all SCIM errors. */
    @Test
    void every_refusal_this_endpoint_produces_is_a_scim_error_document() throws Exception {
        assertRefusal(create("""
                {"schemas":["%s"]}""".formatted(USER_SCHEMA)), 400, "invalidSyntax");
        assertRefusal(create("""
                {"userName":"no-schemas"}"""), 400, "invalidSyntax");
        assertRefusal(create("""
                {"schemas":["urn:ietf:params:scim:schemas:core:2.0:Group"],
                 "userName":"wrong-schema"}"""), 400, "invalidValue");
        assertRefusal(create("""
                {"schemas":["%s","urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"],
                 "userName":"extension"}""".formatted(USER_SCHEMA)), 400, "invalidValue");
        assertRefusal(create("""
                {"schemas":["%s"],"userName":"bad-type","active":"yes"}"""
                .formatted(USER_SCHEMA)), 400, "invalidValue");
        assertRefusal(create("""
                {"schemas":["%s"],"userName":"bad-sub","name":{"nickName":"Babs"}}"""
                .formatted(USER_SCHEMA)), 400, "invalidValue");
        assertRefusal(
                mvc.perform(asConnector(get(USERS)
                                .param("attributes", "userName")
                                .param("excludedAttributes", "meta")))
                        .andReturn(),
                400,
                "invalidValue");
    }

    /**
     * A paging parameter that is not an integer is a {@code 400 invalidValue} naming the
     * parameter, not a container's own type-conversion failure: the parameters are read as
     * strings and parsed here precisely so the refusal is a SCIM error document.
     */
    @ParameterizedTest
    @CsvSource({"startIndex,abc", "startIndex,1.5", "count,abc", "count,''", "count,-"})
    void a_paging_parameter_that_is_not_an_integer_is_a_scim_refusal(
            String parameter, String value) throws Exception {
        MvcResult result = mvc.perform(asConnector(get(USERS).param(parameter, value)))
                .andReturn();

        if (value.isBlank()) {
            // Blank is "absent", which is the default rather than a refusal.
            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            return;
        }
        assertRefusal(result, 400, "invalidValue");
        assertThat(result.getResponse().getContentAsString()).contains(parameter);
    }

    /**
     * Audit: a create and a collection read each leave one event, and a single-resource read
     * leaves none.
     */
    @Test
    void a_create_and_a_bulk_read_are_audited_and_a_single_read_is_not() throws Exception {
        UUID id = UUID.fromString(body(create("""
                {"schemas":["%s"],"userName":"audited"}""".formatted(USER_SCHEMA)))
                .get("id").asText());

        assertThat(eventsOf(AuditOperation.SCIM_USER_CREATE))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.get("subject_id")).hasToString(id.toString());
                    assertThat(event.get("resource_id")).hasToString(id.toString());
                    assertThat(event.get("resource_type")).isEqualTo("User");
                    assertThat(event.get("outcome")).isEqualTo("SUCCESS");
                    assertThat(event.get("status_class")).isEqualTo("ok");
                    assertThat(event.get("http_method")).isEqualTo("POST");
                    assertThat(event.get("request_id")).isNotNull();
                });

        mvc.perform(asConnector(get(USERS + "/" + id))).andReturn();
        assertThat(eventsOf(AuditOperation.SCIM_USER_LIST))
                .as("a single-resource read is the ordinary unit of traffic and is not recorded")
                .isEmpty();

        mvc.perform(asConnector(get(USERS))).andReturn();
        assertThat(eventsOf(AuditOperation.SCIM_USER_LIST))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.get("resource_type")).isEqualTo("User");
                    assertThat(event.get("subject_id"))
                            .as("a collection read names no one subject")
                            .isNull();
                    assertThat(event.get("http_method")).isEqualTo("GET");
                });

        // An empty result is the same act as a full one, so it is recorded the same way.
        mvc.perform(asConnector(get(USERS).param("count", "0"))).andReturn();
        assertThat(eventsOf(AuditOperation.SCIM_USER_LIST)).hasSize(2);
    }

    /** No audit event anywhere carries the password or a hash. */
    @Test
    void no_recorded_event_carries_a_credential() throws Exception {
        create("""
                {"schemas":["%s"],"userName":"never-logged","password":"%s"}"""
                .formatted(USER_SCHEMA, PASSWORD));

        assertThat(jdbc.queryForList("SELECT * FROM audit_events").toString())
                .doesNotContain(PASSWORD)
                .doesNotContain("argon2id")
                .doesNotContain("never-logged");
    }

    private MvcResult create(String body) throws Exception {
        return mvc.perform(asConnector(post(USERS)).contentType(SCIM_JSON).content(body))
                .andReturn();
    }

    private JsonNode readAs(String token, UUID id) throws Exception {
        return json.readTree(rawAs(token, id));
    }

    private String rawAs(String token, UUID id) throws Exception {
        MvcResult read = mvc.perform(get(USERS + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andReturn();
        assertThat(read.getResponse().getStatus()).isEqualTo(200);
        return read.getResponse().getContentAsString();
    }

    private void assertRefusal(MvcResult result, int status, String scimType) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(status);
        JsonNode error = body(result);
        assertThat(error.get("schemas").get(0).asText())
                .isEqualTo("urn:ietf:params:scim:api:messages:2.0:Error");
        assertThat(error.get("status").asText()).isEqualTo(String.valueOf(status));
        assertThat(error.get("scimType").asText()).isEqualTo(scimType);
        assertThat(error.get("detail").asText()).isNotBlank();
    }

    private MockHttpServletRequestBuilder asConnector(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + writeToken);
    }

    private String issueTokenFor(UUID connector, ConnectorTokenScope scope) {
        return connectors.issueToken(connector, scope, null, "test-admin").presentedValue();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    private List<Map<String, Object>> eventsOf(AuditOperation operation) {
        return jdbc.queryForList(USER_EVENTS_BY_ACTOR, operation.name(), connectorId);
    }

    private String storedHash(UUID id) {
        return jdbc.queryForObject(HASH_OF, String.class, id);
    }

    private long users() {
        return jdbc.queryForObject("SELECT count(*) FROM scim_users", Long.class);
    }
}
