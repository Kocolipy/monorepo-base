package com.example.backend.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.audit.RecordingAuditTrail;
import com.example.backend.audit.RecordingAuditTrail.Recorded;
import com.example.backend.audit.domain.AuditOperation;
import com.example.backend.audit.domain.AuditRefusalReason;
import com.example.backend.auth.InMemoryAccountSessions;
import com.example.backend.auth.MutableClock;
import com.example.backend.auth.PendingCommit;
import com.example.backend.scim.InMemoryScimUserRepository;
import com.example.backend.scim.ScimIdentities;
import com.example.backend.scim.domain.LockoutPolicy;
import com.example.backend.scim.domain.NormalizedUserName;
import com.example.backend.scim.domain.ScimLoginState;
import com.example.backend.scim.domain.ReservedResourceName;
import com.example.backend.scim.domain.ScimUser;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Counting login attempts against the one login identity there is: a SCIM User.
 *
 * <p>The failure run and the lock instant live in {@code ScimLoginState} and are written
 * through the port's narrow login-state operation, so every assertion about them is read
 * back off {@code user.login()} rather than off a row of its own — there is no accounts
 * table any more.
 */
class LoginAttemptServiceTests {

    private static final Instant NOW = Instant.parse("2026-09-24T07:00:00Z");

    /** Far past any window the former expiring lockout could have had. */
    private static final Duration A_LONG_TIME = Duration.ofDays(3650);

    private final InMemoryScimUserRepository users = new InMemoryScimUserRepository();
    private final InMemoryAccountSessions sessions = new InMemoryAccountSessions();
    private final PendingCommit transaction = new PendingCommit();
    private final MutableClock clock = new MutableClock(NOW);
    private final RecordingAuditTrail audit = new RecordingAuditTrail();

    private LoginAttemptService attempts;

    @BeforeEach
    void setUp() {
        attempts = new LoginAttemptService(
                users, sessions, transaction, new LockoutPolicy(3), audit, clock);
        users.given(ScimIdentities.user("ada"));
    }

    @Test
    void aRefusedAttemptIsCountedAgainstTheIdentity() {
        attempts.recordFailure("ada", AuditRefusalReason.BAD_CREDENTIALS);

        assertThat(users.require("ada").login().failedLoginAttempts()).isEqualTo(1);
        assertThat(users.require("ada").login().isLocked()).isFalse();
    }

    @Test
    void theThirdConsecutiveRefusalLocksTheIdentity() {
        failTimes(3);

        ScimUser locked = users.require("ada");
        assertThat(locked.login().isLocked()).isTrue();
        assertThat(locked.login().lockedAt()).isEqualTo(NOW);
    }

    @Test
    void anAcceptedLoginResetsTheFailureCount() {
        failTimes(2);

        attempts.recordSuccess("ada");

        assertThat(users.require("ada").login().failedLoginAttempts()).isZero();
        assertThat(users.require("ada").login().lockedAt()).isNull();
    }

    /**
     * Recording nothing for a name that does not exist is what keeps a refusal
     * uninformative: no resource appears, so stored state cannot be used to enumerate
     * identities.
     */
    @Test
    void aRefusalForAnUnknownUsernameIsNotRecordedAnywhere() {
        attempts.recordFailure("nobody", AuditRefusalReason.BAD_CREDENTIALS);

        assertThat(users.findByNormalizedUserName(NormalizedUserName.of("nobody"))).isEmpty();
        assertThat(users.require("ada").login().failedLoginAttempts()).isZero();
    }

    @Test
    void anAcceptedLoginForAnUnknownUsernameIsANoOp() {
        attempts.recordSuccess("nobody");

        assertThat(users.findByNormalizedUserName(NormalizedUserName.of("nobody"))).isEmpty();
        assertThat(transaction.pending()).as("nothing to revoke for nobody").isZero();
    }

    // ---- one concurrent session per User ----------------------------------------------------

