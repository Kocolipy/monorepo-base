package com.example.backend.observability;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.ServerHttpObservationFilter;

/** Recording a tag value onto the request's in-flight observation, and when there is none. */
class MetricTagTests {

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void a_value_is_recorded_on_the_requests_observation() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        ServerRequestObservationContext context = observed(request);

        MetricTag.record(request, MetricTag.SCIM_TYPE, "uniqueness");

        assertThat(context.getLowCardinalityKeyValue(MetricTag.SCIM_TYPE).getValue())
                .isEqualTo("uniqueness");
    }

    @Test
    void a_request_no_observation_is_recording_is_left_alone() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        MetricTag.record(request, MetricTag.SCIM_TYPE, "uniqueness");

        assertThat(request.getAttributeNames().hasMoreElements()).isFalse();
    }

    @Test
    void the_current_request_is_the_one_this_thread_is_serving() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        ServerRequestObservationContext context = observed(request);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        MetricTag.recordOnCurrentRequest(MetricTag.SCIM_TYPE, "invalidFilter");

        assertThat(context.getLowCardinalityKeyValue(MetricTag.SCIM_TYPE).getValue())
                .isEqualTo("invalidFilter");
    }

    @Test
    void with_no_current_request_recording_is_a_no_op() {
        MetricTag.recordOnCurrentRequest(MetricTag.SCIM_TYPE, "invalidFilter");

        assertThat(RequestContextHolder.getRequestAttributes()).isNull();
    }

    private static ServerRequestObservationContext observed(MockHttpServletRequest request) {
        ServerRequestObservationContext context =
                new ServerRequestObservationContext(request, new MockHttpServletResponse());
        request.setAttribute(
                ServerHttpObservationFilter.CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE, context);
        return context;
    }
}
