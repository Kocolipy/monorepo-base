package com.example.backend.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.backend.auth.InMemoryAccountRepository;
import com.example.backend.auth.MutableClock;
import com.example.backend.auth.domain.Account;
import com.example.backend.auth.domain.AccountRole;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

class AccountServiceTests {

    private static final Instant NOW = Instant.parse("2026-09-24T07:00:00Z");

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final MutableClock clock = new MutableClock(NOW);

    private AccountService service;

    @BeforeEach
    void setUp() {
        service = new AccountService(accounts, new PrefixPasswordEncoder(), clock);
    }

    @Test
    void seedsUserAndAdminAccountsWithEncodedPasswords() {
        service.seedDefaults("user", "user-password", "admin", "admin-password");

        assertThat(accounts.findByUsername("user")).contains(
                new Account("user", "encoded:user-password", AccountRole.USER));
        assertThat(accounts.findByUsername("admin")).contains(
                new Account("admin", "encoded:admin-password", AccountRole.ADMIN));
    }

    @Test
    void seedingDoesNotOverwriteAnExistingAccount() {
        Account existing = new Account("user", "existing-hash", AccountRole.ADMIN);
        accounts.save(existing);

        service.seedDefaults("user", "new-password", "admin", "admin-password");

        assertThat(accounts.findByUsername("user")).contains(existing);
        assertThat(accounts.findByUsername("admin")).isPresent();
    }

    @Test
    void loadsThePersistedAccountAsSpringSecurityUserDetails() {
        accounts.save(new Account("admin", "stored-hash", AccountRole.ADMIN));

        var details = service.loadUserByUsername("admin");

        assertThat(details.getUsername()).isEqualTo("admin");
        assertThat(details.getPassword()).isEqualTo("stored-hash");
        assertThat(details.getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactly("ROLE_ADMIN");
        assertThat(details.isAccountNonLocked()).isTrue();
    }

    /**
     * Carrying the lockout into {@code UserDetails} is what rejects a locked
     * account before its password is compared, so the flag has to reflect the
     * stored instant rather than only the attempt count.
     */
    @Test
    void reportsALockedAccountAsLockedToSpringSecurity() {
        accounts.save(new Account(
                "ada", "stored-hash", AccountRole.USER, 3, NOW.plus(Duration.ofMinutes(5))));

        assertThat(service.loadUserByUsername("ada").isAccountNonLocked()).isFalse();
    }

    @Test
    void reportsAnAccountWhoseLockoutHasExpiredAsUsableAgain() {
        accounts.save(new Account(
                "ada", "stored-hash", AccountRole.USER, 3, NOW.plus(Duration.ofMinutes(5))));

        clock.advanceBy(Duration.ofMinutes(5));

        assertThat(service.loadUserByUsername("ada").isAccountNonLocked()).isTrue();
    }

    @Test
    void rejectsAnUnknownUsername() {
        assertThatThrownBy(() -> service.loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Account not found");
    }

    private static final class PrefixPasswordEncoder implements PasswordEncoder {

        @Override
        public String encode(CharSequence rawPassword) {
            return "encoded:" + rawPassword;
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return encode(rawPassword).equals(encodedPassword);
        }
    }
}
