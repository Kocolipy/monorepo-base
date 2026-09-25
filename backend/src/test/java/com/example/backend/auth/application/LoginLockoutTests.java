package com.example.backend.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.backend.auth.InMemoryAccountRepository;
import com.example.backend.auth.MutableClock;
import com.example.backend.auth.config.SecurityConfig;
import com.example.backend.auth.domain.Account;
import com.example.backend.auth.domain.AccountRole;
import com.example.backend.auth.domain.LockoutPolicy;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * The lockout story over the real authentication chain: the login module, the
 * attempt counter, and the account store Spring Security reads its
 * {@code UserDetails} from. Wired by hand rather than through a Spring context so
 * the clock can be moved and no database is needed.
 *
 * <p>Driven through {@link LoginService} rather than through the login endpoint,
 * because that is the module the counting belongs to: any entry point that
 * authenticates submitted credentials goes through here and gets this behaviour,
 * and none of it depends on there being an HTTP session to create. What the
 * endpoint adds on top — the session, the CSRF token, the bare {@code 401} — is
 * asserted in {@code AuthControllerTests}.
 */
class LoginLockoutTests {

    private static final Instant NOW = Instant.parse("2026-09-24T07:00:00Z");

    private static final Duration LOCKOUT = Duration.ofMinutes(20);

    private static final String CORRECT_PASSWORD = "correct-password";

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final MutableClock clock = new MutableClock(NOW);

    private LoginService login;

    @BeforeEach
    void setUp() {
        SecurityConfig config = new SecurityConfig();
        PasswordEncoder passwordEncoder = config.passwordEncoder();
        accounts.save(new Account(
                "ada", passwordEncoder.encode(CORRECT_PASSWORD), AccountRole.USER));
        AccountService users = new AccountService(accounts, passwordEncoder, clock);
        login = new LoginService(
                config.authenticationManager(users, passwordEncoder),
                new LoginAttemptService(accounts, new LockoutPolicy(5, LOCKOUT), clock));
    }

    @Test
    void anAcceptedLoginReportsTheAccountAndItsRole() {
        Authentication authentication = login.logIn("ada", CORRECT_PASSWORD);

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getName()).isEqualTo("ada");
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .contains("ROLE_USER");
    }

    @Test
    void anAcceptedLoginResetsTheFailureCount() {
        submit("wrong");
        submit("wrong");

        login.logIn("ada", CORRECT_PASSWORD);

        assertThat(accounts.require("ada").failedLoginAttempts()).isZero();
        assertThat(accounts.require("ada").lockedUntil()).isNull();
    }

    @Test
    void eachRefusedLoginIncrementsTheFailureCount() {
        submit("wrong");
        assertThat(accounts.require("ada").failedLoginAttempts()).isEqualTo(1);

        submit("wrong");
        assertThat(accounts.require("ada").failedLoginAttempts()).isEqualTo(2);
    }

    @Test
    void theFifthRefusalLocksTheAccount() {
        submit("wrong");
        submit("wrong");
        submit("wrong");
        submit("wrong");
        submit("wrong");

        assertThat(accounts.require("ada").isLocked(clock.instant())).isTrue();
        assertThat(accounts.require("ada").lockedUntil()).isEqualTo(NOW.plus(LOCKOUT));
    }

    /**
     * The point of the lockout: while it holds, the right password is refused too,
     * and no authentication is handed back for it.
     */
    @Test
    void aLockedAccountIsRefusedEvenWithTheCorrectPassword() {
        lockTheAccount();

        assertThatThrownBy(() -> login.logIn("ada", CORRECT_PASSWORD))
                .isInstanceOf(LockedException.class);
    }

    @Test
    void attemptsDuringTheLockoutDoNotExtendIt() {
        lockTheAccount();
        Instant lockedUntil = accounts.require("ada").lockedUntil();

        clock.advanceBy(Duration.ofMinutes(1));
        submit("wrong");
        submit(CORRECT_PASSWORD);

        assertThat(accounts.require("ada").lockedUntil()).isEqualTo(lockedUntil);
        assertThat(accounts.require("ada").failedLoginAttempts()).isEqualTo(5);
    }

    @Test
    void theCorrectPasswordIsAcceptedOnceTheLockoutExpires() {
        lockTheAccount();

        clock.advanceBy(LOCKOUT);
        Authentication authentication = login.logIn("ada", CORRECT_PASSWORD);

        assertThat(authentication.getName()).isEqualTo("ada");
        assertThat(accounts.require("ada").failedLoginAttempts()).isZero();
        assertThat(accounts.require("ada").lockedUntil()).isNull();
    }

    @Test
    void aRefusalAfterTheLockoutExpiresStartsAFreshRunRatherThanRelocking() {
        lockTheAccount();

        clock.advanceBy(LOCKOUT);
        submit("wrong");

        assertThat(accounts.require("ada").failedLoginAttempts()).isEqualTo(1);
        assertThat(accounts.require("ada").isLocked(clock.instant())).isFalse();
    }

    /**
     * A locked account and a wrong password both leave as the same exception type,
     * which the endpoint's handler answers with a bare 401 — so the response
     * cannot be used to tell a real account from an unknown one.
     */
    @Test
    void aLockedAccountAndAWrongPasswordAreRefusedTheSameWay() {
        AuthenticationException wrongPassword = submit("wrong");
        assertThat(wrongPassword).isInstanceOf(BadCredentialsException.class);

        submit("wrong");
        submit("wrong");
        submit("wrong");
        submit("wrong");
        AuthenticationException locked = submit(CORRECT_PASSWORD);

        assertThat(locked).isInstanceOf(AuthenticationException.class);
        assertThat(AuthenticationException.class)
                .isAssignableFrom(wrongPassword.getClass())
                .isAssignableFrom(locked.getClass());
    }

    @Test
    void anUnknownUsernameLeavesNothingBehind() {
        submitAs("nobody", "whatever");

        assertThat(accounts.findByUsername("nobody")).isEmpty();
        assertThat(accounts.require("ada").failedLoginAttempts()).isZero();
    }

    private void lockTheAccount() {
        submit("wrong");
        submit("wrong");
        submit("wrong");
        submit("wrong");
        submit("wrong");
        assertThat(accounts.require("ada").isLocked(clock.instant())).isTrue();
    }

    /** Submits a login expected to be refused, returning the refusal. */
    private AuthenticationException submit(String password) {
        return submitAs("ada", password);
    }

    private AuthenticationException submitAs(String username, String password) {
        try {
            login.logIn(username, password);
            throw new AssertionError("Expected the login to be refused");
        } catch (AuthenticationException refused) {
            return refused;
        }
    }
}
