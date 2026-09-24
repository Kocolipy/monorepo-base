package com.example.backend.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.auth.domain.AccountRepository;
import com.example.backend.auth.domain.AccountRole;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class AccountSeedConfigTests {

    @Autowired
    private AccountRepository accounts;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Test
    void seedsAUserAndAnAdministratorWithEncodedPasswords() {
        var user = accounts.findByUsername("test-user").orElseThrow();
        var admin = accounts.findByUsername("test-admin").orElseThrow();

        assertThat(user.role()).isEqualTo(AccountRole.USER);
        assertThat(admin.role()).isEqualTo(AccountRole.ADMIN);
        assertThat(passwordEncoder.matches("test-password", user.passwordHash())).isTrue();
        assertThat(passwordEncoder.matches("test-admin-password", admin.passwordHash())).isTrue();
    }

    /**
     * The two fields the administrative listing reports besides the credentials.
     * Seeding is the only writer of them, so an account created without them
     * would leave that listing with nothing to show.
     */
    @Test
    void seedsTheEnabledFlagAndCreationTimestamp() {
        var user = accounts.findByUsername("test-user").orElseThrow();
        var admin = accounts.findByUsername("test-admin").orElseThrow();

        assertThat(user.enabled()).isTrue();
        assertThat(admin.enabled()).isTrue();
        assertThat(user.createdAt()).isNotNull().isBeforeOrEqualTo(Instant.now());
        assertThat(admin.createdAt()).isNotNull().isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void seededAccountsAuthenticateWithTheirDatabaseRoles() {
        var user = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("test-user", "test-password"));
        var admin = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        "test-admin", "test-admin-password"));

        assertThat(user.getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .contains("ROLE_USER");
        assertThat(admin.getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .contains("ROLE_ADMIN");
    }
}
