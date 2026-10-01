package com.example.backend.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.scim.domain.NormalizedUserName;
import com.example.backend.scim.domain.ScimUserRepository;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The log stream as an operator and a collector meet it: real requests through
 * the real filter chain, and every record they produce encoded in the format the
 * deployed service emits.
 *
 * <p>Two properties are asserted together because they hold each other up. The
 * output is ECS JSON, so a record's variable parts are named fields; and no
 * forbidden value appears in any of them, because a value that is never
 * concatenated into a message has nowhere to hide. Testing the format without the
 * redaction would bless a well-formed record that leaks a password; testing the
 * redaction without the format would pass over output no collector can read.
 *
 * <p>The three flows are the ones an investigation actually reads — an accepted
 * login, a refused login, and an administrative change — and are exactly the
 * three whose inputs are sensitive.
 */
@SpringBootTest
@Import(com.example.backend.ContainerTestConfiguration.class)
class EcsLogFormatTests {

    /**
     * Values that must never appear anywhere in the log stream. The credentials and
     * identifiers are the test fixtures' own, so a leak shows up as a literal
     * match; the rest are the shapes a secret takes in this service — a BCrypt
     * hash's prefix, and the two cookie names whose values are a session and a CSRF
     * token.
     */
    private static final List<String> FORBIDDEN = List.of(
            "test-user",
            "test-admin",
            "test-password",
            "test-admin-password",
            "not-a-real-account",
            "wrong-password",
            "$2a$",
            "JSESSIONID",
            "XSRF-TOKEN");

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private Environment environment;

    @Autowired
    private RequestIdFilter requestIdFilter;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    @Autowired
    private CsrfTokenRepository csrfTokenRepository;

    @Autowired
    private ScimUserRepository users;

    private MockMvc mvc;

    private EcsLogCapture logs;

    @BeforeEach
    void setUp() {
        // The request-id filter ahead of the security chain, as the deployed
        // ordering has it: a request refused by the chain must still be logged
        // under an id.
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(requestIdFilter, springSecurityFilterChain)
                .build();
        logs = EcsLogCapture.attach(environment);
    }

    @AfterEach
    void tearDown() {
        logs.close();
    }

    /**
     * The deployed value, not a copy of it. `logging.yaml` exists precisely so this
     * can be read: the test resources' `application.yaml` shadows the main one, so
     * a setting stated there would be invisible here and this test would be
     * asserting a duplicate of the production configuration rather than the
     * production configuration.
     */
    @Test
    void theDeployedConfigurationSelectsEcs() throws Exception {
        MutablePropertySources sources = new MutablePropertySources();
        for (PropertySource<?> source
                : new YamlPropertySourceLoader().load("logging", new ClassPathResource("logging.yaml"))) {
            sources.addLast(source);
        }

        // Resolved against this document alone, so the assertion is about the
        // committed default and not about whatever LOG_STRUCTURED_FORMAT happens to
        // be set to on the machine running the test.
        assertThat(new PropertySourcesPropertyResolver(sources)
                .getProperty("logging.structured.format.console"))
                .isEqualTo("ecs");
    }

    @Test
    void anAcceptedLoginIsOneEcsRecordCarryingTheRequestsCorrelationId() throws Exception {
        logIn("test-user", "test-password").andExpect(status().isOk());

        JsonNode record = onlyRecordWithMessage("Login accepted");

        assertThatIsValidEcs(record);
        assertThatClassifiedAs(record, "user-authentication", "process", "user", "allowed");
        assertThat(record.has("app")).as("an exact action keeps no local name").isFalse();
        assertThat(record.at("/event/outcome").asText()).isEqualTo("success");
        assertThat(record.at("/http/request/id").asText()).isNotBlank();
        assertThat(record.at("/log/level").asText()).isEqualTo("INFO");
    }

    /**
     * The login-success record names the identity that logged in, by its stable id —
     * set on the record itself, because the session's principal index that carries it
     * on later requests is written only after the record is emitted.
     */
    @Test
    void anAcceptedLoginCarriesTheIdentitysStableId() throws Exception {
        logIn("test-user", "test-password").andExpect(status().isOk());

        JsonNode record = onlyRecordWithMessage("Login accepted");

        assertThat(record.at("/user/id").asText()).isEqualTo(userId("test-user").toString());
    }

