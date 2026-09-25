package com.example.backend.audit.config;

import com.example.backend.audit.domain.AuditRetentionPolicy;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Where the audit retention window enters the application.
 *
 * <p>Neither the default nor the floor is expressed here. Both live on
 * {@link AuditRetentionPolicy}, so a deployment that configures nothing and a test
 * that constructs the policy directly agree about what one year means and about
 * what is too short. A placeholder default written into this file would be a
 * second copy of a number the domain already owns, and the two would drift.
 *
 * <p>Separate from {@link AuditRetentionScheduleConfig} because that class needs
 * the policy injected: a configuration class cannot take as a constructor argument
 * a bean it declares itself.
 */
@Configuration
public class AuditRetentionPolicyConfig {

    /**
     * Binds the configured window, leaving both the default and the floor to the
     * policy.
     *
     * <p>{@code #{null}} rather than a literal default: an absent setting has to
     * reach the policy as absent, or "the default is one year" would be a fact
     * about this file instead of about the rule. A value below the floor makes this
     * bean's creation throw, and a bean that cannot be created is a context that
     * cannot come up — which is how a too-short window fails startup rather than
     * silently shortening the trail.
     */
    @Bean
    public AuditRetentionPolicy auditRetentionPolicy(
            @Value("${app.audit.retention.period:#{null}}") Duration period,
            @Value("${app.audit.retention.schedule:#{null}}") String schedule) {
        return new AuditRetentionPolicy(period, schedule);
    }
}
