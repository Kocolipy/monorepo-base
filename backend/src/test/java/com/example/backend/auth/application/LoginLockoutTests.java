package com.example.backend.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.backend.audit.RecordingAuditTrail;
import com.example.backend.audit.domain.AuditOperation;
import com.example.backend.auth.InMemoryAccountRepository;
import com.example.backend.auth.InMemoryAccountSessions;
import com.example.backend.auth.MutableClock;
import com.example.backend.auth.PendingCommit;
import com.example.backend.auth.config.SecurityConfig;
import com.example.backend.auth.domain.Account;
import com.example.backend.auth.domain.AccountRole;
import com.example.backend.auth.domain.BootstrapAdmin;
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
 * attempt counter, the administration use case that lifts a lock, and the account
 * store Spring Security reads its {@code UserDetails} from. Wired by hand rather
 * than through a Spring context so the clock can be moved and no database is
 * needed.
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

    /**
     * Ten years. The lockout used to lift after a configured window, so a small
     * advance could not tell "permanent" from "long"; this one is past any window a
     * deployment could plausibly have set.
     */
    private static final Duration A_LONG_TIME = Duration.ofDays(3650);

    private static final String CORRECT_PASSWORD = "correct-password";

    private static final String BOOTSTRAP_ADMIN = "recovery-admin";

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final InMemoryAccountSessions sessions = new InMemoryAccountSessions();
    private final PendingCommit transaction = new PendingCommit();
    private final MutableClock clock = new MutableClock(NOW);
    private final RecordingAuditTrail audit = new RecordingAuditTrail();

    private LoginService login;

    private AccountAdministrationService administration;

    @BeforeEach
    void setUp() {
        SecurityConfig config = new SecurityConfig();
        PasswordEncoder passwordEncoder = config.passwordEncoder();
        accounts.save(new Account(
                "ada", passwordEncoder.encode(CORRECT_PASSWORD), AccountRole.USER));
        accounts.save(new Account(
                BOOTSTRAP_ADMIN, passwordEncoder.encode(CORRECT_PASSWORD), AccountRole.ADMIN));
        AccountService users = new AccountService(accounts, passwordEncoder, clock);
        login = new LoginService(
                config.authenticationManager(users, passwordEncoder),
                new LoginAttemptService(
                        accounts,
                        sessions,
                        transaction,
                        new LockoutPolicy(5),
                        new BootstrapAdmin(BOOTSTRAP_ADMIN),
                        audit,
                        clock),
                users);
        administration = new AccountAdministrationService(
                accounts, sessions, transaction, audit, new BootstrapAdmin(BOOTSTRAP_ADMIN));
    }

    @Test
    void anAcceptedLoginReportsTheAccountAndItsRole() {
        Authentication authentication = login.logIn("ada", CORRECT_PASSWORD).authentication();

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
        assertThat(accounts.require("ada").lockedAt()).isNull();
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
        failFiveTimes();

        assertThat(accounts.require("ada").isLocked()).isTrue();
        assertThat(accounts.require("ada").lockedAt()).isEqualTo(NOW);
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
    void attemptsDuringTheLockoutDoNotDeepenIt() {
        lockTheAccount();
        Instant lockedAt = accounts.require("ada").lockedAt();

        clock.advanceBy(Duration.ofMinutes(1));
        submit("wrong");
        submit(CORRECT_PASSWORD);

        assertThat(accounts.require("ada").lockedAt()).isEqualTo(lockedAt);
        assertThat(accounts.require("ada").failedLoginAttempts()).isEqualTo(5);
    }

    /**
     * The criterion the whole change exists for: no clock advance is a lift, so the
     * correct password is still refused a decade later.
     */
    @Test
    void theCorrectPasswordIsStillRefusedHoweverLongTheLockoutHasStood() {
        lockTheAccount();

        clock.advanceBy(A_LONG_TIME);

        assertThatThrownBy(() -> login.logIn("ada", CORRECT_PASSWORD))
                .isInstanceOf(LockedException.class);
        assertThat(accounts.require("ada").isLocked()).isTrue();
    }

    @Test
    void aRefusalAfterAnyAmountOfTimeDoesNotStartAFreshRun() {
        lockTheAccount();

        clock.advanceBy(A_LONG_TIME);
        submit("wrong");

        assertThat(accounts.require("ada").failedLoginAttempts()).isEqualTo(5);
        assertThat(accounts.require("ada").isLocked()).isTrue();
    }

    /**
     * Unlock is the whole mechanism: the password was never changed, so an accepted
     * login afterward proves the lock and only the lock was what refused it.
     */
    @Test
    void anAdministratorsUnlockIsTheOnlyThingThatLetsTheAccountBackIn() {
        lockTheAccount();
        clock.advanceBy(A_LONG_TIME);

        administration.unlock("ada", BOOTSTRAP_ADMIN);

        Authentication authentication = login.logIn("ada", CORRECT_PASSWORD).authentication();
        assertThat(authentication.getName()).isEqualTo("ada");
        assertThat(accounts.require("ada").failedLoginAttempts()).isZero();
        assertThat(accounts.require("ada").lockedAt()).isNull();
    }

    /** And the lift is recorded against the administrator who performed it. */
    @Test
    void theUnlockIsAuditedWithItsAdministratorAsActor() {
        lockTheAccount();
        audit.reset();

        administration.unlock("ada", BOOTSTRAP_ADMIN);

        assertThat(audit.of(AuditOperation.LOCKOUT_LIFT))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.actorId())
                            .isEqualTo(accounts.require(BOOTSTRAP_ADMIN).id());
                    assertThat(event.subjectId()).isEqualTo(accounts.require("ada").id());
                });
    }

    /**
     * A locked account acts through the sessions it already holds unless the lock
     * takes them, so the lock takes them.
     */
    @Test
    void imposingTheLockoutEndsTheSessionsTheAccountAlreadyHeld() {
        sessions.open(accounts.require("ada").id(), "session-before-the-lock");

        lockTheAccount();
        transaction.commit();

        assertThat(sessions.sessionsOf(accounts.require("ada").id())).isEmpty();
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

    // The Bootstrap Admin, the one principal a failure run cannot close

    /**
     * Well past the threshold, the recovery identity still logs in. Without this
     * the permanent lockout would make an unauthenticated attacker able to brick
     * the deployment.
     */
    @Test
    void theBootstrapAdminStillLogsInAfterFarMoreFailuresThanTheThreshold() {
        for (int attempt = 0; attempt < 10; attempt++) {
            submitAs(BOOTSTRAP_ADMIN, "wrong");
        }

        assertThat(accounts.require(BOOTSTRAP_ADMIN).isLocked()).isFalse();
        assertThat(accounts.require(BOOTSTRAP_ADMIN).failedLoginAttempts()).isEqualTo(10);

        Authentication authentication =
                login.logIn(BOOTSTRAP_ADMIN, CORRECT_PASSWORD).authentication();

        assertThat(authentication.getName()).isEqualTo(BOOTSTRAP_ADMIN);
        assertThat(accounts.require(BOOTSTRAP_ADMIN).failedLoginAttempts()).isZero();
    }

    @Test
    void everyBootstrapAdminFailureIsStillAudited() {
        for (int attempt = 0; attempt < 10; attempt++) {
            submitAs(BOOTSTRAP_ADMIN, "wrong");
        }

        assertThat(audit.of(AuditOperation.LOGIN_FAILURE)).hasSize(10)
                .allSatisfy(event -> assertThat(event.subjectId())
                        .isEqualTo(accounts.require(BOOTSTRAP_ADMIN).id()));
        assertThat(audit.of(AuditOperation.LOCKOUT_SET)).isEmpty();
    }

    private void lockTheAccount() {
        failFiveTimes();
        assertThat(accounts.require("ada").isLocked()).isTrue();
    }

    private void failFiveTimes() {
        for (int attempt = 0; attempt < 5; attempt++) {
            submit("wrong");
        }
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
