package com.example.backend.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import java.util.List;
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
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;

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
        assertThat(record.at("/event/action").asText()).isEqualTo("login");
        assertThat(record.at("/event/outcome").asText()).isEqualTo("success");
        assertThat(record.at("/http/request/id").asText()).isNotBlank();
        assertThat(record.at("/log/level").asText()).isEqualTo("INFO");
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
        assertThat(record.at("/event/action").asText()).isEqualTo("login");
        assertThat(record.at("/event/outcome").asText()).isEqualTo("failure");
        assertThat(record.at("/event/reason").asText()).isEqualTo("BadCredentialsException");
        assertThat(record.at("/http/request/id").asText()).isNotBlank();
        assertThat(record.at("/log/level").asText()).isEqualTo("WARN");
    }

    @Test
    void anAdministrativeChangeIsOneEcsRecordCarryingTheRequestsCorrelationId()
            throws Exception {
        mvc.perform(withCsrf(post("/api/admin/accounts/test-user/unlock"))
                        .session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isOk());

        JsonNode record = onlyRecordWithMessage("Administrative account change applied");

        assertThatIsValidEcs(record);
        assertThat(record.at("/event/action").asText()).isEqualTo("account.unlock");
        assertThat(record.at("/event/outcome").asText()).isEqualTo("success");
        assertThat(record.at("/http/request/id").asText()).isNotBlank();
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

        logIn("test-user", "test-password").andExpect(status().isOk());
        logIn("not-a-real-account", "wrong-password").andExpect(status().isUnauthorized());
        mvc.perform(withCsrf(post("/api/admin/accounts/test-user/unlock"))
                        .session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isOk());

        // The flows really did log, so the assertion below is not vacuously true.
        assertThat(logs.records()).hasSizeGreaterThanOrEqualTo(3);
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

    private JsonNode onlyRecordWithMessage(String message) {
        List<JsonNode> matching = logs.records().stream()
                .filter(record -> message.equals(record.at("/message").asText()))
                .toList();
        assertThat(matching)
                .as("records with message '%s'", message)
                .hasSize(1);
        return matching.getFirst();
    }

    private org.springframework.test.web.servlet.ResultActions logIn(
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

    private MockHttpSession authenticatedSession(String authority) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(
                new TestingAuthenticationToken("an-administrator", null, authority));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                securityContext);
        return session;
    }
}
