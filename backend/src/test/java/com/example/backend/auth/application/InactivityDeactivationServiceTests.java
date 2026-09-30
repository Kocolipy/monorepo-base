package com.example.backend.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.example.backend.audit.CapturedLog;
import com.example.backend.audit.RecordingAuditTrail;
import com.example.backend.audit.RecordingAuditTrail.Recorded;
import com.example.backend.audit.domain.AuditOperation;
import com.example.backend.auth.InMemoryAccountSessions;
import com.example.backend.auth.InMemoryScheduledJobLock;
import com.example.backend.auth.MutableClock;
import com.example.backend.auth.PendingCommit;
import com.example.backend.auth.domain.ScheduledJob;
import com.example.backend.observability.LogEvent;
import com.example.backend.scim.InMemoryScimUserRepository;
import com.example.backend.scim.ScimIdentities;
import com.example.backend.scim.domain.DormancyPolicy;
import com.example.backend.scim.domain.ReservedResourceName;
import com.example.backend.scim.domain.ScimUser;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The inactivity job against the in-memory directory: who it deactivates, what each deactivation
 * does, and what it leaves alone. The same rules against Postgres, with real concurrency and the
 * real seeded Bootstrap Admin, are {@code InactivityGovernanceIntegrationTests}.
 */
class InactivityDeactivationServiceTests {

    /** When every fixture User is created. */
    private static final Instant CREATED = ScimIdentities.NOW;

    private static final Duration WINDOW = DormancyPolicy.DEFAULT_DEACTIVATION_WINDOW;

    private final InMemoryScimUserRepository users = new InMemoryScimUserRepository();
    private final InMemoryAccountSessions accountSessions = new InMemoryAccountSessions();
    private final PendingCommit transaction = new PendingCommit();
    private final RecordingAuditTrail audit = new RecordingAuditTrail();
    private final InMemoryScheduledJobLock lock = new InMemoryScheduledJobLock();
    private final MutableClock clock = new MutableClock(CREATED);

    private InactivityDeactivationService job;

    @BeforeEach
    void setUp() {
        job = new InactivityDeactivationService(
                users,
                new ScimUserSessionRevocation(accountSessions, transaction, audit),
                lock,
                DormancyPolicy.defaults(),
                audit,
                clock);
    }

    // ---- the demo oracle ------------------------------------------------------------------

    /**
     * A User that has not authenticated for longer than 90 days is deactivated on the next run:
     * {@code active=false}, the version advanced once, an actorless event naming it, and its
     * sessions ended — after the commit, recorded as their own actorless event.
     */
    @Test
    void aUserPastTheWindowIsDeactivatedWithItsVersionAdvancedSessionsRevokedAndAnEvent() {
        ScimUser ada = users.given(authenticatedAt("ada", CREATED));
        accountSessions.open(ada.id(), "ada-session");
        clock.advanceBy(WINDOW.plusSeconds(1));

        DormancyRun run = job.deactivateDormantUsers();

        ScimUser after = users.require("ada");
        assertThat(run.skipped()).isFalse();
        assertThat(run.processed()).containsExactly(ada.id());
        assertThat(after.profile().active()).isFalse();
        assertThat(after.version()).isEqualTo(ada.version() + 1);
        assertThat(after.lastModifiedAt()).isEqualTo(clock.instant());
        assertThat(audit.of(AuditOperation.INACTIVITY_DEACTIVATION))
                .containsExactly(new Recorded(
                        AuditOperation.INACTIVITY_DEACTIVATION, null, ada.id(), null));
        assertThat(accountSessions.sessionsOf(ada.id()))
                .as("nothing is revoked before the commit").containsExactly("ada-session");

        transaction.commit();

        assertThat(accountSessions.sessionsOf(ada.id())).isEmpty();
        assertThat(audit.of(AuditOperation.USER_SESSIONS_REVOKE))
                .containsExactly(new Recorded(
                        AuditOperation.USER_SESSIONS_REVOKE, null, ada.id(), "SUCCESS:ACTIVE"));
    }

    /** A rolled-back run revokes nothing, as every deferred revocation does (ADR 0002). */
    @Test
    void aRunWhoseTransactionRollsBackRevokesNothing() {
        ScimUser ada = users.given(authenticatedAt("ada", CREATED));
        accountSessions.open(ada.id(), "ada-session");
        clock.advanceBy(WINDOW.plusSeconds(1));

        job.deactivateDormantUsers();
        transaction.rollback();

        assertThat(accountSessions.revocations()).isEmpty();
        assertThat(accountSessions.sessionsOf(ada.id())).containsExactly("ada-session");
    }

