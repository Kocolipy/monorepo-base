package com.example.backend.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.backend.auth.domain.Account;
import com.example.backend.auth.domain.AccountRepository;
import com.example.backend.auth.domain.AccountRole;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

class AccountServiceTests {

    private final RecordingAccountRepository accounts = new RecordingAccountRepository();
    private AccountService service;

    @BeforeEach
    void setUp() {
        service = new AccountService(accounts, new PrefixPasswordEncoder());
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
    }

    @Test
    void rejectsAnUnknownUsername() {
        assertThatThrownBy(() -> service.loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Account not found");
    }

    private static final class RecordingAccountRepository implements AccountRepository {

        private final Map<String, Account> stored = new HashMap<>();

        @Override
        public Optional<Account> findByUsername(String username) {
            return Optional.ofNullable(stored.get(username));
        }

        @Override
        public Account save(Account account) {
            Account nonNullAccount = Objects.requireNonNull(account);
            stored.put(nonNullAccount.username(), nonNullAccount);
            return nonNullAccount;
        }
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
