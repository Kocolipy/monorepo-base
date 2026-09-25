package com.example.backend.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.backend.auth.InMemoryAccountRepository;
import com.example.backend.auth.MutableClock;
import com.example.backend.auth.application.AccountService.AccountSeed;
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

    private static final AccountSeed USER_SEED = new AccountSeed("user", "user-password");
    private static final AccountSeed ADMIN_SEED = new AccountSeed("admin", "admin-password");

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final MutableClock clock = new MutableClock(NOW);

    private AccountService service;

    @BeforeEach
    void setUp() {
        service = new AccountService(accounts, new PrefixPasswordEncoder(), clock);
    }

    @Test
    void seedsUserAndAdminAccountsWithEncodedPasswords() {
        service.seedDefaults(USER_SEED, ADMIN_SEED);

        assertThat(accounts.findByUsername("user")).contains(
                seeded("user", "encoded:user-password", AccountRole.USER));
        assertThat(accounts.findByUsername("admin")).contains(
                seeded("admin", "encoded:admin-password", AccountRole.ADMIN));
    }

    @Test
    void seedingDoesNotOverwriteAnExistingAccount() {
        Account existing = seeded("user", "existing-hash", AccountRole.ADMIN);
        accounts.save(existing);

        service.seedDefaults(USER_SEED, ADMIN_SEED);

        assertThat(accounts.findByUsername("user")).contains(existing);
        assertThat(accounts.findByUsername("admin")).isPresent();
    }

    /**
     * The creation timestamp column was added to a table that already held rows,
     * so an account seeded before that change reads back without it. Leaving it
     * that way would make the administrative listing permanently incomplete for
     * the two accounts every deployment has.
     */
    @Test
    void seedingBackfillsAnExistingAccountThatPredatesTheCreatedAtColumn() {
        accounts.save(new Account("user", "existing-hash", AccountRole.ADMIN));

        service.seedDefaults(USER_SEED, ADMIN_SEED);

        assertThat(accounts.require("user")).isEqualTo(
                new Account("user", "existing-hash", AccountRole.ADMIN, 0, null, true, NOW));
    }

    /** A backfill touches the timestamp only; nothing else about the account. */
    @Test
    void backfillingKeepsThePasswordRoleDisabledFlagAndFailureRun() {
        accounts.save(new Account(
                "admin", "operator-hash", AccountRole.USER, 2, null, false, null));

        service.seedDefaults(USER_SEED, ADMIN_SEED);

        Account backfilled = accounts.require("admin");
        assertThat(backfilled.passwordHash()).isEqualTo("operator-hash");
        assertThat(backfilled.role()).isEqualTo(AccountRole.USER);
        assertThat(backfilled.enabled()).isFalse();
        assertThat(backfilled.failedLoginAttempts()).isEqualTo(2);
        assertThat(backfilled.createdAt()).isEqualTo(NOW);
    }

    /**
     * Seeding runs on every startup, so an account that needs nothing must cost
     * no write — the same reason the login path compares identity before saving.
     */
    @Test
    void seedingWritesNothingWhenBothAccountsAreAlreadyComplete() {
        service.seedDefaults(USER_SEED, ADMIN_SEED);
        int afterFirstRun = accounts.saves();

        service.seedDefaults(USER_SEED, ADMIN_SEED);

        assertThat(accounts.saves()).isEqualTo(afterFirstRun);
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
        assertThat(details.isEnabled()).isTrue();
    }

    /**
     * Carrying the lockout into {@code UserDetails} is what rejects a locked
     * account before its password is compared, so the flag has to reflect the
     * stored instant rather than only the attempt count.
     */
    @Test
    void reportsALockedAccountAsLockedToSpringSecurity() {
        accounts.save(new Account(
                "ada", "stored-hash", AccountRole.USER, 3, NOW.plus(Duration.ofMinutes(5)),
                true, NOW));

        assertThat(service.loadUserByUsername("ada").isAccountNonLocked()).isFalse();
    }

    @Test
    void reportsAnAccountWhoseLockoutHasExpiredAsUsableAgain() {
        accounts.save(new Account(
                "ada", "stored-hash", AccountRole.USER, 3, NOW.plus(Duration.ofMinutes(5)),
                true, NOW));

        clock.advanceBy(Duration.ofMinutes(5));

        assertThat(service.loadUserByUsername("ada").isAccountNonLocked()).isTrue();
    }

    /**
     * The listing reports {@code enabled}, so authentication has to act on it —
     * otherwise the field is decoration and a disabled account still logs in.
     * Unlike a lockout, no passage of time lifts this.
     */
    @Test
    void reportsADisabledAccountAsDisabled() {
        accounts.save(new Account(
                "retired", "stored-hash", AccountRole.USER, 0, null, false, NOW));

        var details = service.loadUserByUsername("retired");

        assertThat(details.isEnabled()).isFalse();
        assertThat(details.isAccountNonLocked()).isTrue();
    }

    @Test
    void rejectsAnUnknownUsername() {
        assertThatThrownBy(() -> service.loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Account not found");
    }

    /**
     * A credentialless account — no password hash ever set — must still produce
     * {@code UserDetails} with a non-null password: {@code User.withUsername}
     * throws on {@code null} before {@code DaoAuthenticationProvider} ever
     * reaches the comparison, which would refuse the login differently (and
     * detectably) from a wrong-password attempt on an account that does have a
     * hash. What the marker equals is not the point — only that no submitted
     * password matches it, so the account is refused the same way any other
     * wrong password is.
     */
    @Test
    void reportsACredentiallessAccountWithANonNullUnmatchablePassword() {
        accounts.save(new Account("nopass", null, AccountRole.USER));

        var details = service.loadUserByUsername("nopass");

        assertThat(details.getPassword()).isNotNull();
        assertThat(details.getPassword()).isNotEqualTo("anything the caller could submit");
    }

    /** A complete, enabled account created at {@code NOW} — what seeding writes. */
    private static Account seeded(String username, String passwordHash, AccountRole role) {
        return new Account(username, passwordHash, role, 0, null, true, NOW);
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
