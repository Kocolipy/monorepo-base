package com.example.backend.observability.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.tracing.otel.bridge.EventPublishingContextWrapper.ScopeAttachedEvent;
import io.micrometer.tracing.otel.bridge.EventPublishingContextWrapper.ScopeClosedEvent;
import io.micrometer.tracing.otel.bridge.Slf4JEventListener;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/**
 * The listener this configuration registers, driven with the scope events the tracer
 * publishes: it writes the span's ids under the ECS keys, and never under Micrometer's
 * defaults, which the ECS formatter would emit as top-level {@code traceId} / {@code spanId}.
 * {@code EcsLogFormatTests} holds the same end to end.
 */
class TraceLogCorrelationConfigTests {

    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";

    private static final String SPAN = "00f067aa0ba902b7";

    private final Slf4JEventListener listener = new TraceLogCorrelationConfig().ecsSlf4JEventListener();

    @AfterEach
    void clearTheContext() {
        MDC.clear();
    }

    @Test
    void aSpanInScopePutsItsIdsUnderTheEcsKeys() {
        listener.onEvent(new ScopeAttachedEvent(Context.root().with(Span.wrap(
                SpanContext.create(TRACE, SPAN, TraceFlags.getDefault(), TraceState.getDefault())))));

        assertThat(MDC.get("trace.id")).isEqualTo(TRACE);
        assertThat(MDC.get("span.id")).isEqualTo(SPAN);
        assertThat(MDC.get("traceId")).isNull();
        assertThat(MDC.get("spanId")).isNull();
    }

    @Test
    void closingTheScopeRemovesThem() {
        listener.onEvent(new ScopeAttachedEvent(Context.root().with(Span.wrap(
                SpanContext.create(TRACE, SPAN, TraceFlags.getDefault(), TraceState.getDefault())))));

        listener.onEvent(new ScopeClosedEvent());

        assertThat(MDC.get("trace.id")).isNull();
        assertThat(MDC.get("span.id")).isNull();
    }
}
