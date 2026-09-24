package com.example.backend.auth.config;

import com.example.backend.auth.domain.LockoutPolicy;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Where the lockout rule's numbers and its notion of "now" enter the
 * application. Both are beans so the rule itself stays free of configuration and
 * of the system clock, which is what lets a test lock an account and then watch
 * the lockout expire without waiting.
 */
@Configuration
public class LoginLockoutConfig {

    @Bean
    public LockoutPolicy lockoutPolicy(
            @Value("${app.auth.lockout.max-attempts}") int maxAttempts,
            @Value("${app.auth.lockout.duration}") Duration lockDuration) {
        return new LockoutPolicy(maxAttempts, lockDuration);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