    /**
     * A blank name reaches here only if the web adapter's validation was bypassed; it is nobody,
     * exactly as an unknown name is — a subject-less failure, and a success that touches no one.
     */
    @Test
    void aBlankUsernameIsTreatedAsNobody() {
        attempts.recordFailure("   ", AuditRefusalReason.BAD_CREDENTIALS);
        attempts.recordSuccess("   ");
        attempts.recordFailure(null, AuditRefusalReason.BAD_CREDENTIALS);

        assertThat(users.require("ada").login().failedLoginAttempts()).isZero();
        assertThat(audit.of(AuditOperation.LOGIN_FAILURE))
                .extracting(Recorded::subjectId)
                .containsExactly(null, null);
        assertThat(audit.of(AuditOperation.LOGIN_SUCCESS)).isEmpty();
        assertThat(transaction.pending()).isZero();
    }

    /**
     * An accepted login ends every other session the User holds and keeps the one it is completed
     * in, and nobody else's session moves.
     */
    @Test
    void anAcceptedLoginEndsTheUsersOtherSessionsAndKeepsTheRetainedOne() {
        users.given(ScimIdentities.user("bob"));
        java.util.UUID ada = users.require("ada").id();
        java.util.UUID bob = users.require("bob").id();
        sessions.open(ada, "ada-earlier");
        sessions.open(ada, "ada-current");
        sessions.open(bob, "bob-only");

        attempts.recordSuccess("ada", "ada-current");
        transaction.commit();

        assertThat(sessions.sessionsOf(ada)).containsExactly("ada-current");
        assertThat(sessions.sessionsOf(bob)).containsExactly("bob-only");
        assertThat(sessions.loginRevocations()).containsExactly(ada);
    }

    /** A caller holding no session keeps none, so every earlier session of the User ends. */
    @Test
    void anAcceptedLoginWithoutASessionEndsEverySessionOfTheUser() {
        java.util.UUID ada = users.require("ada").id();
        sessions.open(ada, "ada-earlier");

        attempts.recordSuccess("ada");
        transaction.commit();

        assertThat(sessions.sessionsOf(ada)).isEmpty();
    }

    /**
     * The revocation waits for the commit: before it nothing has ended, and a login whose
     * transaction rolls back has signed its owner out nowhere.
     */
    @Test
    void theOtherSessionsEndOnlyOnceTheLoginCommits() {
        java.util.UUID ada = users.require("ada").id();
        sessions.open(ada, "ada-earlier");

        attempts.recordSuccess("ada", "ada-current");
        assertThat(sessions.sessionsOf(ada)).as("before the commit").containsExactly("ada-earlier");

        transaction.rollback();
        assertThat(sessions.sessionsOf(ada)).as("after a rollback").containsExactly("ada-earlier");
        assertThat(sessions.loginRevocations()).isEmpty();
    }

    /** A login confined by a required change follows the same rule as any other. */
    @Test
    void aConfinedLoginAlsoEndsTheUsersOtherSessions() {
        Instant earlier = NOW.minus(Duration.ofDays(10));
        users.given(ScimIdentities.userWithLoginState(
                "bob", new ScimLoginState("hash", 0, null, earlier, earlier)));
        java.util.UUID bob = users.require("bob").id();
        sessions.open(bob, "bob-earlier");
        sessions.open(bob, "bob-current");

        attempts.recordSuccess("bob", "bob-current");
        transaction.commit();

        assertThat(sessions.sessionsOf(bob)).containsExactly("bob-current");
    }

    /**
     * An identity with no failure run has nothing to clear, so the login must not rewrite
     * its failure run — every accepted login would otherwise cost a pointless update. (The
     * dormancy basis is still recorded, through its own narrow write, which the fake's write
     * count does not include; see the tests below.)
     */
    @Test
    void anAcceptedLoginOnAnUntouchedIdentityRewritesNoFailureRun() {
        int writesBefore = users.writes();

        attempts.recordSuccess("ada");

        assertThat(users.writes()).isEqualTo(writesBefore);
    }

