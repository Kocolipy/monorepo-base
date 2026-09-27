package com.example.backend.auth.config;

import com.example.backend.scim.domain.LockoutPolicy;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Where the lockout rule's one number and the application's notion of "now" enter the
 * application. Both are beans so the rule itself stays free of configuration and of the
 * system clock.
 *
 * <p>There is no lockout duration to configure. A lock is lifted by an administrator's
 * Unlock and by nothing else, so a window would express a lift that no code performs —
 * {@code app.auth.lockout.duration} is gone from configuration rather than defaulted here.
 *
 * <p><strong>There is no longer a {@code BootstrapAdmin} bean either</strong>, and that is
 * the substantive change. The recovery identity used to be named here, by reading
 * {@code app.auth.secondary-username} a second time, so "which account must never lock" was
 * a string comparison against a configured value — which a rename could silently move, and
 * which a second identity could acquire by taking the name. It is now the reservation marker
 * on the seeded SCIM User's own resource row, written once by seeding and reachable by no
 * UPDATE. {@code ScimSeedConfig} is the one place the configured name still becomes a
 * privilege, and it does so by creating the marked row rather than by leaving a name for
 * something downstream to compare.
 */
@Configuration
public class LoginLockoutConfig {

    @Bean
    public LockoutPolicy lockoutPolicy(
            @Value("${app.auth.lockout.max-attempts}") int maxAttempts) {
        return new LockoutPolicy(maxAttempts);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
