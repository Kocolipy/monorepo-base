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
    private final PrefixPasswordEncoder passwordEncoder = new PrefixPasswordEncoder();

    private AccountService service;

    @BeforeEach
    void setUp() {
        service = new AccountService(accounts, passwordEncoder, clock);
    }

    @Test
    void seedsUserAndAdminAccountsWithEncodedPasswords() {
        service.seedDefaults(USER_SEED, ADMIN_SEED);

        Account user = accounts.require("user");
        Account admin = accounts.require("admin");
        assertThat(user).isEqualTo(
                seeded(user.id(), "user", "encoded:user-password", AccountRole.USER));
        assertThat(admin).isEqualTo(
                seeded(admin.id(), "admin", "encoded:admin-password", AccountRole.ADMIN));
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
        Account created = accounts.save(new Account("user", "existing-hash", AccountRole.ADMIN));

        service.seedDefaults(USER_SEED, ADMIN_SEED);

        assertThat(accounts.require("user")).isEqualTo(
                new Account(
                        created.id(), "user", "existing-hash", AccountRole.ADMIN,
                        0, null, true, NOW));
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
     * stored lock state rather than only the attempt count.
     */
    @Test
    void reportsALockedAccountAsLockedToSpringSecurity() {
        accounts.save(new Account(
                "ada", "stored-hash", AccountRole.USER, 3, NOW, true, NOW));

        assertThat(service.loadUserByUsername("ada").isAccountNonLocked()).isFalse();
    }

    /**
     * And keeps reporting it locked however long it has stood: nothing but an
     * administrator's Unlock changes the answer, so the clock is not consulted.
     */
    @Test
    void keepsReportingALockedAccountAsLockedHoweverMuchTimeHasPassed() {
        accounts.save(new Account(
                "ada", "stored-hash", AccountRole.USER, 3, NOW, true, NOW));

        clock.advanceBy(Duration.ofDays(3650));

        assertThat(service.loadUserByUsername("ada").isAccountNonLocked()).isFalse();
    }

    /**
     * The listing reports {@code enabled}, so authentication has to act on it —
     * otherwise the field is decoration and a disabled account still logs in.
     * Like a lockout, no passage of time lifts this.
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

    /**
     * The unmatchable marker is encoded once per process and reused. Argon2id is
     * deliberately expensive, so recomputing it on every credentialless login
     * attempt would hand an unauthenticated caller a way to spend this service's
     * CPU at will — the cache is a cost control, not a tidiness measure.
     *
     * <p>Asserted by call count rather than by comparing markers: the test encoder
     * is deterministic, so a marker recomputed on every call is byte-identical to a
     * cached one and no equality assertion can tell them apart.
     */
    @Test
    void theUnmatchableMarkerIsEncodedOnceAndReusedAcrossCalls() {
        accounts.save(new Account("nopass", null, AccountRole.USER));

        String first = service.loadUserByUsername("nopass").getPassword();
        String second = service.loadUserByUsername("nopass").getPassword();
        String third = service.loadUserByUsername("nopass").getPassword();

        assertThat(passwordEncoder.encodeCountOf("no-password-set")).isEqualTo(1);
        assertThat(second).isEqualTo(first);
        assertThat(third).isEqualTo(first);
    }

    /**
     * Two credentialless accounts share the one marker, so the cache is keyed to the
     * process rather than recomputed per account.
     */
    @Test
    void twoCredentiallessAccountsShareTheOneEncodedMarker() {
        accounts.save(new Account("nopass-one", null, AccountRole.USER));
        accounts.save(new Account("nopass-two", null, AccountRole.USER));

        String one = service.loadUserByUsername("nopass-one").getPassword();
        String two = service.loadUserByUsername("nopass-two").getPassword();

        assertThat(passwordEncoder.encodeCountOf("no-password-set")).isEqualTo(1);
        assertThat(two).isEqualTo(one);
    }

    /**
     * The stable id behind a username, which the session index is keyed by. Spring
     * Security carries the username, so without this the session index would be
     * keyed by a mutable value.
     */
    @Test
    void resolvesTheStableIdBehindAUsername() {
        Account stored = accounts.save(seeded("user", "hash", AccountRole.USER));

        assertThat(service.resolveAccountId("user")).isEqualTo(stored.id());
    }

    /**
     * An unknown username is refused rather than resolved to null. A null id would
     * travel into the session index as a key, silently indexing sessions under
     * nothing instead of failing where the mistake was made.
     */
    @Test
    void refusesToResolveAnIdForAnUnknownUsername() {
        assertThatThrownBy(() -> service.resolveAccountId("missing"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Account not found");
    }

    /** A complete, enabled account created at {@code NOW} — what seeding writes. */
    private static Account seeded(String username, String passwordHash, AccountRole role) {
        return new Account(username, passwordHash, role, 0, null, true, NOW);
    }

    /** The seeded account with a caller-supplied id, for comparing against a stored row. */
    private static Account seeded(
            java.util.UUID id, String username, String passwordHash, AccountRole role) {
        return new Account(id, username, passwordHash, role, 0, null, true, NOW);
    }

    private static final class PrefixPasswordEncoder implements PasswordEncoder {

        private final java.util.Map<String, Integer> encodeCounts = new java.util.HashMap<>();

        @Override
        public String encode(CharSequence rawPassword) {
            encodeCounts.merge(rawPassword.toString(), 1, Integer::sum);
            return "encoded:" + rawPassword;
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return encode(rawPassword).equals(encodedPassword);
        }

        /**
         * How many times this raw value was encoded. Counting rather than comparing
         * the result, because this encoder is deterministic: a re-encode returns an
         * identical string, so only the call count can distinguish a cached marker
         * from one recomputed on every call.
         */
        int encodeCountOf(String rawPassword) {
            return encodeCounts.getOrDefault(rawPassword, 0);
        }
    }
}