    /**
     * A refusal has to say what kind of refusal it was — a run of wrong passwords
     * reads differently from a run against accounts that do not exist — without
     * saying which account was named.
     */
    @Test
    void aRefusedLoginIsOneEcsRecordNamingTheRefusalKindAndNoSubmittedValue()
            throws Exception {
        logIn("not-a-real-account", "wrong-password").andExpect(status().isUnauthorized());

        JsonNode record = onlyRecordWithMessage("Login refused");

        assertThatIsValidEcs(record);
        assertThatClassifiedAs(record, "user-authentication", "process", "user", "denied");
        assertThat(record.at("/event/outcome").asText()).isEqualTo("failure");
        assertThat(record.at("/event/reason").asText()).isEqualTo("BadCredentialsException");
        assertThat(record.at("/http/request/id").asText()).isNotBlank();
        assertThat(record.at("/log/level").asText()).isEqualTo("WARN");
        assertThat(record.has("user")).as("an unresolved identity carries no user field").isFalse();
    }

    /**
     * A refused attempt made from a session that is already authenticated still names
     * nobody: the session's User is not whom the attempt was for, so the record must not
     * inherit the request's {@code user.id}. The session's id really is in the context —
     * the later administrative record in the same session carries it — so the absence is
     * the refusal's doing, not the context's.
     */
    @Test
    void aRefusedLoginFromAnAuthenticatedSessionStillCarriesNoUserField() throws Exception {
        MockHttpSession admin = loggedInSession("test-admin", "test-admin-password");
        logs.reset();

        mvc.perform(withCsrf(post("/api/auth/login"))
                        .session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"not-a-real-account\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(withCsrf(post("/api/admin/accounts/{id}/unlock", userId("test-user")))
                        .session(admin))
                .andExpect(status().isOk());

        assertThat(onlyRecordWithMessage("Login refused").has("user")).isFalse();
        assertThat(onlyRecordWithMessage("Administrative identity change applied")
                        .at("/user/id").asText())
                .isEqualTo(userId("test-admin").toString());
    }

    /**
     * An authenticated request's records name the caller by the stable id its session's
     * principal index holds, and an administrative change names the identity acted on
     * separately, as {@code user.target.id}.
     */
    @Test
    void anAdministrativeChangeIsOneEcsRecordNamingTheActorAndTheSubjectByStableId()
            throws Exception {
        MockHttpSession admin = loggedInSession("test-admin", "test-admin-password");
        logs.reset();

        mvc.perform(withCsrf(post("/api/admin/accounts/{id}/unlock", userId("test-user")))
                        .session(admin))
                .andExpect(status().isOk());

        JsonNode record = onlyRecordWithMessage("Administrative identity change applied");

        assertThatIsValidEcs(record);
        assertThatClassifiedAs(record, "access-control", "process", "admin", "user", "change");
        assertThat(record.at("/app/event/action").asText()).isEqualTo("identity.unlock");
        assertThat(record.at("/event/outcome").asText()).isEqualTo("success");
        assertThat(record.at("/http/request/id").asText()).isNotBlank();
        assertThat(record.at("/user/id").asText()).isEqualTo(userId("test-admin").toString());
        assertThat(record.at("/user/target/id").asText())
                .isEqualTo(userId("test-user").toString());
    }

