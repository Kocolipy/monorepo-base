package com.example.backend.observability;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.server.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.stereotype.Component;

/**
 * The tags on {@code http.server.requests}: Spring's own, plus the SCIM dimensions the
 * operational-telemetry contract asks for.
 *
 * <p>Spring's defaults already cover most of the contract and are kept as they are:
 * {@code uri} is the matched route TEMPLATE ({@code /scim/v2/Users/{id}}, never the id),
 * {@code method}, the exact {@code status}, and {@code outcome}, which is the status class
 * ({@code SUCCESS}, {@code CLIENT_ERROR}, {@code SERVER_ERROR}, ...). The query string is
 * never part of any of them, so filter text cannot reach a tag. Added here:
 *
 * <ul>
 *   <li>{@link MetricTag#SCIM_RESOURCE_TYPE} — from the path, so it is present on a
 *       request the security chain refused before any handler ran. That is the {@code 401}
 *       an authentication-failure alert counts, and Spring tags it {@code uri=UNKNOWN}
 *       because no route matched it yet; without this tag it could not be told apart
 *       from a {@code 401} anywhere else in the service.
 *   <li>{@link MetricTag#SCIM_TYPE} — the RFC 7644 error type of a SCIM {@code 4xx}.
 *   <li>{@link MetricTag#SCIM_CONNECTOR} — the connector whose token authenticated the
 *       request: traffic per connector, by its non-secret id.
 *   <li>{@link MetricTag#SCIM_PRECONDITION} — whether a PUT, PATCH or DELETE on an existing
 *       resource carried {@code If-Match}, so the writes made without lost-update protection
 *       can be counted per connector.
 * </ul>
 *
 * <p>Every key is emitted on every request, with {@link MetricTag#NONE} where it does not
 * apply. That is a Prometheus requirement rather than tidiness: a meter name has one set
 * of tag keys, and a series arriving with a different set is dropped.
 *
 * <p><strong>Closed sets, enforced here.</strong> A value a handler recorded is published
 * only if it belongs to its tag's set; anything else becomes {@link MetricTag#OTHER}. The
 * resource type is derived from a fixed table rather than from the path segment, so an
 * arbitrary path cannot mint a tag value either. This is what makes "no identifier, filter
 * text or secret in any tag" a property of this class rather than of every caller.
 */
@Component
public class ScimRequestObservationConvention extends DefaultServerRequestObservationConvention {

    private static final String SCIM_PREFIX = "/scim/v2";

    /** RFC 7644 §3.12, Table 9: the only {@code scimType} values a tag may carry. */
    static final Set<String> SCIM_TYPES = Set.of(
            "invalidFilter", "tooMany", "uniqueness", "mutability", "invalidSyntax",
            "invalidPath", "noTarget", "invalidValue", "invalidVers", "sensitive");

    /** Whether a write against an existing resource sent {@code If-Match}. */
    static final Set<String> PRECONDITIONS = Set.of("if-match", "unconditional");

    @Override
    public KeyValues getLowCardinalityKeyValues(ServerRequestObservationContext context) {
        return super.getLowCardinalityKeyValues(context).and(
                KeyValue.of(MetricTag.SCIM_RESOURCE_TYPE, resourceType(path(context))),
                KeyValue.of(MetricTag.SCIM_TYPE, scimType(recorded(context, MetricTag.SCIM_TYPE))),
                KeyValue.of(MetricTag.SCIM_CONNECTOR,
                        connector(recorded(context, MetricTag.SCIM_CONNECTOR))),
                KeyValue.of(MetricTag.SCIM_PRECONDITION,
                        precondition(recorded(context, MetricTag.SCIM_PRECONDITION))));
    }

    /**
     * The SCIM resource type a path addresses, from a fixed table.
     *
     * <p>{@code any} is the root {@code /.search}, which spans types; {@code discovery} is
     * the three public metadata documents; {@link MetricTag#OTHER} is anything else inside
     * the namespace, and {@link MetricTag#NONE} is a request outside it.
     */
    static String resourceType(String path) {
        if (!(path.equals(SCIM_PREFIX) || path.startsWith(SCIM_PREFIX + "/"))) {
            return MetricTag.NONE;
        }
        String rest = path.substring(SCIM_PREFIX.length());
        if (isUnder(rest, "/Users")) {
            return "User";
        }
        if (isUnder(rest, "/Groups")) {
            return "Group";
        }
        if (rest.equals("/.search")) {
            return "any";
        }
        if (isUnder(rest, "/ServiceProviderConfig") || isUnder(rest, "/ResourceTypes")
                || isUnder(rest, "/Schemas")) {
            return "discovery";
        }
        return MetricTag.OTHER;
    }

    static String scimType(String recorded) {
        if (absent(recorded)) {
            return MetricTag.NONE;
        }
        return SCIM_TYPES.contains(recorded) ? recorded : MetricTag.OTHER;
    }

    static String precondition(String recorded) {
        if (absent(recorded)) {
            return MetricTag.NONE;
        }
        return PRECONDITIONS.contains(recorded) ? recorded : MetricTag.OTHER;
    }

    /** A connector id is a UUID; nothing else a caller recorded may pass for one. */
    static String connector(String recorded) {
        if (absent(recorded)) {
            return MetricTag.NONE;
        }
        try {
            return UUID.fromString(recorded).toString().equals(recorded)
                    ? recorded
                    : MetricTag.OTHER;
        } catch (IllegalArgumentException notAnId) {
            return MetricTag.OTHER;
        }
    }

    /**
     * Nothing was recorded. {@link MetricTag#NONE} counts as nothing: the observation asks
     * this convention for its tags when it starts as well as when it stops, so by the stop
     * the context already holds the {@code none} this class put there at the start.
     */
    private static boolean absent(String recorded) {
        return recorded == null || recorded.equals(MetricTag.NONE);
    }

    /** Exactly {@code segment}, or {@code segment} followed by a sub-path. */
    private static boolean isUnder(String rest, String segment) {
        return rest.equals(segment) || rest.startsWith(segment + "/");
    }

    private static String path(ServerRequestObservationContext context) {
        return context.getCarrier().getRequestURI()
                .substring(context.getCarrier().getContextPath().length());
    }

    private static String recorded(ServerRequestObservationContext context, String key) {
        KeyValue value = context.getLowCardinalityKeyValue(key);
        return value == null ? null : value.getValue();
    }
}
