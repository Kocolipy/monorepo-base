package com.example.backend.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.scim.domain.DormancyPolicy;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.SystemEnvironmentPropertySource;

/**
 * That the two inactivity windows reach the rule from deployment configuration, and that an
 * unusable one stops startup.
 *
 * <p>Driven through a real context refresh, with the conversion service Boot installs, so
 * {@code 60d} is parsed exactly as a deployment's environment variable would be.
 * {@code DormancyPolicyTests} covers the rule itself.
 */
class DormancyPolicyStartupTests {

    private final ApplicationContextRunner contexts = new ApplicationContextRunner()
            .withInitializer(context -> context.getBeanFactory()
                    .setConversionService(ApplicationConversionService.getSharedInstance()))
            .withBean(PropertySourcesPlaceholderConfigurer.class)
            .withUserConfiguration(DormancyPolicyConfig.class);

    @Test
    void unconfiguredWindowsStartWithTheDocumentedDefaults() {
        contexts.run(context -> {
            assertThat(context).hasNotFailed();
            DormancyPolicy policy = context.getBean(DormancyPolicy.class);
            assertThat(policy.deactivationWindow()).isEqualTo(Duration.ofDays(90));
            assertThat(policy.authorityRevocationWindow()).isEqualTo(Duration.ofDays(180));
        });
    }

    @Test
    void configuredWindowsReachThePolicy() {
        contexts.withPropertyValues(
                        "app.dormancy.deactivation.window=60d",
                        "app.dormancy.authority.revocation.window=120d")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    DormancyPolicy policy = context.getBean(DormancyPolicy.class);
                    assertThat(policy.deactivationWindow()).isEqualTo(Duration.ofDays(60));
                    assertThat(policy.authorityRevocationWindow())
                            .isEqualTo(Duration.ofDays(120));
                });
    }

    /** The environment variable names the README and {@code .env.example} document. */
    @Test
    void theDocumentedEnvironmentVariablesReachThePolicy() {
        contexts.withInitializer(context -> context.getEnvironment().getPropertySources()
                        .addFirst(new SystemEnvironmentPropertySource("test-env", Map.<String, Object>of(
                                "APP_DORMANCY_DEACTIVATION_WINDOW", "30d",
                                "APP_DORMANCY_AUTHORITY_REVOCATION_WINDOW", "45d"))))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    DormancyPolicy policy = context.getBean(DormancyPolicy.class);
                    assertThat(policy.deactivationWindow()).isEqualTo(Duration.ofDays(30));
                    assertThat(policy.authorityRevocationWindow())
                            .isEqualTo(Duration.ofDays(45));
                });
    }

    @Test
    void aZeroWindowFailsStartup() {
        contexts.withPropertyValues("app.dormancy.deactivation.window=0d")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .hasRootCauseInstanceOf(IllegalArgumentException.class)
                        .rootCause()
                        .hasMessageContaining("must be positive"));
    }
}
