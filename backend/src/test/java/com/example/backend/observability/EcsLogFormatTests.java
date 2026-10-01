package com.example.backend.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.SessionCsrf;
import com.example.backend.audit.domain.AuditRetentionPolicy;
import com.example.backend.scim.application.ConnectorAdministrationService;
import com.example.backend.scim.domain.ConnectorTokenScope;
import com.example.backend.scim.domain.NormalizedUserName;
import com.example.backend.scim.domain.ScimUserRepository;
import jakarta.servlet.Filter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.scheduling.config.Task;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.ServerHttpObservationFilter;
import org.w3c.dom.Document;
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

    /** A W3C trace id: 32 lowercase hex digits, and not the all-zero invalid id. */
    private static final String TRACE_ID = "(?!0{32})[0-9a-f]{32}";

    /** A W3C span id: 16 lowercase hex digits, and not the all-zero invalid id. */
    private static final String SPAN_ID = "(?!0{16})[0-9a-f]{16}";

    /** Local Singapore time to the millisecond, with its offset. */
    private static final String PLUS_EIGHT_TIMESTAMP =
            "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\+08:00";

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
    private ScimUserRepository users;

    @Autowired
    private FilterRegistrationBean<ServerHttpObservationFilter> observationFilter;

    @Autowired
    private ScheduledTaskHolder scheduledTasks;

    @Autowired
    private AuditRetentionPolicy retentionPolicy;

    @Autowired
    private ConnectorAdministrationService connectors;

    private MockMvc mvc;

    private EcsLogCapture logs;

    @BeforeEach
    void setUp() {
        // The request-id filter ahead of the security chain, as the deployed
        // ordering has it: a request refused by the chain must still be logged
        // under an id. The observation filter between them, as deployed too
        // (HIGHEST_PRECEDENCE + 1): it opens the request's span, so every record
        // inside the chain carries its trace and span ids.
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(requestIdFilter, observationFilter.getFilter(), springSecurityFilterChain)
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

    /**
     * The rest of the record envelope, as committed — the service fields, the timestamp
     * customizer, and a log file that exists only when {@code LOG_FILE} says so. The
     * version placeholder must have been filtered by the build; an unfiltered
     * {@code @project.version@} would be logged literally.
     */
    @Test
    void theDeployedConfigurationStatesTheEnvelope() throws Exception {
        MutablePropertySources sources = new MutablePropertySources();
        for (PropertySource<?> source
                : new YamlPropertySourceLoader().load("logging", new ClassPathResource("logging.yaml"))) {
            sources.addLast(source);
        }
        PropertySourcesPropertyResolver deployed = new PropertySourcesPropertyResolver(sources);

        assertThat(deployed.getProperty("logging.structured.format.file")).isEqualTo("ecs");
        assertThat(deployed.getProperty("logging.structured.ecs.service.name")).isEqualTo("backend");
        assertThat(deployed.getProperty("logging.structured.ecs.service.version"))
                .isEqualTo(builtProjectVersion());
        assertThat(deployed.getProperty("logging.structured.json.customizer"))
                .isEqualTo(EcsTimestampCustomizer.class.getName());
        // Resolved against this document alone, so neither LOG_FILE nor APP_ENVIRONMENT
        // on the machine running the test is seen: these are the committed defaults.
        assertThat(deployed.getProperty("logging.structured.ecs.service.environment"))
                .isEqualTo("local");
        assertThat(deployed.getProperty("logging.file.name")).isEmpty();
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
     * Every record emitted inside a request carries that request's trace and span ids,
     * as ECS {@code trace.id} and {@code span.id}: one trace per request, and a different
     * one for the next request.
     */
    @Test
    void everyRecordInsideARequestCarriesItsTraceAndSpanIds() throws Exception {
        logIn("test-user", "test-password").andExpect(status().isOk());
        logIn("not-a-real-account", "wrong-password").andExpect(status().isUnauthorized());

        List<JsonNode> inRequest = logs.records().stream()
                .filter(record -> !record.at("/http/request/id").asText().isEmpty())
                .toList();
        assertThat(inRequest).as("records emitted inside a request").hasSizeGreaterThanOrEqualTo(2);
        assertThat(inRequest).allSatisfy(record -> {
            assertThat(record.at("/trace/id").asText()).matches(TRACE_ID);
            assertThat(record.at("/span/id").asText()).matches(SPAN_ID);
        });

        String accepted = onlyRecordWithMessage("Login accepted").at("/trace/id").asText();
        String refused = onlyRecordWithMessage("Login refused").at("/trace/id").asText();
        assertThat(accepted).isNotEqualTo(refused);
        Map<String, Set<String>> tracesByRequest = inRequest.stream().collect(Collectors.groupingBy(
                record -> record.at("/http/request/id").asText(),
                Collectors.mapping(record -> record.at("/trace/id").asText(), Collectors.toSet())));
        assertThat(tracesByRequest.values()).allSatisfy(traces -> assertThat(traces).hasSize(1));
    }

    /**
     * The trace id is minted here. A caller's {@code traceparent} is not adopted, for the
     * reason {@link RequestIdFilter} ignores {@code X-Request-Id}: a client able to choose
     * the id could merge unrelated requests in a log search.
     */
    @Test
    void aCallersTraceparentIsNotAdopted() throws Exception {
        String callersTrace = "4bf92f3577b34da6a3ce929d0e0e4736";

        mvc.perform(withCsrf(post("/api/auth/login"))
                        .header("traceparent", "00-" + callersTrace + "-00f067aa0ba902b7-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test-user\",\"password\":\"test-password\"}"))
                .andExpect(status().isOk());

        assertThat(onlyRecordWithMessage("Login accepted").at("/trace/id").asText())
                .matches(TRACE_ID)
                .isNotEqualTo(callersTrace);
    }

    /**
     * A scheduled job runs off any request, so it gets a trace of its own from
     * {@link ScheduledJobMetrics#instrument}: every record of one run shares a trace id,
     * and the next run's differs. Run through the task the scheduler actually holds, not
     * a re-wrapped copy of the job.
     */
    @Test
    void eachScheduledJobRunIsItsOwnTrace() {
        List<Runnable> retentionTasks = scheduledTasks.getScheduledTasks().stream()
                .map(ScheduledTask::getTask)
                .filter(CronTask.class::isInstance)
                .map(CronTask.class::cast)
                .filter(task -> task.getExpression().equals(retentionPolicy.schedule()))
                .map(Task::getRunnable)
                .toList();
        assertThat(retentionTasks).as("the scheduled retention task").hasSize(1);
        Runnable retention = retentionTasks.getFirst();
        String thread = Thread.currentThread().getName();

        List<String> runTraces = new ArrayList<>();
        for (int run = 0; run < 2; run++) {
            logs.reset();
            retention.run();

            List<JsonNode> runRecords = logs.records().stream()
                    .filter(record -> thread.equals(record.at("/process/thread/name").asText()))
                    .toList();
            String trace = onlyRecordWithMessage("Audit retention run complete").at("/trace/id").asText();
            assertThat(trace).matches(TRACE_ID);
            assertThat(runRecords).allSatisfy(record -> {
                assertThat(record.at("/trace/id").asText()).isEqualTo(trace);
                assertThat(record.at("/span/id").asText()).matches(SPAN_ID);
                assertThat(record.has("http")).as("off-request").isFalse();
            });
            runTraces.add(trace);
        }
        assertThat(runTraces.get(0)).isNotEqualTo(runTraces.get(1));
    }

    /**
     * The service fields on every record, whatever emitted it: the name stated in
     * {@code logging.yaml}, the version of the build — read here from the POM itself, so
     * the oracle is not the filtered resource it is checking — and the environment
     * ({@code APP_ENVIRONMENT}, {@code local} when unset).
     */
    @Test
    void everyRecordCarriesTheServiceNameVersionAndEnvironment() throws Exception {
        logIn("test-user", "test-password").andExpect(status().isOk());
        logIn("not-a-real-account", "wrong-password").andExpect(status().isUnauthorized());

        String version = builtProjectVersion();
        assertThat(version).isNotBlank().doesNotContain("@");
        assertThat(logs.records()).hasSizeGreaterThanOrEqualTo(2).allSatisfy(record -> {
            assertThat(record.at("/service/name").asText()).isEqualTo("backend");
            assertThat(record.at("/service/version").asText()).isEqualTo(version);
            assertThat(record.at("/service/environment").asText())
                    .isEqualTo(System.getenv().getOrDefault("APP_ENVIRONMENT", "local"));
        });
    }

    /**
     * Log timestamps are Singapore time; the SCIM wire is not. A record's
     * {@code @timestamp} ends in {@code +08:00} while a {@code meta.lastModified} read in
     * the same test still ends in {@code Z} — the zone was changed for the log, not for
     * the JVM.
     */
    @Test
    void recordTimestampsArePlusEightWhileScimWireTimesStayUtc() throws Exception {
        UUID connectorId = connectors.create("ecs-timestamp-connector", "test-admin").id();
        String token = connectors.issueToken(
                connectorId, ConnectorTokenScope.READ_WRITE, null, "test-admin").presentedValue();
        logIn("test-user", "test-password").andExpect(status().isOk());

        JsonNode user = json(mvc.perform(get("/scim/v2/Users/{id}", userId("test-user"))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()));

        assertThat(user.at("/meta/lastModified").asText()).isNotBlank().endsWith("Z");
        assertThat(logs.records()).hasSizeGreaterThanOrEqualTo(2).allSatisfy(record ->
                assertThat(record.at("/@timestamp").asText()).matches(PLUS_EIGHT_TIMESTAMP));
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
     * {@code /project/version} of the POM this module is built from. The test runs from
     * the module's directory, as Maven runs it.
     */
    private static String builtProjectVersion() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        Document pom = factory.newDocumentBuilder().parse(Path.of("pom.xml").toFile());
        return XPathFactory.newInstance().newXPath().evaluate("/project/version", pom).strip();
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
        return SessionCsrf.withCsrf(mvc, request);
    }
}
