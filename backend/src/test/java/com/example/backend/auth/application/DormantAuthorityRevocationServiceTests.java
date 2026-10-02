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
import com.example.backend.observability.EcsLogCapture;
import com.example.backend.observability.LogEvent;
import com.example.backend.observability.ScheduledJobMetrics;
import com.example.backend.scim.InMemoryScimGroupRepository;
import com.example.backend.scim.InMemoryScimUserRepository;
import com.example.backend.scim.ScimIdentities;
import com.example.backend.scim.domain.DormancyPolicy;
import com.example.backend.scim.domain.ReservedResourceName;
import com.example.backend.scim.domain.ScimGroup;
import com.example.backend.scim.domain.ScimGroupMember;
import com.example.backend.scim.domain.ScimUser;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import tools.jackson.databind.JsonNode;

/**
 * The dormant-authority job against the in-memory directory: whose Admin-group membership it
 * removes, what it leaves in place, and what each removal does. The same rules against Postgres,
 * with real concurrency and the real seeded Bootstrap Admin, are
 * {@code InactivityGovernanceIntegrationTests}.
 */
class DormantAuthorityRevocationServiceTests {

    private static final Instant CREATED = ScimIdentities.NOW;

    private static final Duration WINDOW = DormancyPolicy.DEFAULT_AUTHORITY_REVOCATION_WINDOW;

    private final InMemoryScimUserRepository users = new InMemoryScimUserRepository();
    private final InMemoryScimGroupRepository groups = new InMemoryScimGroupRepository(users);
    private final InMemoryAccountSessions accountSessions = new InMemoryAccountSessions();
    private final PendingCommit transaction = new PendingCommit();
    private final RecordingAuditTrail audit = new RecordingAuditTrail();
    private final InMemoryScheduledJobLock lock = new InMemoryScheduledJobLock();
    private final MutableClock clock = new MutableClock(CREATED);

    private DormantAuthorityRevocationService job;

    private ScimUser bootstrap;

    @BeforeEach
    void setUp() {
        job = new DormantAuthorityRevocationService(
                users,
                groups,
                new ScimUserSessionRevocation(accountSessions, transaction, audit),
                lock,
                DormancyPolicy.defaults(),
                audit,
                clock);
        bootstrap = users.createReserved(
                ScimIdentities.user("root"), ReservedResourceName.BOOTSTRAP_ADMIN);
    }

    // ---- the demo oracle ------------------------------------------------------------------

    /**
     * A dormant Admin past 180 days loses its Admin-group membership and nothing else: it stays
     * active (baseline access), stays in its ordinary Group, and that Group's version does not
     * move. The Admin group's and the User's versions advance once, an actorless event names the
     * User, and its sessions end after the commit.
     */
    @Test
    void aDormantAdminLosesOnlyItsAdminMembership() {
        ScimUser ada = users.given(ScimIdentities.userAuthenticatedAt("ada", CREATED));
        ScimGroup admins = adminGroupOf(bootstrap, ada);
        ScimGroup ordinary = groups.create(ScimIdentities.group("Engineering", ada));
        ScimUser adaBefore = users.require("ada");
        accountSessions.open(ada.id(), "ada-session");
        clock.advanceBy(WINDOW.plusSeconds(1));

        DormancyRun run = job.revokeDormantAuthority();

        assertThat(run.skipped()).isFalse();
        assertThat(run.processed()).containsExactly(ada.id());
        ScimGroup adminsAfter = groups.findById(admins.id()).orElseThrow();
        assertThat(adminsAfter.hasMember(ada.id())).isFalse();
        assertThat(adminsAfter.hasMember(bootstrap.id())).isTrue();
        assertThat(adminsAfter.version()).isEqualTo(admins.version() + 1);
        ScimGroup ordinaryAfter = groups.findById(ordinary.id()).orElseThrow();
        assertThat(ordinaryAfter.hasMember(ada.id())).isTrue();
        assertThat(ordinaryAfter.version()).isEqualTo(ordinary.version());
        ScimUser adaAfter = users.require("ada");
        assertThat(adaAfter.profile().active()).as("baseline access is untouched").isTrue();
        assertThat(adaAfter.version()).isEqualTo(adaBefore.version() + 1);
        assertThat(audit.of(AuditOperation.DORMANT_AUTHORITY_REVOCATION))
                .containsExactly(new Recorded(
                        AuditOperation.DORMANT_AUTHORITY_REVOCATION, null, ada.id(), null));
        assertThat(accountSessions.sessionsOf(ada.id()))
                .as("nothing is revoked before the commit").containsExactly("ada-session");

        transaction.commit();

        assertThat(accountSessions.sessionsOf(ada.id())).isEmpty();
        assertThat(audit.of(AuditOperation.USER_SESSIONS_REVOKE))
                .containsExactly(new Recorded(
                        AuditOperation.USER_SESSIONS_REVOKE, null, ada.id(), "SUCCESS:GROUPS"));
    }