    // ---- the window and its basis ---------------------------------------------------------

    /** Exactly at the window is not yet past it; one instant later is. */
    @Test
    void theWindowIsExclusiveAtItsBoundary() {
        users.given(authenticatedAt("ada", CREATED));

        clock.advanceBy(WINDOW);
        assertThat(job.deactivateDormantUsers().processed()).isEmpty();
        assertThat(users.require("ada").profile().active()).isTrue();

        clock.advanceBy(Duration.ofNanos(1));
        assertThat(job.deactivateDormantUsers().processed()).hasSize(1);
        assertThat(users.require("ada").profile().active()).isFalse();
    }

    /**
     * Measured from the last authentication, not from creation: a User created long ago that
     * logged in recently is not dormant.
     */
    @Test
    void aRecentAuthenticationKeepsAnOldUserActive() {
        clock.advanceBy(WINDOW.multipliedBy(3));
        users.given(authenticatedAt("ada", clock.instant().minus(Duration.ofDays(1))));

        assertThat(job.deactivateDormantUsers().processed()).isEmpty();
        assertThat(users.require("ada").profile().active()).isTrue();
    }

    /**
     * A User that never authenticated is measured from its creation — so a credentialless User
     * is not deactivated the moment it is provisioned, and is once the window has passed.
     */
    @Test
    void aUserThatNeverAuthenticatedIsMeasuredFromItsCreation() {
        ScimUser never = users.given(ScimIdentities.credentiallessUser("never"));
        assertThat(never.login().lastAuthenticatedAt()).isNull();

        assertThat(job.deactivateDormantUsers().processed()).isEmpty();

        clock.advanceBy(WINDOW.plusSeconds(1));
        assertThat(job.deactivateDormantUsers().processed()).containsExactly(never.id());
    }

    /** A configured window replaces the default. */
    @Test
    void aConfiguredWindowIsTheOneApplied() {
        job = new InactivityDeactivationService(
                users,
                new ScimUserSessionRevocation(accountSessions, transaction, audit),
                lock,
                new DormancyPolicy(Duration.ofDays(10), null),
                audit,
                clock);
        users.given(authenticatedAt("ada", CREATED));
        clock.advanceBy(Duration.ofDays(11));

        assertThat(job.deactivateDormantUsers().processed()).hasSize(1);
    }

    // ---- what it never touches ------------------------------------------------------------

    /** The Bootstrap Admin is never deactivated, however long it has been dormant. */
    @Test
    void theBootstrapAdminIsNeverDeactivated() {
        ScimUser bootstrap = users.createReserved(
                ScimIdentities.user("root"), ReservedResourceName.BOOTSTRAP_ADMIN);
        accountSessions.open(bootstrap.id(), "root-session");
        clock.advanceBy(WINDOW.multipliedBy(10));

        assertThat(job.deactivateDormantUsers().processed()).isEmpty();
        transaction.commit();

        ScimUser after = users.require("root");
        assertThat(after.profile().active()).isTrue();
        assertThat(after.version()).isEqualTo(bootstrap.version());
        assertThat(accountSessions.sessionsOf(bootstrap.id())).containsExactly("root-session");
        assertThat(audit.recorded()).isEmpty();
    }

    /**
     * The job decides again on the locked read, so a stale candidate list cannot make it act: not
     * on the Bootstrap Admin (were the query's filter lost), not on a User that authenticated since
     * the list was read, not on one already inactive, and not on one that no longer exists.
     */
    @Test
    void aStaleCandidateIsDecidedAgainOnTheLockedRead() {
        ScimUser bootstrap = users.createReserved(
                ScimIdentities.user("root"), ReservedResourceName.BOOTSTRAP_ADMIN);
        ScimUser inactive = users.given(ScimIdentities.inactiveUser("gone"));
        clock.advanceBy(WINDOW.multipliedBy(2));
        ScimUser fresh = users.given(authenticatedAt("fresh", clock.instant()));
        users.answerDormancyCandidatesWith(
                List.of(bootstrap.id(), inactive.id(), fresh.id(), UUID.randomUUID()));
        int writesBefore = users.writes();

        DormancyRun run = job.deactivateDormantUsers();

        assertThat(run.processed()).isEmpty();
        assertThat(users.writes()).isEqualTo(writesBefore);
        assertThat(users.require("root").profile().active()).isTrue();
        assertThat(users.require("fresh").profile().active()).isTrue();
        assertThat(users.require("gone").version()).isEqualTo(inactive.version());
        assertThat(audit.recorded()).isEmpty();
        assertThat(transaction.pending()).isZero();
    }

