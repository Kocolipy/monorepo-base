package com.example.backend.scim.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.observability.MetricTag;
import com.example.backend.scim.domain.ScimVersionPrecondition;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.ServerHttpObservationFilter;

/**
 * Capturing {@code If-Match}: the precondition handed to the use case, and whether one was sent,
 * recorded on the request's in-flight observation.
 *
 * <p>{@code OperationalTelemetryIntegrationTests} proves the same tag reaches a real scrape, but
 * its traffic runs once in {@code @BeforeAll}, which mutation testing does not credit reliably;
 * this class is what makes the capture load-bearing under PIT.
 */
class ScimIfMatchTests {

    private ServerRequestObservationContext observation;

    @BeforeEach
    void serveAnObservedRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/scim/v2/Users/x");
        observation = new ServerRequestObservationContext(request, new MockHttpServletResponse());
        request.setAttribute(
                ServerHttpObservationFilter.CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE, observation);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void a_write_without_if_match_is_recorded_as_unconditional() {
        ScimVersionPrecondition captured = ScimIfMatch.capture(null);

        assertThat(captured.isConditional()).isFalse();
        assertThat(recorded()).isEqualTo("unconditional");
    }

    @Test
    void a_write_with_if_match_is_recorded_as_conditional_and_carries_the_header() {
        ScimVersionPrecondition captured = ScimIfMatch.capture(List.of("\"7\""));

        assertThat(captured).isEqualTo(ScimVersionPrecondition.ofIfMatch(List.of("\"7\"")));
        assertThat(recorded()).isEqualTo("if-match");
    }

    /** Presence, not validity: a header that will then be refused still counts as sent. */
    @Test
    void a_header_the_use_case_will_refuse_is_still_recorded_as_if_match() {
        ScimIfMatch.capture(List.of("*"));

        assertThat(recorded()).isEqualTo("if-match");
    }

    private String recorded() {
        return observation.getLowCardinalityKeyValue(MetricTag.SCIM_PRECONDITION).getValue();
    }
}