    /**
     * The job takes priority: a membership re-added while the User stays dormant is removed again
     * on the next run.
     */
    @Test
    void aReAddedMembershipIsRemovedAgainWhileTheUserStaysDormant() {
        ScimUser ada = users.given(ScimIdentities.userAuthenticatedAt("ada", CREATED));
        ScimGroup admins = adminGroupOf(bootstrap, ada);
        clock.advanceBy(WINDOW.plusSeconds(1));
        job.revokeDormantAuthority();
        ScimGroup afterFirst = groups.findById(admins.id()).orElseThrow();
        groups.replace(new ScimGroup(
                afterFirst.id(),
                afterFirst.displayName(),
                members(bootstrap, ada),
                afterFirst.reservedName(),
                afterFirst.version(),
                afterFirst.createdAt(),
                clock.instant()));
        assertThat(groups.isMemberOfReservedGroup(ada.id(), ReservedResourceName.ADMIN_GROUP))
                .as("the connector re-added it").isTrue();

        clock.advanceBy(Duration.ofDays(1));
        DormancyRun second = job.revokeDormantAuthority();

        assertThat(second.processed()).containsExactly(ada.id());
        assertThat(groups.isMemberOfReservedGroup(ada.id(), ReservedResourceName.ADMIN_GROUP))
                .isFalse();
        assertThat(audit.of(AuditOperation.DORMANT_AUTHORITY_REVOCATION)).hasSize(2);
    }

    // ---- the window -----------------------------------------------------------------------

    @Test
    void theWindowIsExclusiveAtItsBoundary() {
        ScimUser ada = users.given(ScimIdentities.userAuthenticatedAt("ada", CREATED));
        adminGroupOf(bootstrap, ada);

        clock.advanceBy(WINDOW);
        assertThat(job.revokeDormantAuthority().processed()).isEmpty();

        clock.advanceBy(Duration.ofNanos(1));
        assertThat(job.revokeDormantAuthority().processed()).containsExactly(ada.id());
    }

    /** Past the deactivation window but not this one: authority stays. */
    @Test
    void aUserDormantOnlyPastTheDeactivationWindowKeepsItsAuthority() {
        ScimUser ada = users.given(ScimIdentities.userAuthenticatedAt("ada", CREATED));
        adminGroupOf(bootstrap, ada);
        clock.advanceBy(DormancyPolicy.DEFAULT_DEACTIVATION_WINDOW.plusDays(1));

        assertThat(job.revokeDormantAuthority().processed()).isEmpty();
        assertThat(groups.isMemberOfReservedGroup(ada.id(), ReservedResourceName.ADMIN_GROUP))
                .isTrue();
    }

    /** Authority is revoked from a dormant User whether or not it is still active. */
    @Test
    void anInactiveDormantAdminLosesItsAuthorityToo() {
        ScimUser gone = users.given(ScimIdentities.inactiveUser("gone"));
        adminGroupOf(bootstrap, gone);
        clock.advanceBy(WINDOW.plusSeconds(1));

        assertThat(job.revokeDormantAuthority().processed()).containsExactly(gone.id());
    }

