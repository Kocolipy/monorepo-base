package com.example.backend.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.example.backend.audit.RecordingAuditTrail;
import com.example.backend.audit.domain.AuditOperation;
import com.example.backend.auth.InMemoryAccountSessions;
import com.example.backend.auth.MutableClock;
import com.example.backend.auth.PendingCommit;
import com.example.backend.auth.config.SecurityConfig;
import com.example.backend.scim.InMemoryScimPasswordHistoryRepository;
import com.example.backend.scim.InMemoryScimUserRepository;
import com.example.backend.scim.ScimIdentities;
import com.example.backend.scim.domain.LockoutPolicy;
import com.example.backend.scim.domain.PasswordPolicy;
import com.example.backend.scim.domain.ScimLoginState;
import com.example.backend.scim.domain.ScimUser;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * The self-service change against the in-memory directory: what an accepted change writes, and
 * what each refusal leaves behind. The same flow over HTTP, against Postgres and Redis, is
 * {@code PasswordChangeLifecycleIntegrationTests}.
 */
class PasswordChangeServiceTests {

    private static final String CURRENT = "the-current-password";
    private static final String NEXT = "a-brand-new-passphrase";
    private static final int MAX_ATTEMPTS = 3;

    private final PasswordEncoder encoder = new SecurityConfig().passwordEncoder();
    private final InMemoryScimUserRepository users = new InMemoryScimUserRepository();
    private final InMemoryScimPasswordHistoryRepository history =
            new InMemoryScimPasswordHistoryRepository();
    private final InMemoryAccountSessions accountSessions = new InMemoryAccountSessions();
    private final PendingCommit transaction = new PendingCommit();
    private final RecordingAuditTrail audit = new RecordingAuditTrail();
    private final MutableClock clock = new MutableClock(ScimIdentities.NOW);

    private PasswordChangeService service;
    private ScimUser ada;

    @BeforeEach
    void setUp() {
        LoginAttemptService attempts = new LoginAttemptService(
                users, accountSessions, transaction, new LockoutPolicy(MAX_ATTEMPTS), audit, clock);
        service = new PasswordChangeService(
                users,
                history,
                encoder,
                attempts,
                new ScimUserSessionRevocation(accountSessions, transaction, audit),
                audit,
                clock);
        ada = users.given(ScimIdentities.userWithLoginState("ada", new ScimLoginState(
                encoder.encode(CURRENT), 1, null, null, ScimIdentities.NOW)));
        accountSessions.open(ada.id(), "submitting-session");
        accountSessions.open(ada.id(), "other-session");
        clock.advanceBy(Duration.ofDays(1));
    }