    /**
     * Every accepted login records when it happened — the basis the inactivity jobs measure
     * dormancy from — and a later login moves it forward.
     */
    @Test
    void everyAcceptedLoginRecordsWhenItHappened() {
        assertThat(users.require("ada").login().lastAuthenticatedAt()).isNull();

        attempts.recordSuccess("ada");
        assertThat(users.require("ada").login().lastAuthenticatedAt()).isEqualTo(NOW);

        clock.advanceBy(Duration.ofDays(3));
        attempts.recordSuccess("ada");
        assertThat(users.require("ada").login().lastAuthenticatedAt())
                .isEqualTo(NOW.plus(Duration.ofDays(3)));
    }

    /**
     * Recording a login moves nothing a connector reads: the dormancy basis is not a SCIM
     * attribute, so neither the version nor {@code lastModified} advances.
     */
    @Test
    void recordingALoginDoesNotAdvanceTheVersion() {
        ScimUser before = users.require("ada");
        clock.advanceBy(Duration.ofHours(1));

        attempts.recordSuccess("ada");

        ScimUser after = users.require("ada");
        assertThat(after.version()).isEqualTo(before.version());
        assertThat(after.lastModifiedAt()).isEqualTo(before.lastModifiedAt());
    }

    /**
     * A login by a User that still owes a required password change is confined to the change and
     * logout, so it is not use of the account: it leaves the dormancy basis where it was, or an
     * imposed credential nobody replaces would never age into deactivation. It is still an
     * accepted login — the failure run clears and the success is audited.
     */
    @Test
    void aConfinedLoginDoesNotMoveTheDormancyBasis() {
        Instant earlier = NOW.minus(Duration.ofDays(10));
        users.given(ScimIdentities.userWithLoginState(
                "bob", new ScimLoginState("hash", 2, null, earlier, earlier)));

        attempts.recordSuccess("bob");

        ScimUser bob = users.require("bob");
        assertThat(bob.login().lastAuthenticatedAt()).isEqualTo(earlier);
        assertThat(bob.login().failedLoginAttempts()).isZero();
        assertThat(audit.of(AuditOperation.LOGIN_SUCCESS))
                .extracting(Recorded::subjectId)
                .containsExactly(bob.id());
    }

    /** A refused attempt is not an authentication, so it leaves the dormancy basis alone. */
    @Test
    void aRefusedAttemptDoesNotRecordAnAuthentication() {
        attempts.recordSuccess("ada");
        clock.advanceBy(Duration.ofDays(1));

        attempts.recordFailure("ada", AuditRefusalReason.BAD_CREDENTIALS);

        assertThat(users.require("ada").login().lastAuthenticatedAt()).isEqualTo(NOW);
    }

    @Test
    void anAcceptedLoginAfterAFailureDoesWrite() {
        attempts.recordFailure("ada", AuditRefusalReason.BAD_CREDENTIALS);
        int writesBefore = users.writes();

        attempts.recordSuccess("ada");

        assertThat(users.writes()).isEqualTo(writesBefore + 1);
    }

    /**
     * A failure run is not a SCIM attribute, so writing one must not move the resource's
     * ETag: a mistyped password cannot invalidate every cached copy of the User.
     */
    @Test
    void countingAFailureDoesNotAdvanceTheResourceVersion() {
        long versionBefore = users.require("ada").version();

        failTimes(3);

        assertThat(users.require("ada").version()).isEqualTo(versionBefore);
    }