    @Test
    void aConfiguredWindowIsTheOneApplied() {
        job = new DormantAuthorityRevocationService(
                users,
                groups,
                new ScimUserSessionRevocation(accountSessions, transaction, audit),
                lock,
                new DormancyPolicy(null, Duration.ofDays(10)),
                audit,
                clock);
        ScimUser ada = users.given(ScimIdentities.userAuthenticatedAt("ada", CREATED));
        adminGroupOf(bootstrap, ada);
        clock.advanceBy(Duration.ofDays(11));

        assertThat(job.revokeDormantAuthority().processed()).containsExactly(ada.id());
    }

    // ---- what it never touches ------------------------------------------------------------

    /** The Bootstrap Admin keeps its Admin-group membership, however long it has been dormant. */
    @Test
    void theBootstrapAdminNeverLosesItsMembership() {
        ScimGroup admins = adminGroupOf(bootstrap);
        accountSessions.open(bootstrap.id(), "root-session");
        clock.advanceBy(WINDOW.multipliedBy(10));

        assertThat(job.revokeDormantAuthority().processed()).isEmpty();
        transaction.commit();

        assertThat(groups.findById(admins.id()).orElseThrow().hasMember(bootstrap.id())).isTrue();
        assertThat(groups.findById(admins.id()).orElseThrow().version())
                .isEqualTo(admins.version());
        assertThat(accountSessions.sessionsOf(bootstrap.id())).containsExactly("root-session");
        assertThat(audit.recorded()).isEmpty();
    }

    /**
     * The job decides again on the locked read, so a stale candidate list cannot make it act: not
     * on the Bootstrap Admin, not on a member that authenticated since, not on a User that is no
     * longer a member, and not on an id naming nobody.
     */
    @Test
    void aStaleCandidateIsDecidedAgainOnTheLockedRead() {
        ScimUser outsider = users.given(ScimIdentities.userAuthenticatedAt("outsider", CREATED));
        clock.advanceBy(WINDOW.multipliedBy(2));
        ScimUser fresh = users.given(
                ScimIdentities.userAuthenticatedAt("fresh", clock.instant()));
        ScimGroup admins = adminGroupOf(bootstrap, fresh);
        groups.answerDormantMemberCandidatesWith(
                List.of(bootstrap.id(), fresh.id(), outsider.id(), UUID.randomUUID()));

        DormancyRun run = job.revokeDormantAuthority();

        assertThat(run.processed()).isEmpty();
        ScimGroup after = groups.findById(admins.id()).orElseThrow();
        assertThat(after.hasMember(bootstrap.id())).isTrue();
        assertThat(after.hasMember(fresh.id())).isTrue();
        assertThat(after.version()).isEqualTo(admins.version());
        assertThat(audit.recorded()).isEmpty();
        assertThat(transaction.pending()).isZero();
    }

    /** Before seeding there is no Admin group and so no authority to revoke — and no failure. */
    @Test
    void withNoAdminGroupThereIsNothingToRevoke() {
        ScimUser ada = users.given(ScimIdentities.userAuthenticatedAt("ada", CREATED));
        groups.answerDormantMemberCandidatesWith(List.of(ada.id()));
        clock.advanceBy(WINDOW.plusSeconds(1));

        DormancyRun run = job.revokeDormantAuthority();

        assertThat(run.skipped()).isFalse();
        assertThat(run.processed()).isEmpty();
        assertThat(audit.recorded()).isEmpty();
    }

    // ---- serialization --------------------------------------------------------------------

    @Test
    void aRunSkipsWhileAnotherRunOfTheSameJobHoldsItsLock() {
        ScimUser ada = users.given(ScimIdentities.userAuthenticatedAt("ada", CREATED));
        adminGroupOf(bootstrap, ada);
        clock.advanceBy(WINDOW.plusSeconds(1));
        lock.holdElsewhere(ScheduledJob.DORMANT_AUTHORITY_REVOCATION);

        DormancyRun run = job.revokeDormantAuthority();

        assertThat(run.skipped()).isTrue();
        assertThat(run.processed()).isEmpty();
        assertThat(groups.isMemberOfReservedGroup(ada.id(), ReservedResourceName.ADMIN_GROUP))
                .isTrue();
        assertThat(audit.recorded()).isEmpty();
        assertThat(lock.attempts()).containsExactly(ScheduledJob.DORMANT_AUTHORITY_REVOCATION);
    }

