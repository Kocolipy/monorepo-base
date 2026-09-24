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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

class AccountServiceTests {

    private static final Instant NOW = Instant.parse("2026-09-24T07:00:00Z");

    private static final AccountSeed USER_SEED =
            new AccountSeed("user", "user-password", "user@example.com");
    private static final AccountSeed ADMIN_SEED =
            new AccountSeed("admin", "admin-password", "admin@example.com");

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
                seeded("user", "encoded:user-password", AccountRole.USER, "user@example.com"));
        assertThat(accounts.findByUsername("admin")).contains(
                seeded("admin", "encoded:admin-password", AccountRole.ADMIN, "admin@example.com"));
    }

    @Test
    void seedingDoesNotOverwriteAnExistingAccount() {
        Account existing =
                seeded("user", "existing-hash", AccountRole.ADMIN, "existing@example.com");
        accounts.save(existing);

        service.seedDefaults(USER_SEED, ADMIN_SEED);

        assertThat(accounts.findByUsername("user")).contains(existing);
        assertThat(accounts.findByUsername("admin")).isPresent();
    }

    /**
     * The profile columns were added to a table that already held rows, so an
     * account seeded before that change reads back with neither field. Leaving it
     * that way would make the administrative listing permanently incomplete for
     * the two accounts every deployment has.
     */
    @Test
    void seedingBackfillsAnExistingAccountThatPredatesTheProfileColumns() {
        accounts.save(new Account("user", "existing-hash", AccountRole.ADMIN));

        service.seedDefaults(USER_SEED, ADMIN_SEED);

        assertThat(accounts.require("user")).isEqualTo(new Account(
                "user", "existing-hash", AccountRole.ADMIN, 0, null,
                "user@example.com", true, NOW));
    }

    /** A backfill touches the profile only; nothing else about the account. */
    @Test
    void backfillingKeepsThePasswordRoleDisabledFlagAndFailureRun() {
        accounts.save(new Account(
                "admin", "operator-hash", AccountRole.USER, 2, null, null, false, null));

        service.seedDefaults(USER_SEED, ADMIN_SEED);

        Account backfilled = accounts.require("admin");
        assertThat(backfilled.passwordHash()).isEqualTo("operator-hash");
        assertThat(backfilled.role()).isEqualTo(AccountRole.USER);
        assertThat(backfilled.enabled()).isFalse();
        assertThat(backfilled.failedLoginAttempts()).isEqualTo(2);
        assertThat(backfilled.email()).isEqualTo("admin@example.com");
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
    void listsEveryAccountWithoutItsPasswordHash() {
        service.seedDefaults(USER_SEED, ADMIN_SEED);

        List<AccountSummary> listed = service.listAccounts();

        assertThat(listed).containsExactly(
                new AccountSummary("admin", "admin@example.com", AccountRole.ADMIN, true, NOW),
                new AccountSummary("user", "user@example.com", AccountRole.USER, true, NOW));
    }

    @Test
    void listsAccountsOrderedByUsername() {
        accounts.save(account("zoe", AccountRole.USER));
        accounts.save(account("ada", AccountRole.ADMIN));
        accounts.save(account("bob", AccountRole.USER));

        assertThat(service.listAccounts())
                .extracting(AccountSummary::username)
                .containsExactly("ada", "bob", "zoe");
    }

    /**
     * A lockout is not part of the listing. It is transient state the login path
     * owns, and an administrator reviewing access is asking a different question.
     */
    @Test
    void listsALockedAccountWithoutMentioningTheLockout() {
        accounts.save(new Account(
                "ada", "hash", AccountRole.USER, 3, NOW.plus(Duration.ofMinutes(5)),
                "ada@example.com", true, NOW));

        assertThat(service.listAccounts()).containsExactly(
                new AccountSummary("ada", "ada@example.com", AccountRole.USER, true, NOW));
    }

    @Test
    void listsNoAccountsWhenNoneAreStored() {
        assertThat(service.listAccounts()).isEmpty();
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
                "ada@example.com", true, NOW));

        assertThat(service.loadUserByUsername("ada").isAccountNonLocked()).isFalse();
    }

    @Test
    void reportsAnAccountWhoseLockoutHasExpiredAsUsableAgain() {
        accounts.save(new Account(
                "ada", "stored-hash", AccountRole.USER, 3, NOW.plus(Duration.ofMinutes(5)),
                "ada@example.com", true, NOW));

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
                "retired", "stored-hash", AccountRole.USER, 0, null,
                "retired@example.com", false, NOW));

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

    private static Account account(String username, AccountRole role) {
        return seeded(username, "hash", role, username + "@example.com");
    }

    /** A complete, enabled account created at {@code NOW} — what seeding writes. */
    private static Account seeded(
            String username, String passwordHash, AccountRole role, String email) {
        return new Account(username, passwordHash, role, 0, null, email, true, NOW);
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
