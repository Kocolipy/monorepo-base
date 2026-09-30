package com.example.backend.auth.config;

import com.example.backend.scim.domain.DormancyPolicy;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Where the two inactivity windows enter the application.
 *
 * <p>Neither default is written here or in {@code application.yaml}. Both live on
 * {@link DormancyPolicy}, as the audit retention defaults live on their policy, so an unset
 * setting reaches the rule as unset and "the default is 90 days" is a fact about the rule
 * rather than about a config file a deployment may replace.
 *
 * <p>Separate from {@link DormancyScheduleConfig} because that class needs the policy injected,
 * and a configuration class cannot take as a constructor argument a bean it declares itself.
 */
@Configuration
public class DormancyPolicyConfig {

    /**
     * Binds the configured windows — {@code APP_DORMANCY_DEACTIVATION_WINDOW} and
     * {@code APP_DORMANCY_AUTHORITY_REVOCATION_WINDOW} through relaxed binding — leaving both
     * defaults to the policy. A zero or negative window makes this bean's creation throw, which
     * fails startup rather than deactivating every User on the next run.
     */
    @Bean
    public DormancyPolicy dormancyPolicy(
            @Value("${app.dormancy.deactivation.window:#{null}}") Duration deactivationWindow,
            @Value("${app.dormancy.authority.revocation.window:#{null}}")
                    Duration authorityRevocationWindow) {
        return new DormancyPolicy(deactivationWindow, authorityRevocationWindow);
    }
}