    @Test
    void theOtherJobHoldingItsLockDoesNotStopThisOne() {
        ScimUser ada = users.given(ScimIdentities.userAuthenticatedAt("ada", CREATED));
        adminGroupOf(bootstrap, ada);
        clock.advanceBy(WINDOW.plusSeconds(1));
        lock.holdElsewhere(ScheduledJob.INACTIVITY_DEACTIVATION);

        assertThat(job.revokeDormantAuthority().processed()).containsExactly(ada.id());
    }

    /** A second run finds nothing left to remove: no version bump, no event. */
    @Test
    void aSecondRunDoesNotProcessTheSameUserAgain() {
        ScimUser ada = users.given(ScimIdentities.userAuthenticatedAt("ada", CREATED));
        ScimGroup admins = adminGroupOf(bootstrap, ada);
        clock.advanceBy(WINDOW.plusSeconds(1));
        job.revokeDormantAuthority();
        long versionAfterFirst = groups.findById(admins.id()).orElseThrow().version();
        audit.reset();

        assertThat(job.revokeDormantAuthority().processed()).isEmpty();
        assertThat(groups.findById(admins.id()).orElseThrow().version())
                .isEqualTo(versionAfterFirst);
        assertThat(audit.recorded()).isEmpty();
    }

    /**
     * The Admin group's row is taken under its resource lock before any membership moves — the
     * point a connector's conditional write to it serializes on — and not taken at all by a run
     * that found nobody dormant.
     */
    @Test
    void theAdminGroupIsLockedOnlyWhenThereIsAMembershipToRemove() {
        ScimUser ada = users.given(ScimIdentities.userAuthenticatedAt("ada", CREATED));
        ScimGroup admins = adminGroupOf(bootstrap, ada);

        job.revokeDormantAuthority();
        assertThat(groups.lockedReads()).as("nobody dormant yet").isEmpty();

        clock.advanceBy(WINDOW.plusSeconds(1));
        job.revokeDormantAuthority();
        assertThat(groups.lockedReads()).containsExactly(admins.id());
    }

    // ---- the run log ----------------------------------------------------------------------

    /**
     * A run that did the work reports itself, an empty one included, with this job's window and
     * count. A skipped run writes no summary: its end is the wrapper's {@code lock-held} record.
     */
    @Test
    void everyRunThatDidTheWorkIsLoggedWithItsWindowAndCount() {
        ScimUser ada = users.given(ScimIdentities.userAuthenticatedAt("ada", CREATED));
        adminGroupOf(bootstrap, ada);
        clock.advanceBy(WINDOW.plusSeconds(1));

        try (CapturedLog captured = CapturedLog.attach()) {
            job.revokeDormantAuthority();
            job.revokeDormantAuthority();
            lock.holdElsewhere(ScheduledJob.DORMANT_AUTHORITY_REVOCATION);
            job.revokeDormantAuthority();

            List<ILoggingEvent> runs = captured.withAction(
                    Level.INFO, LogEvent.LOCAL_ACTION, DormantAuthorityRevocationService.OPERATION.local());
            assertThat(runs).hasSize(2);
            assertThat(CapturedLog.fields(runs.get(0)))
                    .containsEntry(LogEvent.CATEGORY, List.of("batch"))
                    .containsEntry(LogEvent.TYPE, List.of("info"))
                    .containsEntry(LogEvent.DORMANCY_WINDOW, WINDOW.toString())
                    .containsEntry(LogEvent.DORMANCY_PROCESSED, 1)
                    .doesNotContainKey(LogEvent.OUTCOME);
            assertThat(CapturedLog.fields(runs.get(1)))
                    .containsEntry(LogEvent.DORMANCY_PROCESSED, 0);
        }
    }

