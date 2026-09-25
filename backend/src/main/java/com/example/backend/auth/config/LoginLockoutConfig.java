package com.example.backend.auth.config;

import com.example.backend.auth.domain.BootstrapAdmin;
import com.example.backend.auth.domain.LockoutPolicy;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Where the lockout rule's one number, the account it must never apply to, and
 * the application's notion of "now" enter the application. All three are beans so
 * the rule itself stays free of configuration and of the system clock.
 *
 * <p>There is no lockout duration to configure. A lock is lifted by an
 * administrator's Unlock and by nothing else, so a window would express a lift
 * that no code performs — {@code app.auth.lockout.duration} is gone from
 * configuration rather than defaulted here.
 */
@Configuration
public class LoginLockoutConfig {

    @Bean
    public LockoutPolicy lockoutPolicy(
            @Value("${app.auth.lockout.max-attempts}") int maxAttempts) {
        return new LockoutPolicy(maxAttempts);
    }

    /**
     * The recovery identity, named by the same configured username startup seeding
     * creates the administrator account under, so the account that exists to
     * recover the deployment and the account exempt from lockout cannot be two
     * different ones.
     */
    @Bean
    public BootstrapAdmin bootstrapAdmin(
            @Value("${app.auth.secondary-username}") String username) {
        return new BootstrapAdmin(username);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
