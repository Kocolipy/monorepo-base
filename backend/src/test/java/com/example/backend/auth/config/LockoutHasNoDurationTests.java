package com.example.backend.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.auth.domain.Account;
import com.example.backend.auth.domain.LockoutPolicy;
import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

/**
 * A lockout has no duration, and this is where that is held rather than argued.
 *
 * <p>Two halves, because either alone would pass while the criterion was broken.
 * The context half proves the application starts with
 * {@code app.auth.lockout.duration} absent — a setting {@code LoginLockoutConfig}
 * still required would fail startup here rather than at a deployment's next
 * restart. The file half proves the key is not merely unread but gone from the
 * files an operator configures the service from, including the ones no bean ever
 * reads (the example environment, the operator reference).
 *
 * <p>Each file assertion checks a control string is present before asserting the
 * forbidden one is absent, so a renamed or moved file fails this test instead of
 * passing it by having nothing to find.
 */
@SpringBootTest
@Import(com.example.backend.ContainerTestConfiguration.class)
class LockoutHasNoDurationTests {

    /** Deployed configuration, the example environment, and the operator reference. */
    private static Stream<Path> operatorFacingFiles() {
        return Stream.of(
                Path.of("src/main/resources/application.yaml"),
                Path.of(".env.example"),
                Path.of("README.md"));
    }

    @Autowired
    private Environment environment;

    @Autowired
    private LockoutPolicy policy;

    @Test
    void theApplicationStartsWithNoLockoutDurationConfigured() {
        assertThat(environment.getProperty("app.auth.lockout.duration")).isNull();
        assertThat(environment.getProperty("app.auth.lockout.max-attempts")).isNotNull();
        assertThat(policy.maxAttempts()).isPositive();
    }

    @ParameterizedTest
    @MethodSource("operatorFacingFiles")
    void noOperatorFacingFileMentionsALockoutDuration(Path file) throws IOException {
        String contents = Files.readString(file);

        assertThat(contents).contains("LOCKOUT_MAX_ATTEMPTS");
        assertThat(contents)
                .doesNotContain("APP_LOCKOUT_DURATION")
                .doesNotContain("lockout.duration");
    }

    /**
     * The deployed configuration is read directly rather than through the
     * environment, because the test resources' own {@code application.yaml}
     * shadows it entirely: a duration left in the deployed file would be invisible
     * to every context-based assertion in this suite.
     */
    @Test
    void theDeployedConfigurationHasNoDurationUnderTheLockoutBlock() throws IOException {
        String contents = Files.readString(Path.of("src/main/resources/application.yaml"));

        assertThat(contents).contains("lockout:");
        assertThat(contents.lines().map(String::strip))
                .contains("max-attempts: ${APP_LOCKOUT_MAX_ATTEMPTS:5}")
                .noneSatisfy(line -> assertThat(line).startsWith("duration:"));
    }

    /** No domain type can express a window either, whatever configuration says. */
    @Test
    void noDomainFieldExpressesALockoutDuration() {
        assertThat(componentsOf(LockoutPolicy.class))
                .extracting(RecordComponent::getName)
                .containsExactly("maxAttempts");
        assertThat(componentsOf(LockoutPolicy.class))
                .noneSatisfy(component ->
                        assertThat(component.getType()).isEqualTo(Duration.class));
        assertThat(componentsOf(Account.class))
                .extracting(RecordComponent::getName)
                .contains("lockedAt")
                .doesNotContain("lockedUntil");
        assertThat(componentsOf(Account.class))
                .noneSatisfy(component ->
                        assertThat(component.getType()).isEqualTo(Duration.class));
    }

    private static java.util.List<RecordComponent> componentsOf(Class<?> type) {
        return Arrays.asList(type.getRecordComponents());
    }
}
