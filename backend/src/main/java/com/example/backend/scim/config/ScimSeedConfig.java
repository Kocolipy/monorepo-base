package com.example.backend.scim.config;

import com.example.backend.scim.application.ScimSeedService;
import com.example.backend.scim.application.ScimSeedService.DevFixture;
import com.example.backend.scim.application.ScimSeedService.SeededIdentity;
import com.example.backend.scim.domain.DormancyPolicy;
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
 * {@code app.auth.bootstrap-username} / {@code bootstrap-password} ({@code APP_BOOTSTRAP_*}) name
 * the deployment's recovery identity. They were {@code app.auth.secondary-*}
 * ({@code APP_SECONDARY_*}) while a second, ordinary identity was configured beside it; that one is
 * a development fixture now, so a deployment upgrading across the rename must rename the two
 * variables in its environment, or it seeds the {@code application.yaml} fallback instead.
 *
 * <p>The Bootstrap Admin is the one configured identity every deployment gets; the
 * non-administrative {@code user} is a development fixture now, seeded only beside the others. The
 * Bootstrap Admin is the one {@code LoginLockoutConfig} used to name as the lockout-exempt
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
     * Seeding, then the development fixtures when enabled — the dormant fixture last of them —
     * then the half of the role mapping's validation that needs the directory — in that order, in
     * one runner, so the check sees exactly the Groups startup created and a failure of any step
     * fails startup.
     */
    @Bean
    ApplicationRunner seedScimDirectory(
            ScimSeedService seeding,
            DevFixtureProperties devFixtures,
            DormancyPolicy dormancy,
            @Value("${app.auth.bootstrap-username}") String recoveryUserName,
            @Value("${app.auth.bootstrap-password}") String recoveryPassword) {
        return arguments -> {
            seeding.seed(new SeededIdentity(recoveryUserName, recoveryPassword));
            if (devFixtures.enabled()) {
                seeding.seedDevFixtures(
                        devFixtures.groups() == null ? List.of() : devFixtures.groups().stream()
                                .map(group -> new DevFixture(
                                        group.id(), group.displayName(), group.member()))
                                .toList(),
                        devFixtures.baselineMember(),
                        devFixtures.password());
                if (devFixtures.dormantMember() != null) {
                    // A day past the window, so the startup dormancy run locks it.
                    seeding.seedDormantDevFixture(devFixtures.dormantMember(),
                            devFixtures.password(), dormancy.lockoutWindow().plusDays(1));
                }
            }
            seeding.verifyMappedGroups();
        };
    }
}