    /**
     * Each connector token lifecycle write names the administrator who made it. No
     * {@code user.target.id}: the subject is a connector, which is not a User.
     */
    @Test
    void everyConnectorLifecycleRecordCarriesTheActorsStableId() throws Exception {
        MockHttpSession admin = loggedInSession("test-admin", "test-admin-password");
        logs.reset();

        String connectorId = json(mvc.perform(withCsrf(post("/api/admin/connectors"))
                        .session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"ecs-log-connector\"}"))
                .andExpect(status().isCreated())).at("/id").asText();
        String issuedId = json(mvc.perform(withCsrf(
                                post("/api/admin/connectors/{c}/tokens", connectorId))
                        .session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scope\":\"READ_WRITE\"}"))
                .andExpect(status().isCreated())).at("/tokenId").asText();
        String rotatedId = json(mvc.perform(withCsrf(
                                post("/api/admin/connectors/{c}/tokens/{t}/rotate",
                                        connectorId, issuedId))
                        .session(admin))
                .andExpect(status().isCreated())).at("/tokenId").asText();
        mvc.perform(withCsrf(post("/api/admin/connectors/{c}/tokens/{t}/revoke",
                                connectorId, rotatedId))
                        .session(admin))
                .andExpect(status().isNoContent());
        mvc.perform(withCsrf(delete("/api/admin/connectors/{c}", connectorId)).session(admin))
                .andExpect(status().isNoContent());

        List<JsonNode> lifecycle = recordsWithMessage("SCIM connector lifecycle change applied");
        assertThat(lifecycle)
                .extracting(record -> record.at("/app/event/action").asText())
                .containsExactly(
                        "scim.connector.create",
                        "scim.connector.token.issue",
                        "scim.connector.token.rotate",
                        "scim.connector.token.revoke",
                        "scim.connector.delete");
        assertThat(lifecycle).allSatisfy(record -> {
            assertThat(record.at("/user/id").asText()).isEqualTo(userId("test-admin").toString());
            assertThat(record.at("/user").has("target")).isFalse();
        });
        assertThatClassifiedAs(lifecycle.get(1),
                "access-control", "configuration", "admin", "creation");
        assertThatClassifiedAs(lifecycle.get(2),
                "access-control", "configuration", "admin", "change");
        assertThatClassifiedAs(lifecycle.get(3),
                "access-control", "configuration", "admin", "deletion");
    }

    /**
     * The ticket's oracle. All three flows in one exchange sequence, then the whole
     * captured stream read as JSON and searched for every value that must not be in
     * it — in a message, in a field name, in a field value, in the logging context.
     * Searching the raw encoded text rather than selected fields is deliberate:
     * a leak into a field nobody thought to check still fails this.
     */
    @Test
    void noRecordFromAnyFlowContainsAForbiddenValue() throws Exception {
        logs.reset();

        MockHttpSession admin = loggedInSession("test-admin", "test-admin-password");
        logIn("test-user", "test-password").andExpect(status().isOk());
        logIn("not-a-real-account", "wrong-password").andExpect(status().isUnauthorized());
        mvc.perform(withCsrf(post("/api/admin/accounts/{id}/unlock", userId("test-user")))
                        .session(admin))
                .andExpect(status().isOk());

        // The flows really did log, and did name their identities by id, so the
        // assertion below is not vacuously true.
        assertThat(logs.records()).hasSizeGreaterThanOrEqualTo(4);
        assertThat(logs.records()).anySatisfy(record ->
                assertThat(record.at("/user/target/id").asText()).isNotBlank());
        logs.records().forEach(EcsLogFormatTests::assertThatIsValidEcs);
        assertThat(logs.lines()).doesNotContain(FORBIDDEN.toArray(String[]::new));
    }

    /** The fields a collector indexes on. Absent any one of them, the record is not ECS. */
    private static void assertThatIsValidEcs(JsonNode record) {
        assertThat(record.at("/@timestamp").asText()).isNotBlank();
        assertThat(record.at("/ecs/version").asText()).isEqualTo("8.11");
        assertThat(record.at("/log/level").asText()).isNotBlank();
        assertThat(record.at("/log/logger").asText()).isNotBlank();
        assertThat(record.at("/message").asText()).isNotBlank();
    }

    /**
     * The record's classification in the standard's vocabulary: {@code event.kind}
     * {@code event}, a one-element {@code event.category} array, and the
     * {@code event.type} array exactly — both encoded as JSON arrays, as ECS has them.
     */
    private static void assertThatClassifiedAs(
            JsonNode record, String action, String category, String... types) {
        assertThat(record.at("/event/action").asText()).isEqualTo(action);
        assertThat(record.at("/event/kind").asText()).isEqualTo("event");
        assertThat(record.at("/event/category").isArray()).isTrue();
        assertThat(record.at("/event/category").valueStream().map(JsonNode::asText).toList())
                .containsExactly(category);
        assertThat(record.at("/event/type").isArray()).isTrue();
        assertThat(record.at("/event/type").valueStream().map(JsonNode::asText).toList())
                .containsExactly(types);
    }

    private JsonNode onlyRecordWithMessage(String message) {
        List<JsonNode> matching = recordsWithMessage(message);
        assertThat(matching)
                .as("records with message '%s'", message)
                .hasSize(1);
        return matching.getFirst();
    }

    private List<JsonNode> recordsWithMessage(String message) {
        return logs.records().stream()
                .filter(record -> message.equals(record.at("/message").asText()))
                .toList();
    }

    private UUID userId(String userName) {
        return users.findByNormalizedUserName(NormalizedUserName.of(userName))
                .orElseThrow().id();
    }

    /**
     * A session as a real login leaves it — its principal index holding the identity's
     * stable id, written by the login itself — rather than a fixture placed by hand.
     */
    private MockHttpSession loggedInSession(String username, String password) throws Exception {
        MvcResult login = logIn(username, password).andExpect(status().isOk()).andReturn();
        return (MockHttpSession) login.getRequest().getSession(false);
    }

    private static JsonNode json(ResultActions result) throws Exception {
        return JSON.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private ResultActions logIn(
            String username, String password) throws Exception {
        return mvc.perform(withCsrf(post("/api/auth/login"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)));
    }

    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request) {
        CsrfToken token = csrfTokenRepository.generateToken(new MockHttpServletRequest());
        return request
                .cookie(new Cookie("XSRF-TOKEN", token.getToken()))
                .header("X-XSRF-TOKEN", token.getToken());
    }
}