    /** An already-deactivated User is not processed again: no version bump, no event. */
    @Test
    void aSecondRunDoesNotProcessTheSameUserAgain() {
        users.given(authenticatedAt("ada", CREATED));
        clock.advanceBy(WINDOW.plusSeconds(1));
        job.deactivateDormantUsers();
        long versionAfterFirst = users.require("ada").version();
        audit.reset();

        DormancyRun second = job.deactivateDormantUsers();

        assertThat(second.processed()).isEmpty();
        assertThat(users.require("ada").version()).isEqualTo(versionAfterFirst);
        assertThat(audit.recorded()).isEmpty();
    }

    // ---- serialization --------------------------------------------------------------------

    /**
     * While another run of THIS job holds its lock, a run skips and changes nothing. The lock asked
     * for is this job's own, which is what keeps it from waiting on the other dormancy job.
     */
    @Test
    void aRunSkipsWhileAnotherRunOfTheSameJobHoldsItsLock() {
        users.given(authenticatedAt("ada", CREATED));
        clock.advanceBy(WINDOW.plusSeconds(1));
        lock.holdElsewhere(ScheduledJob.INACTIVITY_DEACTIVATION);

        DormancyRun run = job.deactivateDormantUsers();

        assertThat(run.skipped()).isTrue();
        assertThat(run.processed()).isEmpty();
        assertThat(users.require("ada").profile().active()).isTrue();
        assertThat(audit.recorded()).isEmpty();
        assertThat(lock.attempts()).containsExactly(ScheduledJob.INACTIVITY_DEACTIVATION);
    }

    /** The other job's lock being held is no reason to skip. */
    @Test
    void theOtherJobHoldingItsLockDoesNotStopThisOne() {
        users.given(authenticatedAt("ada", CREATED));
        clock.advanceBy(WINDOW.plusSeconds(1));
        lock.holdElsewhere(ScheduledJob.DORMANT_AUTHORITY_REVOCATION);

        DormancyRun run = job.deactivateDormantUsers();

        assertThat(run.skipped()).isFalse();
        assertThat(run.processed()).hasSize(1);
    }

    // ---- the run log ----------------------------------------------------------------------

    /**
     * Every run reports itself — a run that changed Users and a skipped one alike — so "nobody
     * was dormant" and "the job never ran" can be told apart from the log stream.
     */
    @Test
    void everyRunIsLoggedWithItsWindowOutcomeAndCount() {
        users.given(authenticatedAt("ada", CREATED));
        clock.advanceBy(WINDOW.plusSeconds(1));

        try (CapturedLog captured = CapturedLog.attach()) {
            job.deactivateDormantUsers();
            lock.holdElsewhere(ScheduledJob.INACTIVITY_DEACTIVATION);
            job.deactivateDormantUsers();

            List<ILoggingEvent> runs = captured.withAction(
                    Level.INFO, LogEvent.ACTION, InactivityDeactivationService.ACTION);
            assertThat(runs).hasSize(2);
            assertThat(CapturedLog.fields(runs.get(0)))
                    .containsEntry(LogEvent.OUTCOME, LogEvent.SUCCESS)
                    .containsEntry(LogEvent.DORMANCY_WINDOW, WINDOW.toString())
                    .containsEntry(LogEvent.DORMANCY_SKIPPED, false)
                    .containsEntry(LogEvent.DORMANCY_PROCESSED, 1);
            assertThat(CapturedLog.fields(runs.get(1)))
                    .containsEntry(LogEvent.DORMANCY_SKIPPED, true)
                    .containsEntry(LogEvent.DORMANCY_PROCESSED, 0);
        }
    }

    // ---- reactivation ---------------------------------------------------------------------

    /**
     * Only a stored inactive-to-active transition resets the window. Reactivated, the User is not
     * dormant at the next run; left alone for another full window, it is deactivated again.
     */
    @Test
    void anExplicitReactivationResetsTheWindow() {
        ScimUser ada = users.given(authenticatedAt("ada", CREATED));
        clock.advanceBy(WINDOW.plusSeconds(1));
        job.deactivateDormantUsers();

        users.updateActive(ada.id(), true, clock.instant());
        clock.advanceBy(Duration.ofDays(1));
        assertThat(job.deactivateDormantUsers().processed()).isEmpty();
        assertThat(users.require("ada").profile().active()).isTrue();

        clock.advanceBy(WINDOW);
        assertThat(job.deactivateDormantUsers().processed()).containsExactly(ada.id());
    }

    private static ScimUser authenticatedAt(String userName, Instant at) {
        return ScimIdentities.userAuthenticatedAt(userName, at);
    }
}
