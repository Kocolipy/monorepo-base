package com.example.backend.observability.config;

import com.example.backend.observability.LogContext;
import io.micrometer.tracing.otel.bridge.Slf4JEventListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Puts the current trace and span ids in the logging context under their ECS names,
 * {@code trace.id} and {@code span.id}.
 *
 * <p>Boot registers the same listener with Micrometer's default keys, {@code traceId} and
 * {@code spanId}. The ECS formatter writes a context key as it finds it and nests on the
 * dots, so those would arrive as two top-level fields no ECS query selects on. Named
 * {@link LogContext#TRACE_ID} and {@link LogContext#SPAN_ID}, they nest into
 * {@code trace.id} / {@code span.id} exactly as {@code http.request.id} does. Boot's bean
 * backs off for this one.
 *
 * <p>One consequence for local development: Boot's human-readable pattern prints its
 * correlation block from the default keys, so with {@code LOG_STRUCTURED_FORMAT} set
 * empty the pattern shows no trace id. The JSON output, which is what every deployment
 * emits, carries it.
 */
@Configuration(proxyBeanMethods = false)
public class TraceLogCorrelationConfig {

    @Bean
    Slf4JEventListener ecsSlf4JEventListener() {
        return new Slf4JEventListener(LogContext.TRACE_ID, LogContext.SPAN_ID);
    }
}