    /**
     * Run as it is scheduled — through {@link ScheduledJobMetrics#instrumentLocked} — a run is
     * {@code job-start}, this job's summary and {@code job-end}, all under one
     * {@code batch.job.run.id}, and the next run has another.
     */
    @Test
    void aScheduledRunIsJobStartTheSummaryAndJobEndUnderOneRunId() {
        ScimUser ada = users.given(ScimIdentities.userAuthenticatedAt("ada", CREATED));
        adminGroupOf(bootstrap, ada);
        clock.advanceBy(WINDOW.plusSeconds(1));
        Runnable scheduled = scheduled();

        try (EcsLogCapture ecs = EcsLogCapture.attach(new StandardEnvironment())) {
            scheduled.run();
            List<JsonNode> first = onThisThread(ecs);
            ecs.reset();
            scheduled.run();
            List<JsonNode> second = onThisThread(ecs);

            assertThat(first).extracting(record -> record.at("/message").asText()).containsExactly(
                    "Scheduled job started", "Dormant authority revocation run complete",
                    "Scheduled job completed");
            String runId = first.getFirst().at("/batch/job/run/id").asText();
            assertThat(runId).isNotBlank();
            assertThat(first).allSatisfy(record -> {
                assertThat(record.at("/batch/job/run/id").asText()).isEqualTo(runId);
                assertThat(record.at("/app/event/action").asText())
                        .isEqualTo("identity.dormant_authority_revocation");
            });
            assertThat(first.get(1).at("/dormancy/processed").asInt()).isEqualTo(1);
            assertThat(second.getFirst().at("/batch/job/run/id").asText())
                    .isNotBlank().isNotEqualTo(runId);
        }
    }

    /**
     * Scheduled while another run holds the lock, a run ends in the one {@code lock-held}
     * {@code job-end} and does nothing: the membership stays, no event, no summary.
     */
    @Test
    void aScheduledRunThatFindsTheLockHeldLogsTheSkipAndDoesNoWork() {
        ScimUser ada = users.given(ScimIdentities.userAuthenticatedAt("ada", CREATED));
        adminGroupOf(bootstrap, ada);
        clock.advanceBy(WINDOW.plusSeconds(1));
        lock.holdElsewhere(ScheduledJob.DORMANT_AUTHORITY_REVOCATION);

        try (EcsLogCapture ecs = EcsLogCapture.attach(new StandardEnvironment())) {
            scheduled().run();

            List<JsonNode> records = onThisThread(ecs);
            assertThat(records).extracting(record -> record.at("/message").asText()).containsExactly(
                    "Scheduled job started", "Scheduled job skipped: another run holds its lock");
            assertThat(records.get(1).at("/event/reason").asText()).isEqualTo("lock-held");
            assertThat(records.get(1).at("/log/level").asText()).isEqualTo("INFO");
        }
        assertThat(groups.isMemberOfReservedGroup(ada.id(), ReservedResourceName.ADMIN_GROUP))
                .isTrue();
        assertThat(audit.recorded()).isEmpty();
        assertThat(lock.attempts()).containsExactly(ScheduledJob.DORMANT_AUTHORITY_REVOCATION);
    }

    private Runnable scheduled() {
        return new ScheduledJobMetrics(new SimpleMeterRegistry(), ObservationRegistry.NOOP, clock)
                .instrumentLocked("dormant-authority-revocation",
                        DormantAuthorityRevocationService.OPERATION, job::revokeDormantAuthority);
    }

    private static List<JsonNode> onThisThread(EcsLogCapture ecs) {
        String thread = Thread.currentThread().getName();
        return ecs.records().stream()
                .filter(record -> thread.equals(record.at("/process/thread/name").asText()))
                .toList();
    }

    private ScimGroup adminGroupOf(ScimUser... members) {
        return groups.createReserved(
                ScimGroup.created(UUID.randomUUID(), "Admins", members(members), CREATED),
                ReservedResourceName.ADMIN_GROUP);
    }

    private static List<ScimGroupMember> members(ScimUser... users) {
        return Arrays.stream(users).map(user -> ScimGroupMember.reference(user.id())).toList();
    }
}
