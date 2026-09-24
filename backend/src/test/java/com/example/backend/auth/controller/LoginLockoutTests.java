package com.example.backend.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.backend.auth.InMemoryAccountRepository;
import com.example.backend.auth.MutableClock;
import com.example.backend.auth.application.AccountService;
import com.example.backend.auth.application.LoginAttemptService;
import com.example.backend.auth.config.SecurityConfig;
import com.example.backend.auth.domain.Account;
import com.example.backend.auth.domain.AccountRole;
import com.example.backend.auth.domain.LockoutPolicy;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * The lockout story end to end over the real authentication chain: the
 * controller, the attempt counter, and the account store Spring Security reads
 * its {@code UserDetails} from. Wired by hand rather than through a Spring
 * context so the clock can be moved and no database is needed.
 */
class LoginLockoutTests {

    private static final Instant NOW = Instant.parse("2026-09-24T07:00:00Z");

    private static final Duration LOCKOUT = Duration.ofMinutes(5);

    private static final String CORRECT_PASSWORD = "correct-password";

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final MutableClock clock = new MutableClock(NOW);

    private AuthController controller;

    @BeforeEach
    void setUp() {
        SecurityConfig config = new SecurityConfig();
        PasswordEncoder passwordEncoder = config.passwordEncoder();
        accounts.save(new Account(
                "ada", passwordEncoder.encode(CORRECT_PASSWORD), AccountRole.USER));
        AccountService users = new AccountService(accounts, passwordEncoder, clock);
        DefaultCookieSerializer cookieSerializer = new DefaultCookieSerializer();
        cookieSerializer.setCookieName("JSESSIONID");
        controller = new AuthController(
                config.authenticationManager(users, passwordEncoder),
                config.securityContextRepository(),
                config.sessionAuthenticationStrategy(),
                config.csrfTokenRepository(),
                cookieSerializer,
                new LoginAttemptService(accounts, new LockoutPolicy(3, LOCKOUT), clock));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anAcceptedLoginCreatesASessionAndResetsTheFailureCount() {
        submit("wrong");
        submit("wrong");
        MockHttpServletRequest request = new MockHttpServletRequest();

        controller.login(
                new AuthController.LoginRequest("ada", CORRECT_PASSWORD),
                request,
                new MockHttpServletResponse());

        assertThat(request.getSession(false)).isNotNull();
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
    void theThirdRefusalLocksTheAccount() {
        submit("wrong");
        submit("wrong");
        submit("wrong");

        assertThat(accounts.require("ada").isLocked(clock.instant())).isTrue();
        assertThat(accounts.require("ada").lockedUntil()).isEqualTo(NOW.plus(LOCKOUT));
    }

    /**
     * The point of the lockout: while it holds, the right password is refused too,
     * and no session is created for it.
     */
    @Test
    void aLockedAccountIsRefusedEvenWithTheCorrectPassword() {
        lockTheAccount();
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThatThrownBy(() -> controller.login(
                new AuthController.LoginRequest("ada", CORRECT_PASSWORD),
                request,
                new MockHttpServletResponse()))
                .isInstanceOf(LockedException.class);
        assertThat(request.getSession(false)).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void attemptsDuringTheLockoutDoNotExtendIt() {
        lockTheAccount();
        Instant lockedUntil = accounts.require("ada").lockedUntil();

        clock.advanceBy(Duration.ofMinutes(1));
        submit("wrong");
        submit(CORRECT_PASSWORD);

        assertThat(accounts.require("ada").lockedUntil()).isEqualTo(lockedUntil);
        assertThat(accounts.require("ada").failedLoginAttempts()).isEqualTo(3);
    }

    @Test
    void theCorrectPasswordIsAcceptedOnceTheLockoutExpires() {
        lockTheAccount();

        clock.advanceBy(LOCKOUT);
        MockHttpServletRequest request = new MockHttpServletRequest();
        AuthController.UserResponse response = controller.login(
                new AuthController.LoginRequest("ada", CORRECT_PASSWORD),
                request,
                new MockHttpServletResponse());

        assertThat(response.username()).isEqualTo("ada");
        assertThat(request.getSession(false)).isNotNull();
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
     * which the controller's handler answers with a bare 401 — so the response
     * cannot be used to tell a real account from an unknown one.
     */
    @Test
    void aLockedAccountAndAWrongPasswordAreRefusedTheSameWay() {
        AuthenticationException wrongPassword = submit("wrong");
        assertThat(wrongPassword).isInstanceOf(BadCredentialsException.class);

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
        assertThat(accounts.require("ada").isLocked(clock.instant())).isTrue();
    }

    /** Submits a login expected to be refused, returning the refusal. */
    private AuthenticationException submit(String password) {
        return submitAs("ada", password);
    }

    private AuthenticationException submitAs(String username, String password) {
        try {
            controller.login(
                    new AuthController.LoginRequest(username, password),
                    new MockHttpServletRequest(),
                    new MockHttpServletResponse());
            throw new AssertionError("Expected the login to be refused");
        } catch (AuthenticationException refused) {
            return refused;
        }
    }
}
