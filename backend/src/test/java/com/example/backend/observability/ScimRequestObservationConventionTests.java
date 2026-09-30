package com.example.backend.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * The tag policy, unit by unit: which values each added tag may carry and what everything
 * else becomes. {@code OperationalTelemetryIntegrationTests} proves the same tags reach a
 * real scrape; this class proves the closed sets hold for inputs no sample traffic sends.
 */
class ScimRequestObservationConventionTests {

    private static final String CONNECTOR = "3f2b8c1e-9a4d-4c7b-8e21-5d6f7a8b9c0d";

    private final ScimRequestObservationConvention convention =
            new ScimRequestObservationConvention();

    @ParameterizedTest
    @CsvSource({
        "/scim/v2/Users, User",
        "/scim/v2/Users/2819c223-7f76-453a-919d-413861904646, User",
        "/scim/v2/Users/.search, User",
        "/scim/v2/Groups, Group",
        "/scim/v2/Groups/abc, Group",
        "/scim/v2/.search, any",
        "/scim/v2/ServiceProviderConfig, discovery",
        "/scim/v2/ResourceTypes/User, discovery",
        "/scim/v2/Schemas, discovery",
        "/scim/v2/Schemas/urn:ietf:params:scim:schemas:core:2.0:User, discovery",
        "/scim/v2, other",
        "/scim/v2/Bulk, other",
        "/scim/v2/UsersAndMore, other",
        "/scim/v2/Groupsx/1, other",
        "/scim/v2/.searchx, other",
        "/scim/v2/Me, other",
        "/scim/v2/telemetry-probe-userName, other",
        "/scim/v2x/Users, none",
        "/scim, none",
        "/api/auth/login, none",
        "/actuator/prometheus, none",
        "/, none",
    })
    void the_resource_type_comes_from_a_fixed_table_never_from_the_path(
            String path, String expected) {
        assertThat(ScimRequestObservationConvention.resourceType(path)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "invalidFilter", "tooMany", "uniqueness", "mutability", "invalidSyntax",
        "invalidPath", "noTarget", "invalidValue", "invalidVers", "sensitive"})
    void every_rfc_7644_scim_type_is_published_as_itself(String scimType) {
        assertThat(ScimRequestObservationConvention.scimType(scimType)).isEqualTo(scimType);
    }

    @ParameterizedTest
    @ValueSource(strings = {"userName eq \"alice\"", "Uniqueness", "", "other"})
    void a_scim_type_outside_rfc_7644_becomes_other(String recorded) {
        assertThat(ScimRequestObservationConvention.scimType(recorded)).isEqualTo("other");
    }

    @Test
    void a_connector_id_is_published_as_itself() {
        assertThat(ScimRequestObservationConvention.connector(CONNECTOR)).isEqualTo(CONNECTOR);
    }

    /**
     * {@code 1-1-1-1-1} is one {@link java.util.UUID#fromString} accepts, and a token or a
     * userName is not one at all: neither may pass for a connector id.
     */
    @ParameterizedTest
    @ValueSource(strings = {"1-1-1-1-1", "scim_Q2xvdWRUb2tlblZhbHVl", "alice", "",
        "3F2B8C1E-9A4D-4C7B-8E21-5D6F7A8B9C0D"})
    void anything_but_a_canonical_uuid_does_not_pass_for_a_connector(String recorded) {
        assertThat(ScimRequestObservationConvention.connector(recorded)).isEqualTo("other");
    }

    @Test
    void nothing_recorded_is_none_including_the_none_this_convention_set_at_start() {
        assertThat(ScimRequestObservationConvention.scimType(null)).isEqualTo("none");
        assertThat(ScimRequestObservationConvention.scimType("none")).isEqualTo("none");
        assertThat(ScimRequestObservationConvention.connector(null)).isEqualTo("none");
        assertThat(ScimRequestObservationConvention.connector("none")).isEqualTo("none");
    }

    /**
     * Every request carries every key — a Prometheus meter has one set of tag keys — and
     * Spring's own tags survive beside them.
     */
    @Test
    void a_request_nothing_was_recorded_on_carries_every_key_as_none() {
        Map<String, String> tags = tags(context("/api/auth/login"));

        assertThat(tags).containsEntry("scim.resource.type", "none")
                .containsEntry("scim.type", "none")
                .containsEntry("scim.connector", "none")
                .containsKeys("method", "status", "uri", "outcome", "exception");
    }

    @Test
    void recorded_values_are_published_through_the_closed_sets() {
        ServerRequestObservationContext context = context("/scim/v2/Users");
        context.addLowCardinalityKeyValue(KeyValue.of("scim.type", "uniqueness"));
        context.addLowCardinalityKeyValue(KeyValue.of("scim.connector", CONNECTOR));

        assertThat(tags(context)).containsEntry("scim.resource.type", "User")
                .containsEntry("scim.type", "uniqueness")
                .containsEntry("scim.connector", CONNECTOR);
    }

    @Test
    void a_recorded_value_outside_its_set_is_published_as_other() {
        ServerRequestObservationContext context = context("/scim/v2/Users");
        context.addLowCardinalityKeyValue(KeyValue.of("scim.type", "userName eq \"alice\""));
        context.addLowCardinalityKeyValue(KeyValue.of("scim.connector", "alice"));

        assertThat(tags(context)).containsEntry("scim.type", "other")
                .containsEntry("scim.connector", "other");
    }

    /** The path is read without the servlet context path, as the security chains read it. */
    @Test
    void the_resource_type_ignores_the_context_path() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/app/scim/v2/Groups");
        request.setContextPath("/app");

        assertThat(tags(new ServerRequestObservationContext(
                request, new MockHttpServletResponse()))).containsEntry("scim.resource.type", "Group");
    }

    private static ServerRequestObservationContext context(String path) {
        return new ServerRequestObservationContext(
                new MockHttpServletRequest("GET", path), new MockHttpServletResponse());
    }

    private Map<String, String> tags(ServerRequestObservationContext context) {
        KeyValues values = convention.getLowCardinalityKeyValues(context);
        Map<String, String> tags = new LinkedHashMap<>();
        values.forEach(value -> tags.put(value.getKey(), value.getValue()));
        return tags;
    }
}
