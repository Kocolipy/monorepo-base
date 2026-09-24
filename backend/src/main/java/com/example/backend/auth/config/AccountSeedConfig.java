package com.example.backend.auth.config;

import com.example.backend.auth.application.AccountService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AccountSeedConfig {

    @Bean
    ApplicationRunner seedAccounts(
            AccountService accounts,
            @Value("${app.auth.username}") String username,
            @Value("${app.auth.password}") String password,
            @Value("${app.auth.secondary-username}") String adminUsername,
            @Value("${app.auth.secondary-password}") String adminPassword) {
        return arguments -> accounts.seedDefaults(
                username, password, adminUsername, adminPassword);
    }
}