    @Test
    void anAcceptedChangeHashesClearsTheFlagAdvancesTheVersionAndRevokesEverySessionAfterCommit() {
        service.changePassword(ada.id(), CURRENT, NEXT);

        ScimUser after = users.require("ada");
        assertThat(encoder.matches(NEXT, after.login().passwordHash())).isTrue();
        assertThat(after.login().passwordHash()).doesNotContain(NEXT);
        assertThat(after.login().isPasswordChangeRequired()).isFalse();
        assertThat(after.login().failedLoginAttempts())
                .as("a verified current password ends the failure run, as a login does")
                .isZero();
        assertThat(after.version()).isEqualTo(ada.version() + 1);
        assertThat(history.findRecentHashes(ada.id())).containsExactly(after.login().passwordHash());
        assertThat(audit.of(AuditOperation.PASSWORD_CHANGE))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.actorId()).isEqualTo(ada.id());
                    assertThat(event.subjectId()).isEqualTo(ada.id());
                    assertThat(event.detail()).as("an accepted change carries no reason").isNull();
                });

        assertThat(accountSessions.sessionsOf(ada.id()))
                .as("revocation waits for the commit")
                .hasSize(2);
        transaction.commit();
        assertThat(accountSessions.sessionsOf(ada.id()))
                .as("every session ends, the submitter's included")
                .isEmpty();
        assertThat(audit.of(AuditOperation.USER_SESSIONS_REVOKE))
                .singleElement()
                .extracting(RecordingAuditTrail.Recorded::detail)
                .isEqualTo("SUCCESS:PASSWORD");
    }

    /**
     * The completed change is use of the account — for a User that owed it, the first, since its
     * confined logins did not move the dormancy basis — so it records the authentication as of the
     * change. A refused change records none: see the refusal tests below.
     */
    @Test
    void anAcceptedChangeMovesTheDormancyBasis() {
        assertThat(ada.login().lastAuthenticatedAt()).isNull();

        service.changePassword(ada.id(), CURRENT, NEXT);

        assertThat(users.require("ada").login().lastAuthenticatedAt()).isEqualTo(clock.instant());
    }

    @Test
    void aRefusedChangeLeavesTheDormancyBasisAlone() {
        assertThatThrownBy(() -> service.changePassword(ada.id(), "not-the-password", NEXT))
                .isInstanceOf(CurrentPasswordRejectedException.class);
        assertThatThrownBy(() -> service.changePassword(ada.id(), CURRENT, "short"))
                .isInstanceOf(PasswordPolicyViolationException.class);

        assertThat(users.require("ada").login().lastAuthenticatedAt()).isNull();
    }

    @Test
    void aWrongCurrentPasswordIsRefusedAndCountedTowardTheLoginLockout() {
        assertThatThrownBy(() -> service.changePassword(ada.id(), "not-the-password", NEXT))
                .isInstanceOf(CurrentPasswordRejectedException.class);

        ScimUser after = users.require("ada");
        assertThat(after.login().failedLoginAttempts()).isEqualTo(2);
        assertThat(after.login().passwordHash()).isEqualTo(ada.login().passwordHash());
        assertThat(after.login().isPasswordChangeRequired()).isTrue();
        assertThat(audit.of(AuditOperation.PASSWORD_CHANGE))
                .extracting(RecordingAuditTrail.Recorded::detail)
                .containsExactly("BAD_CURRENT_PASSWORD");
    }

    @Test
    void atTheThresholdTheUserLocksItsSessionsEndAndEvenTheCorrectPasswordIsRefused() {
        assertThatThrownBy(() -> service.changePassword(ada.id(), "wrong-once", NEXT))
                .isInstanceOf(CurrentPasswordRejectedException.class);
        assertThatThrownBy(() -> service.changePassword(ada.id(), "wrong-twice", NEXT))
                .isInstanceOf(CurrentPasswordRejectedException.class);

        assertThat(users.require("ada").login().isLocked()).isTrue();
        assertThat(audit.of(AuditOperation.LOCKOUT_SET)).hasSize(1);
        transaction.commit();
        assertThat(accountSessions.sessionsOf(ada.id())).isEmpty();

        audit.reset();
        assertThatThrownBy(() -> service.changePassword(ada.id(), CURRENT, NEXT))
                .as("nothing lifts the lock but an Admin's Unlock")
                .isInstanceOf(CurrentPasswordRejectedException.class);
        clock.advanceBy(Duration.ofDays(365));
        assertThatThrownBy(() -> service.changePassword(ada.id(), CURRENT, NEXT))
                .as("and no passage of time does")
                .isInstanceOf(CurrentPasswordRejectedException.class);

        ScimUser after = users.require("ada");
        assertThat(encoder.matches(CURRENT, after.login().passwordHash())).isTrue();
        assertThat(after.login().failedLoginAttempts())
                .as("attempts against a lock are not counted")
                .isEqualTo(MAX_ATTEMPTS);
        assertThat(audit.of(AuditOperation.PASSWORD_CHANGE))
                .extracting(RecordingAuditTrail.Recorded::detail)
                .containsExactly("ACCOUNT_LOCKED", "ACCOUNT_LOCKED");
    }

    @Test
    void anInactiveUserIsRefusedWithoutAComparison() {
        ScimUser bob = users.given(ScimIdentities.inactiveUser("bob"));

        assertThatThrownBy(() -> service.changePassword(bob.id(), "anything-at-all", NEXT))
                .isInstanceOf(CurrentPasswordRejectedException.class);

        assertThat(users.require("bob").login().failedLoginAttempts()).isZero();
        assertThat(audit.of(AuditOperation.PASSWORD_CHANGE))
                .extracting(RecordingAuditTrail.Recorded::detail)
                .containsExactly("ACCOUNT_DISABLED");
    }

    @Test
    void anUnknownUserIsRefused() {
        assertThatThrownBy(() -> service.changePassword(java.util.UUID.randomUUID(), CURRENT, NEXT))
                .isInstanceOf(CurrentPasswordRejectedException.class);
        assertThat(audit.recorded()).isEmpty();
    }

    @Test
    void anAcceptedChangeIsStampedWithTheClockAndWritesTheCredentialOnceWithoutAFailureRun() {
        ScimUser bob = users.given(ScimIdentities.userWithLoginState("bob", new ScimLoginState(
                encoder.encode(CURRENT), 0, null, null, ScimIdentities.NOW)));
        int writesBefore = users.writes();

        service.changePassword(bob.id(), CURRENT, NEXT);

        assertThat(users.require("bob").lastModifiedAt()).isEqualTo(clock.instant());
        assertThat(users.writes())
                .as("no failure run to clear, so only the credential write")
                .isEqualTo(writesBefore + 1);
    }

    @Test
    void aUserWithNoCredentialIsRefusedAsABadCurrentPassword() {
        ScimUser carol = users.given(ScimIdentities.credentiallessUser("carol"));

        assertThatThrownBy(() -> service.changePassword(carol.id(), "anything-at-all", NEXT))
                .isInstanceOf(CurrentPasswordRejectedException.class);

        assertThat(users.require("carol").login().hasPassword()).isFalse();
        assertThat(audit.of(AuditOperation.PASSWORD_CHANGE))
                .extracting(RecordingAuditTrail.Recorded::detail)
                .containsExactly("BAD_CURRENT_PASSWORD");
    }

    @Test
    void aStandingRefusalIsAttributedToTheUser() {
        ScimUser bob = users.given(ScimIdentities.inactiveUser("bob"));
        ScimUser dan = users.given(ScimIdentities.userWithLoginState("dan", new ScimLoginState(
                encoder.encode(CURRENT), MAX_ATTEMPTS, ScimIdentities.NOW, null, null)));

        assertThatThrownBy(() -> service.changePassword(bob.id(), CURRENT, NEXT))
                .isInstanceOf(CurrentPasswordRejectedException.class);
        assertThatThrownBy(() -> service.changePassword(dan.id(), CURRENT, NEXT))
                .isInstanceOf(CurrentPasswordRejectedException.class);

        assertThat(audit.of(AuditOperation.PASSWORD_CHANGE))
                .extracting(RecordingAuditTrail.Recorded::subjectId, RecordingAuditTrail.Recorded::detail)
                .containsExactly(
                        tuple(bob.id(), "ACCOUNT_DISABLED"),
                        tuple(dan.id(), "ACCOUNT_LOCKED"));
    }

    @Test
    void aTooShortPasswordIsRefusedNamingTheRuleWithoutEitherValue() {
        assertPolicyRefusal("short-pass1", PasswordPolicy.Rule.TOO_SHORT, "TOO_SHORT");
    }

    @Test
    void aTooLongPasswordIsRefusedNamingTheRule() {
        assertPolicyRefusal("x".repeat(PasswordPolicy.MAX_LENGTH + 1),
                PasswordPolicy.Rule.TOO_LONG, "TOO_LONG");
    }

    @Test
    void aPasswordContainingTheUserNameIsRefusedNamingTheRule() {
        assertPolicyRefusal("my-name-is-ADA-okay", PasswordPolicy.Rule.CONTAINS_USER_NAME,
                "CONTAINS_USER_NAME");
    }

    @Test
    void theCurrentPasswordIsRefusedAsReused() {
        assertPolicyRefusal(CURRENT, PasswordPolicy.Rule.REUSED, "REUSED");
    }

    @Test
    void aRetainedPreviousPasswordIsRefusedAsReused() {
        String previous = "an-older-passphrase";
        history.record(ada.id(), encoder.encode(previous), ScimIdentities.NOW);

        assertPolicyRefusal(previous, PasswordPolicy.Rule.REUSED, "REUSED");
    }

    private void assertPolicyRefusal(String candidate, PasswordPolicy.Rule rule, String reason) {
        assertThatThrownBy(() -> service.changePassword(ada.id(), CURRENT, candidate))
                .isInstanceOfSatisfying(PasswordPolicyViolationException.class, refused -> {
                    assertThat(refused.ruleName()).isEqualTo(rule.name());
                    assertThat(refused.getMessage())
                            .isEqualTo(rule.message())
                            .doesNotContain(candidate)
                            .doesNotContain(CURRENT);
                });

        ScimUser after = users.require("ada");
        assertThat(after.login().passwordHash()).isEqualTo(ada.login().passwordHash());
        assertThat(after.login().isPasswordChangeRequired()).isTrue();
        assertThat(after.version()).isEqualTo(ada.version());
        assertThat(after.login().failedLoginAttempts())
                .as("a policy violation is not a credential failure")
                .isEqualTo(1);
        assertThat(audit.of(AuditOperation.PASSWORD_CHANGE))
                .singleElement()
                .extracting(RecordingAuditTrail.Recorded::detail)
                .isEqualTo(reason);
        transaction.commit();
        assertThat(accountSessions.sessionsOf(ada.id())).hasSize(2);
    }
}