    /**
     * No amount of elapsed time is a lift. The clock is moved a decade rather than a few
     * minutes so the assertion could not pass against a merely long window.
     */
    @Test
    void noPassageOfTimeEndsTheLockout() {
        failTimes(3);

        clock.advanceBy(A_LONG_TIME);
        attempts.recordFailure("ada", AuditRefusalReason.ACCOUNT_LOCKED);

        ScimUser stillLocked = users.require("ada");
        assertThat(stillLocked.login().isLocked()).isTrue();
        assertThat(stillLocked.login().lockedAt()).isEqualTo(NOW);
        assertThat(stillLocked.login().failedLoginAttempts()).isEqualTo(3);
    }

    // The sessions the lock takes away

    /**
     * A lock that left live sessions alone would close the front door while the identity
     * kept acting through a session it already held.
     */
    @Test
    void imposingTheLockoutRevokesTheIdentitysSessionsAfterTheCommit() {
        sessions.open(users.require("ada").id(), "session-1");

        failTimes(3);

        assertThat(transaction.pending()).isEqualTo(1);
        assertThat(sessions.revocations()).isEmpty();

        transaction.commit();

        assertThat(sessions.revocations()).containsExactly(users.require("ada").id());
        assertThat(sessions.sessionsOf(users.require("ada").id())).isEmpty();
    }

    /** A rolled-back transaction wrote no lock, so it must revoke nothing. */
    @Test
    void aRolledBackFailureRevokesNothing() {
        sessions.open(users.require("ada").id(), "session-1");

        failTimes(3);
        transaction.rollback();

        assertThat(sessions.revocations()).isEmpty();
        assertThat(sessions.sessionsOf(users.require("ada").id()))
                .containsExactly("session-1");
    }

    @Test
    void aFailureBelowTheLimitRevokesNothing() {
        failTimes(2);
        transaction.commit();

        assertThat(sessions.revocations()).isEmpty();
    }

    /** Only the transition into the lock revokes; a refusal after it does not. */
    @Test
    void anAttemptAgainstAnAlreadyLockedIdentityRevokesNothingFurther() {
        failTimes(3);
        transaction.commit();

        attempts.recordFailure("ada", AuditRefusalReason.ACCOUNT_LOCKED);
        transaction.commit();

        assertThat(sessions.revocations()).containsExactly(users.require("ada").id());
    }

    // The Bootstrap Admin, which is counted and audited but never locked

    @Test
    void theBootstrapAdminIsNeverLockedHoweverLongItsFailureRunGrows() {
        givenBootstrapAdmin("recovery-admin");

        failTimes(10, "recovery-admin");

        ScimUser recovery = users.require("recovery-admin");
        assertThat(recovery.login().isLocked()).isFalse();
        assertThat(recovery.login().lockedAt()).isNull();
        assertThat(recovery.login().failedLoginAttempts()).isEqualTo(10);
    }

    @Test
    void everyBootstrapAdminFailureIsAuditedAgainstItsStableId() {
        ScimUser recovery = givenBootstrapAdmin("recovery-admin");

        failTimes(10, "recovery-admin");

        assertThat(audit.of(AuditOperation.LOGIN_FAILURE)).hasSize(10)
                .allSatisfy(event -> assertThat(event.subjectId()).isEqualTo(recovery.id()));
        assertThat(audit.of(AuditOperation.LOCKOUT_SET)).isEmpty();
    }

    @Test
    void theBootstrapAdminKeepsItsSessionsThroughAFailureRun() {
        ScimUser recovery = givenBootstrapAdmin("recovery-admin");
        sessions.open(recovery.id(), "recovery-session");

        failTimes(10, "recovery-admin");
        transaction.commit();

        assertThat(sessions.revocations()).isEmpty();
        assertThat(sessions.sessionsOf(recovery.id())).containsExactly("recovery-session");
    }

