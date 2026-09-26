package com.example.backend.scim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.example.backend.ContainerTestConfiguration;
import com.example.backend.InMemorySessionRegistryConfiguration;
import com.example.backend.observability.RequestIdFilter;
import com.example.backend.scim.application.ConnectorAdministrationService;
import com.example.backend.scim.controller.ScimDiscovery;
import com.example.backend.scim.domain.ConnectorTokenScope;
import com.example.backend.scim.domain.ScimPageRequest;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Discovery, and the claim that it describes this service rather than SCIM in general.
 *
 * <p>Each advertised flag is asserted BESIDE the behaviour it advertises — the filter flag
 * beside a filtered query, the sort flag beside a sorted one, the ETag flag beside a
 * response's validator. An assertion on the document alone would pass just as well if the
 * document were a fiction, which is the failure mode that matters: a connector configures
 * itself from this and has no other way to find out.
 */
@SpringBootTest
@Import({ContainerTestConfiguration.class, InMemorySessionRegistryConfiguration.class})
@TestPropertySource(properties = "app.scim.enabled=true")
class ScimDiscoveryIntegrationTests {

    private static final String BASE = "/scim/v2";

    private static final String USER_SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:User";

    private static final String ENTERPRISE_SCHEMA =
            "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User";

    private static final MediaType SCIM_JSON = MediaType.valueOf("application/scim+json");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ConnectorAdministrationService connectors;

    @Autowired
    private RequestIdFilter requestIdFilter;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    private final JsonMapper json = JsonMapper.builder().build();

    private MockMvc mvc;

