package com.example.backend.scim.controller;

import com.example.backend.observability.MetricTag;
import com.example.backend.scim.domain.ScimVersionPrecondition;
import java.util.List;

/**
 * The {@code If-Match} header of a write against an existing resource, as the use case takes it,
 * with whether one was sent recorded on the request's metrics.
 *
 * <p>The precondition is optional (see {@link ScimVersionPrecondition}), so a connector that never
 * sends it is served — without lost-update protection. That is invisible in the response, so it
 * is made visible in telemetry instead: every such write is tagged
 * {@link MetricTag#SCIM_PRECONDITION} {@code =} {@link #UNCONDITIONAL}, beside the connector tag
 * the bearer filter already recorded, and an operator can see which integrations write without a
 * validator. Recorded before the use case runs, so the tag describes what the CONNECTOR sent; the
 * {@code status} tag says what became of the write.
 */
final class ScimIfMatch {

    /** A write that carried at least one {@code If-Match} value. */
    static final String CONDITIONAL = "if-match";

    /** A write that carried none, applied last-writer-wins. */
    static final String UNCONDITIONAL = "unconditional";

    private ScimIfMatch() {
    }

    /** The precondition these header values carry, its presence recorded on the current request. */
    static ScimVersionPrecondition capture(List<String> headerValues) {
        ScimVersionPrecondition precondition = ScimVersionPrecondition.ofIfMatch(headerValues);
        MetricTag.recordOnCurrentRequest(MetricTag.SCIM_PRECONDITION,
                precondition.isConditional() ? CONDITIONAL : UNCONDITIONAL);
        return precondition;
    }
}