    /**
     * The exemption is one identity's, not every administrator's — and it is keyed on the
     * reservation marker rather than on a configured name, which is the substantive change
     * from the account aggregate's version of this rule.
     *
     * <p>Arranged so a name comparison would get it exactly backwards: the ORDINARY
     * identity carries the name a configured-string exemption would have matched, and the
     * reserved one carries a name nothing could have been configured with. An
     * implementation that compared names would lock the reserved identity and spare the
     * ordinary one.
     */
    @Test
    void theExemptionFollowsTheReservationMarkerAndNotTheUserName() {
        users.given(ScimIdentities.user("recovery-admin"));
        givenBootstrapAdmin("someone-else");

        failTimes(3, "recovery-admin");
        failTimes(10, "someone-else");

        assertThat(users.require("recovery-admin").login().isLocked()).isTrue();
        assertThat(users.require("someone-else").login().isLocked()).isFalse();
        assertThat(users.require("someone-else").login().failedLoginAttempts()).isEqualTo(10);
    }

    // What the trail is told, which is the other half of counting an attempt

    @Test
    void aRefusedAttemptIsRecordedAgainstTheIdentitysStableIdWithItsReason() {
        attempts.recordFailure("ada", AuditRefusalReason.BAD_CREDENTIALS);

        assertThat(audit.recorded()).containsExactly(new Recorded(
                AuditOperation.LOGIN_FAILURE,
                null,
                users.require("ada").id(),
                AuditRefusalReason.BAD_CREDENTIALS.name()));
    }

    /**
     * No identity carries the name, so there is no subject — and the submitted value is
     * not recorded in its place. The reason the caller supplied is replaced too: whether
     * the name exists is settled here, by looking, not guessed from an exception type the
     * authentication library deliberately makes ambiguous.
     */
    @Test
    void aRefusalForAnUnknownUsernameIsRecordedWithNoSubjectAndItsOwnReason() {
        attempts.recordFailure("nobody", AuditRefusalReason.BAD_CREDENTIALS);

        assertThat(audit.recorded()).containsExactly(new Recorded(
                AuditOperation.LOGIN_FAILURE,
                null,
                null,
                AuditRefusalReason.UNKNOWN_ACCOUNT.name()));
    }

    @Test
    void reachingTheLimitRecordsTheLockoutOnceBesideEachRefusal() {
        failTimes(3);
        // Refused by the lockout now, which neither deepens it nor re-imposes it.
        attempts.recordFailure("ada", AuditRefusalReason.ACCOUNT_LOCKED);

        assertThat(audit.of(AuditOperation.LOCKOUT_SET)).containsExactly(new Recorded(
                AuditOperation.LOCKOUT_SET, null, users.require("ada").id(), null));
        assertThat(audit.of(AuditOperation.LOGIN_FAILURE)).hasSize(4);
        assertThat(audit.of(AuditOperation.LOCKOUT_LIFT)).isEmpty();
    }

    /**
     * Nothing on this path can record a lift. There is no unrequested lift to record: the
     * only one is an administrator's Unlock, which happens in
     * {@link IdentityAdministrationService} and names its actor.
     */
    @Test
    void noLoginAttemptEverRecordsALockoutLift() {
        failTimes(3);
        clock.advanceBy(A_LONG_TIME);

        attempts.recordFailure("ada", AuditRefusalReason.ACCOUNT_LOCKED);
        attempts.recordSuccess("ada");

        assertThat(audit.of(AuditOperation.LOCKOUT_LIFT)).isEmpty();
    }

    @Test
    void anAcceptedLoginIsRecordedAgainstTheIdentitysStableId() {
        attempts.recordSuccess("ada");

        assertThat(audit.recorded()).containsExactly(new Recorded(
                AuditOperation.LOGIN_SUCCESS,
                users.require("ada").id(),
                users.require("ada").id(),
                null));
    }

    @Test
    void anAcceptedLoginForAnUnknownUsernameRecordsNothing() {
        attempts.recordSuccess("nobody");

        assertThat(audit.recorded()).isEmpty();
    }

    // ---- a wrong current password on the self-service change -------------------------------

