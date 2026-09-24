package com.example.backend.auth.config;

import com.example.backend.auth.application.AccountService;
import com.example.backend.auth.application.AccountService.AccountSeed;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Seeding reads the clock through the {@code Clock} bean rather than
 * {@code Instant.now()}, so what a seeded account records as its creation time is
 * assertable. That bean is declared once, by {@link LoginLockoutConfig}, and
 * shared — a second one here would leave the container with two candidates and
 * fail startup.
 */
@Configuration
public class AccountSeedConfig {

    @Bean
    ApplicationRunner seedAccounts(
            AccountService accounts,
            @Value("${app.auth.username}") String username,
            @Value("${app.auth.password}") String password,
            @Value("${app.auth.email}") String email,
            @Value("${app.auth.secondary-username}") String adminUsername,
            @Value("${app.auth.secondary-password}") String adminPassword,
            @Value("${app.auth.secondary-email}") String adminEmail) {
        return arguments -> accounts.seedDefaults(
                new AccountSeed(username, password, email),
                new AccountSeed(adminUsername, adminPassword, adminEmail));
    }
}
