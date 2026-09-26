package com.example.backend.scim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.example.backend.ContainerTestConfiguration;
import com.example.backend.InMemorySessionRegistryConfiguration;
import com.example.backend.observability.RequestIdFilter;
import com.example.backend.scim.application.ConnectorAdministrationService;
import com.example.backend.scim.domain.ConnectorTokenScope;
import jakarta.servlet.Filter;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * The release gate, closed — which is what a deployment gets when nobody turned SCIM on.
 *
 * <p><strong>No property is set here on purpose.</strong> The point of this class is the
 * DEFAULT: if it declared {@code app.scim.enabled=false} it would prove only that the flag
 * works when set, and a deployment that never heard of the flag would be untested. The
 * default lives in {@code ScimSecurityConfig}'s {@code @Value} expression rather than in
 * any YAML, so an absent setting reaches the gate as closed here exactly as it would in
 * production.
 */
@SpringBootTest
@Import({ContainerTestConfiguration.class, InMemorySessionRegistryConfiguration.class})
class ScimReleaseGateIntegrationTests {

    private static final String USERS = "/scim/v2/Users";

    private static final String CREATE_BODY = """
            {"schemas":["urn:ietf:params:scim:schemas:core:2.0:User"],"userName":"gated"}""";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ConnectorAdministrationService connectors;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RequestIdFilter requestIdFilter;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(requestIdFilter, springSecurityFilterChain)
                .build();
    }

    /**
     * Every path in the namespace, discovery included, is simply not there.
     *
     * <p>Discovery is the interesting one: it is PUBLIC when the gate is open, so a closed
     * gate that only covered the authenticated endpoints would leave the service advertising
     * a SCIM interface it does not serve.
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "/scim/v2/ServiceProviderConfig",
        "/scim/v2/ResourceTypes",
        "/scim/v2/ResourceTypes/User",
        "/scim/v2/Schemas",
        "/scim/v2/Users",
        "/scim/v2/Users/8a5c1f4e-0000-4000-8000-000000000001",
        "/scim/v2/Groups",
    })
    void the_whole_namespace_answers_not_found(String path) throws Exception {
        MvcResult result = mvc.perform(get(path)).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(404);
        assertThat(result.getResponse().getContentAsString())
                .as("a closed gate answers as SCIM, so it cannot be mistaken for the SPA")
                .contains("urn:ietf:params:scim:api:messages:2.0:Error");
    }

    /**
     * And never as the single-page application's HTML shell, which is what a reserved-path
     * mistake would produce — with a {@code 200} a provisioning client would read as an
     * empty success.
     */
    @ParameterizedTest
    @ValueSource(strings = {"/scim/v2/Users", "/scim/v2/Userz", "/scim/v2/anything/at/all"})
    void a_gated_path_never_falls_through_to_the_single_page_shell(String path)
            throws Exception {
        MvcResult result = mvc.perform(get(path)).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(404);
        assertThat(result.getResponse().getContentAsString().toLowerCase())
                .doesNotContain("<html")
                .doesNotContain("<!doctype");
    }

    /**
     * The gate is ahead of authentication, which this proves by elimination: a garbage token
     * is what the bearer filter answers {@code 401} to, so a {@code 404} here means the gate
     * decided first. Without that ordering the closed namespace could be probed for whether
     * it exists.
     */
    @Test
    void a_closed_gate_answers_before_the_credential_is_looked_at() throws Exception {
        assertThat(mvc.perform(get(USERS).header(HttpHeaders.AUTHORIZATION, "Bearer garbage"))
                        .andReturn()
                        .getResponse()
                        .getStatus())
                .isEqualTo(404);

        assertThat(mvc.perform(get(USERS)).andReturn().getResponse().getStatus())
                .as("and with no credential at all: 404, not the bearer challenge")
                .isEqualTo(404);
    }

    /** A real write credential does not open it, and nothing is created. */
    @Test
    void a_valid_write_token_creates_nothing_while_the_gate_is_closed() throws Exception {
        UUID connectorId = connectors.create("Okta", "test-admin").id();
        String token = connectors
                .issueToken(connectorId, ConnectorTokenScope.READ_WRITE, null, "test-admin")
                .presentedValue();
        long before = users();

        MvcResult result = mvc.perform(post(USERS)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.valueOf("application/scim+json"))
                        .content(CREATE_BODY))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(404);
        assertThat(users()).isEqualTo(before);
    }

    /**
     * The closed gate's own response declares itself as SCIM JSON in UTF-8.
     *
     * <p>This filter writes its body before any Spring MVC message converter is reached, so
     * the content type and encoding are its own to set. Without them a provisioning client
     * receives an error document it may decline to parse, and a non-ASCII detail would be
     * decoded with the container's default charset rather than UTF-8.
     */
    @Test
    void the_closed_gates_own_response_declares_scim_json_in_utf8() throws Exception {
        MvcResult result = mvc.perform(get(USERS)).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(404);
        assertThat(result.getResponse().getContentType()).startsWith("application/scim+json");
        assertThat(result.getResponse().getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");
    }

    private long users() {
        return jdbc.queryForObject("SELECT count(*) FROM scim_users", Long.class);
    }
}
