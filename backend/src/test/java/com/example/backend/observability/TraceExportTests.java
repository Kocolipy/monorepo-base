package com.example.backend.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ClassUtils;

/**
 * Tracing here is for log correlation and nothing leaves the host: no span exporter
 * exists, nothing that could construct one is on the classpath, no exporter endpoint is
 * configured, export is switched off in the deployed configuration, and no propagator is
 * installed, so an inbound trace header is never adopted. The tracer itself is real, so
 * this is not true merely because tracing is absent.
 *
 * <p>"No outbound connection" is argued from these rather than observed: a span reaches
 * the network only through an exporter, and there is none to reach it through.
 */
@SpringBootTest
@Import(com.example.backend.ContainerTestConfiguration.class)
class TraceExportTests {

    /** Every exporter the OpenTelemetry and Micrometer ecosystems ship for spans, logs or metrics. */
    private static final List<String> EXPORTERS = List.of(
            "io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter",
            "io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter",
            "io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter",
            "io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter",
            "io.opentelemetry.exporter.zipkin.ZipkinSpanExporter",
            "io.opentelemetry.exporter.logging.LoggingSpanExporter",
            "io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk",
            "io.micrometer.registry.otlp.OtlpMeterRegistry",
            "zipkin2.reporter.BytesMessageSender");

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private Tracer tracer;

    @Autowired
    private TextMapPropagator propagator;

    @Test
    void theTracerIsReal() {
        assertThat(tracer).isInstanceOf(OtelTracer.class);
    }

    @Test
    void noSpanExporterExists() {
        assertThat(context.getBeansOfType(SpanExporter.class)).isEmpty();
    }

    @Test
    void noExporterIsOnTheClasspath() {
        // The check would be vacuous if class lookup were broken, so it first finds a
        // class that is there.
        assertThat(ClassUtils.isPresent("io.opentelemetry.sdk.trace.SdkTracerProvider", null)).isTrue();
        assertThat(EXPORTERS).allSatisfy(exporter ->
                assertThat(ClassUtils.isPresent(exporter, null)).as(exporter).isFalse());
    }

    /** No configuration file the service loads names an exporter endpoint or enables export. */
    @Test
    void noExporterIsConfigured() {
        List<String> configured = environment.getPropertySources().stream()
                .filter(source -> source.getName().startsWith("Config resource"))
                .filter(EnumerablePropertySource.class::isInstance)
                .flatMap(source -> Arrays.stream(((EnumerablePropertySource<?>) source).getPropertyNames()))
                .toList();
        assertThat(configured).as("properties loaded from config files")
                .contains("management.tracing.export.enabled");
        assertThat(configured).noneMatch(name -> name.matches("(?i).*(otlp|zipkin|opentelemetry).*"));
        assertThat(environment.getProperty("management.tracing.export.enabled", Boolean.class)).isFalse();
    }

    /** The deployed document itself, not the test context's view of it. */
    @Test
    void theDeployedConfigurationTurnsExportOff() throws Exception {
        MutablePropertySources sources = new MutablePropertySources();
        for (PropertySource<?> source
                : new YamlPropertySourceLoader().load("telemetry", new ClassPathResource("telemetry.yaml"))) {
            sources.addLast(source);
        }

        assertThat(new PropertySourcesPropertyResolver(sources)
                .getProperty("management.tracing.export.enabled", Boolean.class))
                .isFalse();
    }

    @Test
    void noPropagatorIsInstalled() {
        assertThat(propagator).isSameAs(TextMapPropagator.noop());
    }
}
