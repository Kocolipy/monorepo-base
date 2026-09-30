package com.example.backend.observability;

import io.micrometer.common.KeyValue;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.ServerHttpObservationFilter;

/**
 * The metric tag keys this service adds to Spring's {@code http.server.requests}, and the
 * one way a request handler contributes a value to one.
 *
 * <p>The metric counterpart of {@link LogEvent}: shared vocabulary, so the adapter that
 * learns a value (the bearer filter learns the connector, the SCIM error renderer learns
 * the {@code scimType}) and the convention that publishes it name the key identically.
 *
 * <p><strong>A tag is not a place for an identifier.</strong> Every value published under
 * these keys comes from a closed set — a resource type, an RFC 7644 {@code scimType}, or a
 * connector's non-secret id — and the convention that publishes them replaces anything
 * else with {@link #OTHER}. So a caller passing a {@code userName}, a filter or a token here
 * by mistake produces a useless series, never a leaked one.
 */
public final class MetricTag {

    /** SCIM resource type the request addressed: {@code User}, {@code Group}, and so on. */
    public static final String SCIM_RESOURCE_TYPE = "scim.resource.type";

    /** RFC 7644 §3.12 {@code scimType} of a SCIM error response. */
    public static final String SCIM_TYPE = "scim.type";

    /** Non-secret id of the connector whose token authenticated the request. */
    public static final String SCIM_CONNECTOR = "scim.connector";

    /** The value when the tag does not apply to this request. */
    public static final String NONE = "none";

    /** The value a tag carries when what it was given is outside its closed set. */
    public static final String OTHER = "other";

    private MetricTag() {
    }

    /**
     * Records a tag value on the request's in-flight {@code http.server.requests}
     * observation. A request no observation is recording (a unit test's mock, or a
     * dispatch outside the observation filter) is left alone.
     */
    public static void record(HttpServletRequest request, String key, String value) {
        ServerHttpObservationFilter.findObservationContext(request)
                .ifPresent(context -> context.addLowCardinalityKeyValue(KeyValue.of(key, value)));
    }

    /**
     * {@link #record(HttpServletRequest, String, String)} for the request this thread is
     * serving, for a caller — an exception handler — that is not handed the request.
     */
    public static void recordOnCurrentRequest(String key, String value) {
        if (RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes) {
            record(attributes.getRequest(), key, value);
        }
    }
}
