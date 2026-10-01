package com.example.backend.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The durable log file a forwarding agent collects: written when {@code LOG_FILE} is set,
 * one ECS JSON object per line, rolled by size and by time, beside stdout rather than
 * instead of it — and not written at all when {@code LOG_FILE} is unset.
 *
 * <p>Each case starts the service's configuration in a child JVM ({@link LogFileProbe})
 * with a real environment variable, exactly as a deployment sets it. In-process would
 * mean reinitializing the JVM-global logging system under every other test's context.
 */
class LogFileTests {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Test
    void withLogFileSetEveryLineOfTheFileIsOneEcsRecordAndStdoutStillCarriesThem(
            @TempDir Path dir) throws Exception {
        Path file = dir.resolve("logs").resolve("backend.json");

        Probe probe = run(dir, Map.of("LOG_FILE", file.toString()));

        assertThat(probe.exit()).as(probe.stdout()).isZero();
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertThat(lines).as("the file has records").hasSizeGreaterThanOrEqualTo(2);
        assertThat(lines).allSatisfy(line -> {
            JsonNode record = JSON.readTree(line);
            assertThat(record.isObject()).isTrue();
            assertThat(record.at("/ecs/version").asText()).isEqualTo("8.11");
            assertThat(record.at("/@timestamp").asText()).endsWith("+08:00");
            assertThat(record.at("/log/level").asText()).isNotBlank();
            assertThat(record.at("/service/name").asText()).isEqualTo("backend");
            assertThat(record.at("/message").asText()).isNotBlank();
        });
        assertThat(lines).anySatisfy(line ->
                assertThat(JSON.readTree(line).at("/message").asText()).isEqualTo(LogFileProbe.MESSAGE));
        assertThat(probe.stdout()).as("stdout keeps working").contains(LogFileProbe.MESSAGE);
    }

    /**
     * Size and time: Boot's size-and-time policy on a daily, indexed, compressed pattern,
     * with the limits {@code logging.yaml} sets — read back from the live appender.
     */
    @Test
    void theFileRollsDailyAndAtFiftyMegabytesKeepingFourteenDaysUnderOneGigabyte(
            @TempDir Path dir) throws Exception {
        Path file = dir.resolve("backend.json");

        Probe probe = run(dir, Map.of("LOG_FILE", file.toString()));

        assertThat(probe.exit()).as(probe.stdout()).isZero();
        assertThat(probe.stdout().lines().filter(line -> line.startsWith(LogFileProbe.POLICY)))
                .singleElement()
                .isEqualTo(LogFileProbe.POLICY
                        + "class=ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy"
                        + " pattern=" + file + ".%d{yyyy-MM-dd}.%i.gz"
                        + " maxHistory=14"
                        + " maxFileSize=" + 50L * 1024 * 1024
                        + " totalSizeCap=" + 1024L * 1024 * 1024);
    }

    /** Unset — local development and the tests — means no file anywhere, and no file appender. */
    @Test
    void withLogFileUnsetNoFileIsWritten(@TempDir Path dir) throws Exception {
        Probe probe = run(dir, Map.of());

        assertThat(probe.exit()).as(probe.stdout()).isZero();
        assertThat(probe.stdout()).contains(LogFileProbe.MESSAGE);
        assertThat(probe.stdout()).doesNotContain(LogFileProbe.POLICY);
        try (var written = Files.list(dir)) {
            assertThat(written).as("files written to the working directory").isEmpty();
        }
    }

    private record Probe(int exit, String stdout) {
    }

    /**
     * Runs {@link LogFileProbe} with this JVM's classpath, in {@code dir}, with
     * {@code LOG_FILE} and {@code LOG_STRUCTURED_FORMAT} removed from the inherited
     * environment so only {@code environment} decides them.
     */
    private static Probe run(Path dir, Map<String, String> environment)
            throws IOException, InterruptedException {
        // Test-only: launches this JVM's own java binary on its own classpath with a constant
        // class name. No argument comes from a request, and the rule's concern (a service
        // spawning processes) does not apply to a test harness.
        // nosemgrep: be-process-execution
        ProcessBuilder builder = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", System.getProperty("java.class.path"),
                LogFileProbe.class.getName())
                .directory(dir.toFile())
                .redirectErrorStream(true);
        builder.environment().remove("LOG_FILE");
        builder.environment().remove("LOG_STRUCTURED_FORMAT");
        builder.environment().putAll(environment);
        Process process = builder.start();
        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(process.waitFor(60, TimeUnit.SECONDS)).as("probe finished").isTrue();
        return new Probe(process.exitValue(), stdout);
    }
}
