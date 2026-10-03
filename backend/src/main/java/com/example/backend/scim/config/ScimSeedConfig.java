package com.example.backend.scim.config;

import com.example.backend.scim.application.ScimSeedService;
import com.example.backend.scim.application.ScimSeedService.DevFixture;
import com.example.backend.scim.application.ScimSeedService.SeededIdentity;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Runs the directory's seeding at startup, so a fresh deployment has a recovery path before it
 * serves a request.
 *
 * <p>Replaces {@code AccountSeedConfig}, which seeded two rows of a table that no longer exists.
 * The configuration keys are unchanged — {@code app.auth.secondary-username} still names the
 * deployment's recovery identity, as it did when that identity was an account — so an existing
 * deployment's environment does not have to change for the identities to come back under the same
 * names and passwords.
 *
 * <p>Which of the two configured identities is the Bootstrap Admin is decided HERE, by argument
 * position, and it is the same one {@code LoginLockoutConfig} used to name as the lockout-exempt
 * account. That is no longer a second reading of the same setting, though: the exemption is now the
 * reservation marker on the seeded row, so this file is the only place the configured name is
 * turned into a privilege, and nothing downstream compares a username to decide anything.
 *
 * <p>Seeding reads the clock through the {@code Clock} bean rather than {@code Instant.now()}, so
 * what a seeded resource records as its creation time is assertable. That bean is declared once, by
 * {@code LoginLockoutConfig}, and shared — a second one here would leave the container with two
 * candidates and fail startup.
 */
@Configuration
@EnableConfigurationProperties(DevFixtureProperties.class)
public class ScimSeedConfig {

    /**
     * Seeding, then the development fixtures when enabled, then the half of the role mapping's
     * validation that needs the directory — in that order, in one runner, so the check sees
     * exactly the Groups startup created and a failure of any step fails startup.
     */
    @Bean
    ApplicationRunner seedScimDirectory(
            ScimSeedService seeding,
            DevFixtureProperties devFixtures,
            @Value("${app.auth.username}") String userName,
            @Value("${app.auth.password}") String password,
            @Value("${app.auth.secondary-username}") String recoveryUserName,
            @Value("${app.auth.secondary-password}") String recoveryPassword) {
        return arguments -> {
            seeding.seed(
                    new SeededIdentity(userName, password),
                    new SeededIdentity(recoveryUserName, recoveryPassword));
            if (devFixtures.enabled()) {
                seeding.seedDevFixtures(
                        devFixtures.groups() == null ? List.of() : devFixtures.groups().stream()
                                .map(group -> new DevFixture(
                                        group.id(), group.displayName(), group.member()))
                                .toList(),
                        devFixtures.password());
            }
            seeding.verifyMappedGroups();
        };
    }
}