    private String writeToken;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(requestIdFilter, springSecurityFilterChain)
                .build();
        UUID connectorId = connectors.create("Okta", "test-admin").id();
        writeToken = connectors
                .issueToken(connectorId, ConnectorTokenScope.READ_WRITE, null, "test-admin")
                .presentedValue();
    }

    /** Discovery is readable with no credential: a connector reads it before it has one. */
    @ParameterizedTest
    @ValueSource(strings = {
        BASE + "/ServiceProviderConfig",
        BASE + "/ResourceTypes",
        BASE + "/Schemas",
    })
    void discovery_is_public_and_answers_as_scim_json(String path) throws Exception {
        MvcResult result = mvc.perform(get(path)).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentType()).startsWith("application/scim+json");
    }

    @Test
    void the_capability_document_advertises_bulk_as_unsupported_with_zero_limits()
            throws Exception {
        JsonNode config = body(get(BASE + "/ServiceProviderConfig"));

        assertThat(config.get("bulk").get("supported").booleanValue()).isFalse();
        assertThat(config.get("bulk").get("maxOperations").asInt()).isZero();
        assertThat(config.get("bulk").get("maxPayloadSize").asInt()).isZero();
    }

    @Test
    void the_capability_document_declares_exactly_one_bearer_scheme() throws Exception {
        JsonNode schemes = body(get(BASE + "/ServiceProviderConfig"))
                .get("authenticationSchemes");

        assertThat(schemes.size()).isEqualTo(1);
        assertThat(schemes.get(0).get("type").asText()).isEqualTo("oauthbearertoken");
        assertThat(schemes.get(0).get("primary").booleanValue()).isTrue();
    }

    /**
     * {@code filter.supported=false}, and a filtered query is refused. The pair is the
     * assertion: either both change or the document is lying.
     */
    @Test
    void the_filter_flag_agrees_with_what_a_filtered_query_does() throws Exception {
        assertThat(body(get(BASE + "/ServiceProviderConfig"))
                        .get("filter").get("supported").booleanValue())
                .isEqualTo(ScimDiscovery.FILTER_SUPPORTED)
                .isFalse();

        MvcResult filtered = mvc.perform(asConnector(get(BASE + "/Users")
                        .param("filter", "userName eq \"bjensen\"")))
                .andReturn();

        assertThat(filtered.getResponse().getStatus()).isEqualTo(403);
        assertThat(json.readTree(filtered.getResponse().getContentAsString()).get("schemas")
                        .get(0).asText())
                .isEqualTo("urn:ietf:params:scim:api:messages:2.0:Error");
    }

    /** The page ceiling advertised is the one the paging rule enforces. */
    @Test
    void the_advertised_maximum_results_is_the_enforced_page_ceiling() throws Exception {
        assertThat(body(get(BASE + "/ServiceProviderConfig"))
                        .get("filter").get("maxResults").asInt())
                .isEqualTo(ScimPageRequest.MAX_COUNT);
    }

    @Test
    void the_sort_flag_agrees_with_what_a_sorted_query_does() throws Exception {
        assertThat(body(get(BASE + "/ServiceProviderConfig"))
                        .get("sort").get("supported").booleanValue())
                .isEqualTo(ScimDiscovery.SORT_SUPPORTED)
                .isFalse();

        assertThat(mvc.perform(asConnector(get(BASE + "/Users").param("sortBy", "userName")))
                        .andReturn().getResponse().getStatus())
                .isEqualTo(403);
    }

    /**
     * {@code patch.supported=false}, and a PATCH does not succeed. The refusal's SHAPE is
     * the dispatcher's rather than this slice's — no handler is mapped for the method, so
     * the SCIM error advice never runs — which is why this asserts the outcome is not a
     * success rather than asserting a body.
     */
    @Test
    void the_patch_flag_agrees_with_the_absence_of_a_patch_endpoint() throws Exception {
        assertThat(body(get(BASE + "/ServiceProviderConfig"))
                        .get("patch").get("supported").booleanValue())
                .isEqualTo(ScimDiscovery.PATCH_SUPPORTED)
                .isFalse();

        int status = mvc.perform(asConnector(
                        patch(BASE + "/Users/8a5c1f4e-0000-4000-8000-000000000001"))
                        .contentType(SCIM_JSON)
                        .content("{}"))
                .andReturn()
                .getResponse()
                .getStatus();

        assertThat(status).isGreaterThanOrEqualTo(400);
    }

    /** {@code etag.supported=true}, and a created resource carries one that matches its version. */
    @Test
    void the_etag_flag_agrees_with_the_validator_a_response_carries() throws Exception {
        assertThat(body(get(BASE + "/ServiceProviderConfig"))
                        .get("etag").get("supported").booleanValue())
                .isEqualTo(ScimDiscovery.ETAG_SUPPORTED)
                .isTrue();

        MvcResult created = mvc.perform(asConnector(post(BASE + "/Users"))
                        .contentType(SCIM_JSON)
                        .content("""
                                {"schemas":["urn:ietf:params:scim:schemas:core:2.0:User"],
                                 "userName":"etag-probe"}"""))
                .andReturn();

        assertThat(created.getResponse().getStatus()).isEqualTo(201);
        assertThat(created.getResponse().getHeader(HttpHeaders.ETAG))
                .isEqualTo(json.readTree(created.getResponse().getContentAsString())
                        .get("meta").get("version").asText());
    }

    /**
     * {@code changePassword.supported=false}: a password may be SET at create time, which is
     * not the capability that flag names — that one needs a PUT or PATCH, and there is none.
     */
    @Test
    void change_password_is_advertised_as_unsupported_while_there_is_no_write_to_do_it_with()
            throws Exception {
        assertThat(body(get(BASE + "/ServiceProviderConfig"))
                        .get("changePassword").get("supported").booleanValue())
                .isEqualTo(ScimDiscovery.CHANGE_PASSWORD_SUPPORTED)
                .isFalse();
    }

    @Test
    void the_resource_types_are_user_alone_while_groups_have_no_endpoints() throws Exception {
        JsonNode types = body(get(BASE + "/ResourceTypes"));

        assertThat(types.get("totalResults").asInt()).isEqualTo(1);
        assertThat(types.get("Resources").get(0).get("id").asText()).isEqualTo("User");
        assertThat(types.get("Resources").get(0).get("endpoint").asText()).isEqualTo("/Users");
        assertThat(types.get("Resources").get(0).get("schema").asText()).isEqualTo(USER_SCHEMA);

        JsonNode one = body(get(BASE + "/ResourceTypes/User"));
        assertThat(one.get("id").asText())
                .as("the single-resource endpoint serves the resource type, not an empty body")
                .isEqualTo("User");
        assertThat(one.get("endpoint").asText()).isEqualTo("/Users");
        assertThat(one.get("schema").asText()).isEqualTo(USER_SCHEMA);

        assertThat(mvc.perform(get(BASE + "/ResourceTypes/Group")).andReturn()
                        .getResponse().getStatus())
                .as("a resource type in the list is a claim that its endpoint answers")
                .isEqualTo(404);
    }

    @Test
    void the_schemas_are_the_core_user_schema_alone() throws Exception {
        JsonNode schemas = body(get(BASE + "/Schemas"));

        assertThat(schemas.get("totalResults").asInt()).isEqualTo(1);
        assertThat(schemas.get("Resources").get(0).get("id").asText()).isEqualTo(USER_SCHEMA);

        JsonNode one = body(get(BASE + "/Schemas/" + USER_SCHEMA));
        assertThat(one.get("id").asText())
                .as("the single-schema endpoint serves the schema, not an empty body")
                .isEqualTo(USER_SCHEMA);
        assertThat(one.get("attributes").isArray()).isTrue();

        assertThat(mvc.perform(get(BASE + "/Schemas/" + ENTERPRISE_SCHEMA)).andReturn()
                        .getResponse().getStatus())
                .as("no extension is implemented, so none is served")
                .isEqualTo(404);
    }

    /**
     * A refusal that has no {@code scimType} omits the key rather than carrying a null.
     *
     * <p>RFC 7644 §3.12 makes {@code scimType} optional and defines its values as a closed
     * set, so {@code "scimType": null} is not a member of it. A client that switches on the
     * value would see a type it cannot map.
     */
    @Test
    void a_refusal_with_no_scim_type_omits_the_key() throws Exception {
        MvcResult result = mvc.perform(get(BASE + "/Schemas/" + ENTERPRISE_SCHEMA)).andReturn();

        JsonNode error = json.readTree(result.getResponse().getContentAsString());
        assertThat(error.has("scimType")).isFalse();
        assertThat(error.get("status").asText()).isEqualTo("404");
        assertThat(error.get("detail").asText()).isNotBlank();
    }

    /**
     * The schema document lists exactly the implemented attributes, checked against the
     * write surface: an attribute the schema declares is accepted on a create, and one it
     * does not is refused. The unit test pins the two sets to one list; this pins the list
     * to the running service.
     */
    @Test
    void an_attribute_the_schema_does_not_declare_is_refused_on_a_create() throws Exception {
        MvcResult refused = mvc.perform(asConnector(post(BASE + "/Users"))
                        .contentType(SCIM_JSON)
                        .content("""
                                {"schemas":["urn:ietf:params:scim:schemas:core:2.0:User"],
                                 "userName":"undeclared-attribute","nickName":"Babs"}"""))
                .andReturn();

        assertThat(refused.getResponse().getStatus()).isEqualTo(400);
        JsonNode error = json.readTree(refused.getResponse().getContentAsString());
        assertThat(error.get("scimType").asText()).isEqualTo("invalidValue");
        assertThat(error.get("status").asText()).isEqualTo("400");
    }

    /**
     * A filter on a discovery path is refused rather than ignored, on every one of them.
     *
     * <p>RFC 7644 has a provider ignore filtering here, and ignoring it is the one answer a
     * client cannot tell from a match.
     */
    @ParameterizedTest
    @ValueSource(strings = {
        BASE + "/ServiceProviderConfig",
        BASE + "/ResourceTypes",
        BASE + "/ResourceTypes/User",
        BASE + "/Schemas",
        BASE + "/Schemas/" + USER_SCHEMA,
    })
    void a_filter_on_a_discovery_path_is_refused(String path) throws Exception {
        MvcResult result = mvc.perform(get(path).param("filter", "id eq \"User\"")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        assertThat(json.readTree(result.getResponse().getContentAsString()).get("status").asText())
                .isEqualTo("403");
    }

    private MockHttpServletRequestBuilder asConnector(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + writeToken);
    }

    private JsonNode body(MockHttpServletRequestBuilder request) throws Exception {
        MvcResult result = mvc.perform(request).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        return json.readTree(result.getResponse().getContentAsString());
    }
}