    /**
     * A wrong current password counts toward the same run as a refused login, is audited as a
     * refused change rather than a login failure, and locks — revoking every session after the
     * commit — once the threshold is reached.
     */
    @Test
    void aWrongCurrentPasswordCountsTowardTheRunAndLocksAtTheThreshold() {
        java.util.UUID ada = users.require("ada").id();
        sessions.open(ada, "ada-session");

        attempts.recordPasswordChangeFailure(ada);
        attempts.recordPasswordChangeFailure(ada);
        assertThat(users.require("ada").login().failedLoginAttempts()).isEqualTo(2);
        assertThat(users.require("ada").login().isLocked()).isFalse();
        assertThat(transaction.pending()).as("no lock, so nothing to revoke").isZero();

        attempts.recordPasswordChangeFailure(ada);

        assertThat(users.require("ada").login().isLocked()).isTrue();
        assertThat(users.require("ada").login().lockedAt()).isEqualTo(NOW);
        assertThat(audit.of(AuditOperation.LOCKOUT_SET))
                .extracting(Recorded::subjectId)
                .containsExactly(ada);
        assertThat(audit.of(AuditOperation.LOGIN_FAILURE)).isEmpty();
        assertThat(audit.recorded())
                .filteredOn(recorded -> recorded.operation() != AuditOperation.LOCKOUT_SET)
                .hasSize(3)
                .allSatisfy(recorded -> assertThat(recorded.subjectId()).isEqualTo(ada));
        assertThat(sessions.sessionsOf(ada)).as("before the commit").containsExactly("ada-session");
        transaction.commit();
        assertThat(sessions.sessionsOf(ada)).isEmpty();
        assertThat(sessions.revocations()).containsExactly(ada);
    }

    /** Past the threshold the lock is not re-imposed, so nothing is revoked or audited twice. */
    @Test
    void aWrongCurrentPasswordOnAnAlreadyLockedIdentityDoesNotReimposeTheLock() {
        java.util.UUID ada = users.require("ada").id();
        failTimes(3);
        transaction.commit();
        audit.reset();

        attempts.recordPasswordChangeFailure(ada);

        assertThat(audit.of(AuditOperation.LOCKOUT_SET)).isEmpty();
        assertThat(transaction.pending()).isZero();
    }

    /** The Bootstrap Admin's run is counted on this path too, and never locks. */
    @Test
    void theBootstrapAdminsWrongCurrentPasswordsAreCountedButNeverLock() {
        ScimUser admin = givenBootstrapAdmin("recovery");

        for (int attempt = 0; attempt < 5; attempt++) {
            attempts.recordPasswordChangeFailure(admin.id());
        }

        assertThat(users.require("recovery").login().failedLoginAttempts()).isEqualTo(5);
        assertThat(users.require("recovery").login().isLocked()).isFalse();
        assertThat(transaction.pending()).isZero();
    }

    /** An id naming nobody is ignored: nothing counted, nothing audited. */
    @Test
    void aWrongCurrentPasswordForAnUnknownIdRecordsNothing() {
        attempts.recordPasswordChangeFailure(java.util.UUID.randomUUID());

        assertThat(audit.recorded()).isEmpty();
        assertThat(users.require("ada").login().failedLoginAttempts()).isZero();
    }

    /**
     * The reservation is applied through the port, because production has no other way to
     * produce one: there is deliberately no factory that mints a reserved resource.
     */
    private ScimUser givenBootstrapAdmin(String userName) {
        return users.createReserved(
                ScimIdentities.user(userName), ReservedResourceName.BOOTSTRAP_ADMIN);
    }

    private void failTimes(int times) {
        failTimes(times, "ada");
    }

    private void failTimes(int times, String userName) {
        for (int attempt = 0; attempt < times; attempt++) {
            attempts.recordFailure(userName, AuditRefusalReason.BAD_CREDENTIALS);
        }
    }
}
