package com.example.backend.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.audit.RecordingAuditTrail;
import com.example.backend.audit.RecordingAuditTrail.Recorded;
import com.example.backend.audit.domain.AuditOperation;
import com.example.backend.auth.InMemoryAccountSessions;
import com.example.backend.auth.MutableClock;
import com.example.backend.auth.PendingCommit;
import com.example.backend.authorization.domain.Permission;
import com.example.backend.authorization.domain.RoleMapping;
import com.example.backend.authorization.domain.RoleMapping.GroupAssignment;
import com.example.backend.authorization.domain.RoleMapping.RoleDefinition;
import com.example.backend.observability.EcsLogCapture;
import com.example.backend.observability.ScheduledJobMetrics;
import com.example.backend.scheduling.InMemoryScheduledJobLock;
import com.example.backend.scheduling.domain.ScheduledJob;
import com.example.backend.scim.InMemoryScimGroupRepository;
import com.example.backend.scim.InMemoryScimUserRepository;
import com.example.backend.scim.ScimIdentities;
import com.example.backend.scim.domain.DormancyPolicy;
import com.example.backend.scim.domain.LockCause;
import com.example.backend.scim.domain.ReservedResourceName;
import com.example.backend.scim.domain.ScimGroup;
import com.example.backend.scim.domain.ScimGroupMember;
import com.example.backend.scim.domain.ScimGroupMembership;
import com.example.backend.scim.domain.ScimLoginState;
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
 * The dormancy job against the in-memory directory: whom it locks, whose mapped Group memberships
 * it removes, what it leaves in place, and what each change records. The same rules against
 * Postgres, with the real seeded Bootstrap Admin, are {@code DormancyIntegrationTests}.
 */
class DormancyServiceTests {

    private static final Instant CREATED = ScimIdentities.NOW;

    private static final Duration LOCKOUT = DormancyPolicy.DEFAULT_LOCKOUT_WINDOW;

    private static final Duration ROLE_REVOCATION = DormancyPolicy.DEFAULT_ROLE_REVOCATION_WINDOW;

    /** Sorts before {@link #SUPERUSER_GROUP}, so the Roles lost read in a fixed order. */
    private static final UUID ACCOUNT_ADMINS =
            UUID.fromString("00000000-0000-4000-8000-00000000a000");

    private static final UUID SUPERUSER_GROUP =
            UUID.fromString("00000000-0000-4000-8000-00000000a001");

    private static final RoleMapping MAPPING = RoleMapping.of(
            List.of(
                    new RoleDefinition("Superuser",
                            Arrays.stream(Permission.values()).map(Permission::value).toList()),
                    new RoleDefinition("Account admin", List.of("user:read", "user:write"))),
            List.of(
                    new GroupAssignment(SUPERUSER_GROUP, "Superuser", true),
                    new GroupAssignment(ACCOUNT_ADMINS, "Account admin", false)));

    private final InMemoryScimUserRepository users = new InMemoryScimUserRepository();
    private final InMemoryScimGroupRepository groups = new InMemoryScimGroupRepository(users);
    private final InMemoryAccountSessions accountSessions = new InMemoryAccountSessions();
    private final PendingCommit transaction = new PendingCommit();
    private final RecordingAuditTrail audit = new RecordingAuditTrail();
    private final InMemoryScheduledJobLock lock = new InMemoryScheduledJobLock();
    private final MutableClock clock = new MutableClock(CREATED);

    private DormancyService job;

    private ScimUser bootstrap;

    @BeforeEach
    void setUp() {
        job = job(DormancyPolicy.defaults());
        bootstrap = users.createReserved(
                ScimIdentities.user("root"), ReservedResourceName.BOOTSTRAP_ADMIN);
    }

    // ---- the lockout step ------------------------------------------------------------------

    /**
     * Past the lockout window an unlocked User is locked for DORMANCY: the lock and its cause are
     * set, the failure run, {@code active} and the version are untouched, an actorless event names
     * the User, and its sessions end only once the run commits.
     */
    @Test
    void aUserPastTheLockoutWindowIsLockedForDormancy() {
        ScimUser ada = users.given(ScimIdentities.userAuthenticatedAt("ada", CREATED));
        accountSessions.open(ada.id(), "ada-session");
        clock.advanceBy(LOCKOUT.plusSeconds(1));

        DormancyRun run = job.run();

        assertThat(run.skipped()).isFalse();
        assertThat(run.locked()).containsExactly(ada.id());
        assertThat(run.rolesRevoked()).isEmpty();
        ScimUser after = users.require("ada");
        assertThat(after.login().lockedAt()).isEqualTo(clock.instant());
        assertThat(after.login().lockCause()).isEqualTo(LockCause.DORMANCY);
        assertThat(after.login().failedLoginAttempts()).isZero();
        assertThat(after.profile().active()).as("active is the directory's").isTrue();
        assertThat(after.version()).isEqualTo(ada.version());
        assertThat(audit.recorded()).containsExactly(
                new Recorded(AuditOperation.DORMANCY_LOCKOUT, null, ada.id(), null));
        assertThat(accountSessions.sessionsOf(ada.id()))
                .as("nothing is revoked before the commit").containsExactly("ada-session");

        transaction.commit();

        assertThat(accountSessions.sessionsOf(ada.id())).isEmpty();
        assertThat(audit.of(AuditOperation.USER_SESSIONS_REVOKE)).containsExactly(
                new Recorded(AuditOperation.USER_SESSIONS_REVOKE, null, ada.id(), "SUCCESS:"));
    }

    @Test
    void theLockoutWindowIsExclusiveAtItsBoundary() {
        ScimUser ada = users.given(ScimIdentities.userAuthenticatedAt("ada", CREATED));

        clock.advanceBy(LOCKOUT);
        assertThat(job.run().locked()).isEmpty();
        assertThat(users.require("ada").login().isLocked()).isFalse();

        clock.advanceBy(Duration.ofNanos(1));
        assertThat(job.run().locked()).containsExactly(ada.id());
    }

    /** A User never authenticated is measured from its creation. */
    @Test
    void aUserThatNeverAuthenticatedIsMeasuredFromCreation() {
        ScimUser idle = users.given(ScimIdentities.user("idle"));
        clock.advanceBy(LOCKOUT.plusSeconds(1));

        assertThat(job.run().locked()).containsExactly(idle.id());
    }

    /** An already locked User keeps its lock and the cause it was first locked for. */
    @Test
    void aUserLockedForFailuresKeepsItsCause() {
        Instant lockedAt = CREATED.plusSeconds(60);
        users.given(ScimIdentities.userWithLoginState(
                "ada", new ScimLoginState("hash", 5, lockedAt, CREATED)));
        clock.advanceBy(LOCKOUT.plusDays(1));

        DormancyRun run = job.run();

        assertThat(run.locked()).isEmpty();
        ScimLoginState login = users.require("ada").login();
        assertThat(login.lockCause()).isEqualTo(LockCause.FAILURES);
        assertThat(login.lockedAt()).isEqualTo(lockedAt);
        assertThat(login.failedLoginAttempts()).isEqualTo(5);
        assertThat(audit.recorded()).isEmpty();
        assertThat(transaction.pending()).isZero();
    }

    /**
     * A candidate locked for failures after the candidate query is not locked again: the lock write
     * itself refuses a User already locked, and nothing is recorded, logged or revoked for it.
     */
    @Test
    void aCandidateLockedSinceTheQueryIsNotLockedAgain() {
        Instant lockedAt = CREATED.plusSeconds(60);
        ScimUser ada = users.given(ScimIdentities.userWithLoginState(
                "ada", new ScimLoginState("hash", 5, lockedAt, CREATED)));
        clock.advanceBy(LOCKOUT.plusDays(1));
        users.answerDormancyCandidatesWith(List.of(ada.id()));

        DormancyRun run = job.run();

        assertThat(run.locked()).isEmpty();
        ScimLoginState login = users.require("ada").login();
        assertThat(login.lockCause()).isEqualTo(LockCause.FAILURES);
        assertThat(login.lockedAt()).isEqualTo(lockedAt);
        assertThat(audit.recorded()).isEmpty();
        assertThat(transaction.pending()).isZero();
    }

    /** The lock is the application's own: an inactive dormant User is locked as well. */
    @Test
    void anInactiveDormantUserIsLockedAndStaysInactive() {
        ScimUser gone = users.given(ScimIdentities.inactiveUser("gone"));
        clock.advanceBy(LOCKOUT.plusSeconds(1));

        assertThat(job.run().locked()).containsExactly(gone.id());
        ScimUser after = users.require("gone");
        assertThat(after.profile().active()).isFalse();
        assertThat(after.login().lockCause()).isEqualTo(LockCause.DORMANCY);
    }

    /** Past the lockout window but not the role-revocation window: locked, Roles kept. */
    @Test
    void aUserLockedButNotYetPastTheRoleRevocationWindowKeepsItsRoles() {
        ScimUser ada = users.given(ScimIdentities.userAuthenticatedAt("ada", CREATED));
        ScimGroup superusers = superuserGroupOf(bootstrap, ada);
        clock.advanceBy(LOCKOUT.plusDays(1));

        DormancyRun run = job.run();

        assertThat(run.locked()).containsExactly(ada.id());
        assertThat(run.rolesRevoked()).isEmpty();
        assertThat(groups.findById(superusers.id()).orElseThrow().hasMember(ada.id())).isTrue();
        assertThat(groups.lockedReads()).as("no Group is locked with nothing to revoke").isEmpty();
    }

    // ---- the role-revocation step ----------------------------------------------------------

    /**
     * The demo oracle: past 180 days a User loses its direct membership of every mapped Group,
     * keeps its unmapped one, and is locked. Each mapped Group's version and the User's advance; an
     * unmapped Group's does not. One actorless event names the Roles lost, and the sessions end
     * after the commit.
     */
    @Test
    void aUserPastTheRoleRevocationWindowLosesEveryMappedMembershipAndOnlyThose() {
        ScimUser ada = users.given(ScimIdentities.userAuthenticatedAt("ada", CREATED));
        ScimGroup superusers = superuserGroupOf(bootstrap, ada);
        ScimGroup accountAdmins = groups.create(
                ScimGroup.created(ACCOUNT_ADMINS, "Account admins", members(ada), CREATED));
        ScimGroup engineering = groups.create(ScimIdentities.group("Engineering", ada));
        long adaVersion = users.require("ada").version();
        accountSessions.open(ada.id(), "ada-session");
        clock.advanceBy(ROLE_REVOCATION.plusSeconds(1));

        DormancyRun run = job.run();

        assertThat(run.rolesRevoked()).containsExactly(ada.id());
        assertThat(run.locked()).containsExactly(ada.id());
        ScimGroup superusersAfter = groups.findById(superusers.id()).orElseThrow();
        assertThat(superusersAfter.hasMember(ada.id())).isFalse();
        assertThat(superusersAfter.hasMember(bootstrap.id())).isTrue();
        assertThat(superusersAfter.version()).isEqualTo(superusers.version() + 1);
        ScimGroup accountAdminsAfter = groups.findById(accountAdmins.id()).orElseThrow();
        assertThat(accountAdminsAfter.hasMember(ada.id())).isFalse();
        assertThat(accountAdminsAfter.version()).isEqualTo(accountAdmins.version() + 1);
        ScimGroup engineeringAfter = groups.findById(engineering.id()).orElseThrow();
        assertThat(engineeringAfter.hasMember(ada.id())).as("unmapped: untouched").isTrue();
        assertThat(engineeringAfter.version()).isEqualTo(engineering.version());
        assertThat(users.require("ada").version()).isEqualTo(adaVersion + 2);
        assertThat(audit.recorded()).containsExactly(
                new Recorded(AuditOperation.DORMANCY_ROLE_REVOCATION, null, ada.id(),
                        "Account admin,Superuser"),
                new Recorded(AuditOperation.DORMANCY_LOCKOUT, null, ada.id(), null));
        assertThat(accountSessions.sessionsOf(ada.id())).containsExactly("ada-session");

        transaction.commit();

        assertThat(accountSessions.sessionsOf(ada.id())).isEmpty();
        assertThat(audit.of(AuditOperation.USER_SESSIONS_REVOKE)).containsExactly(
                new Recorded(AuditOperation.USER_SESSIONS_REVOKE, null, ada.id(), "SUCCESS:GROUPS"),
                new Recorded(AuditOperation.USER_SESSIONS_REVOKE, null, ada.id(), "SUCCESS:"));
    }

    @Test
    void theRoleRevocationWindowIsExclusiveAtItsBoundary() {
        ScimUser ada = users.given(ScimIdentities.userAuthenticatedAt("ada", CREATED));
        superuserGroupOf(bootstrap, ada);

        clock.advanceBy(ROLE_REVOCATION);
        assertThat(job.run().rolesRevoked()).isEmpty();

        clock.advanceBy(Duration.ofNanos(1));
        assertThat(job.run().rolesRevoked()).containsExactly(ada.id());
    }

    /**
     * The job takes priority over the directory: a membership a connector re-adds while the User
     * stays dormant is removed again by the next run, though the User is already locked.
     */
    @Test
    void aReAddedMembershipIsRemovedAgainWhileTheUserStaysDormant() {
        ScimUser ada = users.given(ScimIdentities.userAuthenticatedAt("ada", CREATED));
        ScimGroup superusers = superuserGroupOf(bootstrap, ada);
        clock.advanceBy(ROLE_REVOCATION.plusSeconds(1));
        job.run();
        ScimGroup afterFirst = groups.findById(superusers.id()).orElseThrow();
        groups.replace(new ScimGroup(
                afterFirst.id(),
                afterFirst.displayName(),
                members(bootstrap, ada),
                afterFirst.reservedName(),
                afterFirst.version(),
                afterFirst.createdAt(),
                clock.instant()));
        assertThat(groups.findById(superusers.id()).orElseThrow().hasMember(ada.id()))
                .as("the connector re-added it").isTrue();
        audit.reset();

        clock.advanceBy(Duration.ofDays(1));
        DormancyRun second = job.run();

        assertThat(second.rolesRevoked()).containsExactly(ada.id());
        assertThat(second.locked()).as("already locked").isEmpty();
        assertThat(groups.findById(superusers.id()).orElseThrow().hasMember(ada.id())).isFalse();
        assertThat(audit.recorded()).containsExactly(new Recorded(
                AuditOperation.DORMANCY_ROLE_REVOCATION, null, ada.id(), "Superuser"));
    }

    /** Roles are revoked from a dormant User whether or not it is active or already locked. */
    @Test
    void aLockedInactiveUserStillLosesItsRoles() {
        ScimUser gone = users.given(ScimIdentities.userWithLoginState(
                "gone", new ScimLoginState("hash", 5, CREATED, CREATED)));
        superuserGroupOf(bootstrap, gone);
        clock.advanceBy(ROLE_REVOCATION.plusSeconds(1));

        DormancyRun run = job.run();

        assertThat(run.rolesRevoked()).containsExactly(gone.id());
        assertThat(run.locked()).isEmpty();
        assertThat(users.require("gone").login().lockCause()).isEqualTo(LockCause.FAILURES);
    }

    /** A User in no mapped Group has no Role to lose: no event, no Group touched. */
    @Test
    void aUserInOnlyUnmappedGroupsHasNothingRevoked() {
        ScimUser ada = users.given(ScimIdentities.userAuthenticatedAt("ada", CREATED));
        ScimGroup engineering = groups.create(ScimIdentities.group("Engineering", ada));
        clock.advanceBy(ROLE_REVOCATION.plusSeconds(1));

        DormancyRun run = job.run();

        assertThat(run.rolesRevoked()).isEmpty();
        assertThat(groups.findById(engineering.id()).orElseThrow().version())
                .isEqualTo(engineering.version());
        assertThat(audit.of(AuditOperation.DORMANCY_ROLE_REVOCATION)).isEmpty();
    }

    /**
     * Every affected Group is locked, in id order, before any membership moves — the point a
     * connector's conditional write serializes on.
     */
    @Test
    void theAffectedGroupsAreLockedInIdOrder() {
        ScimUser ada = users.given(ScimIdentities.userAuthenticatedAt("ada", CREATED));
        ScimUser bob = users.given(ScimIdentities.userAuthenticatedAt("bob", CREATED));
        superuserGroupOf(bootstrap, ada);
        groups.create(ScimGroup.created(ACCOUNT_ADMINS, "Account admins", members(bob), CREATED));
        clock.advanceBy(ROLE_REVOCATION.plusSeconds(1));

        job.run();

        assertThat(groups.lockedReads()).containsExactly(ACCOUNT_ADMINS, SUPERUSER_GROUP);
    }

    // ---- what it never touches -------------------------------------------------------------

    /** The Bootstrap Admin is exempt from both steps, however long it has been dormant. */
    @Test
    void theBootstrapAdminIsExemptFromBothSteps() {
        ScimGroup superusers = superuserGroupOf(bootstrap);
        accountSessions.open(bootstrap.id(), "root-session");
        clock.advanceBy(ROLE_REVOCATION.multipliedBy(10));

        DormancyRun run = job.run();
        transaction.commit();

        assertThat(run.locked()).isEmpty();
        assertThat(run.rolesRevoked()).isEmpty();
        assertThat(users.findById(bootstrap.id()).orElseThrow().login().isLocked()).isFalse();
        assertThat(groups.findById(superusers.id()).orElseThrow().hasMember(bootstrap.id()))
                .isTrue();
        assertThat(accountSessions.sessionsOf(bootstrap.id())).containsExactly("root-session");
        assertThat(audit.recorded()).isEmpty();
    }

    /**
     * The job decides again on the locked read, so a stale candidate list cannot make it act: not
     * on the Bootstrap Admin, not on a User that authenticated since, not on a membership that no
     * longer exists, and not on an id naming nobody.
     */
    @Test
    void aStaleCandidateIsDecidedAgainOnTheLockedRead() {
        ScimUser outsider = users.given(ScimIdentities.userAuthenticatedAt("outsider", CREATED));
        clock.advanceBy(ROLE_REVOCATION.multipliedBy(2));
        ScimUser fresh = users.given(
                ScimIdentities.userAuthenticatedAt("fresh", clock.instant()));
        ScimGroup superusers = superuserGroupOf(bootstrap, fresh);
        UUID nobody = UUID.randomUUID();
        users.answerDormancyCandidatesWith(List.of(bootstrap.id(), fresh.id(), nobody));
        groups.answerDormantMemberCandidatesWith(List.of(
                new ScimGroupMembership(bootstrap.id(), SUPERUSER_GROUP),
                new ScimGroupMembership(fresh.id(), SUPERUSER_GROUP),
                new ScimGroupMembership(outsider.id(), SUPERUSER_GROUP),
                new ScimGroupMembership(outsider.id(), UUID.randomUUID()),
                new ScimGroupMembership(nobody, SUPERUSER_GROUP)));

        DormancyRun run = job.run();

        assertThat(run.locked()).isEmpty();
        assertThat(run.rolesRevoked()).isEmpty();
        ScimGroup after = groups.findById(superusers.id()).orElseThrow();
        assertThat(after.hasMember(bootstrap.id())).isTrue();
        assertThat(after.hasMember(fresh.id())).isTrue();
        assertThat(after.version()).isEqualTo(superusers.version());
        assertThat(users.require("fresh").login().isLocked()).isFalse();
        assertThat(audit.recorded()).isEmpty();
        assertThat(transaction.pending()).isZero();
    }

    @Test
    void configuredWindowsAreTheOnesApplied() {
        job = job(new DormancyPolicy(Duration.ofDays(5), Duration.ofDays(10)));
        ScimUser ada = users.given(ScimIdentities.userAuthenticatedAt("ada", CREATED));
        superuserGroupOf(bootstrap, ada);

        clock.advanceBy(Duration.ofDays(6));
        DormancyRun first = job.run();
        assertThat(first.locked()).containsExactly(ada.id());
        assertThat(first.rolesRevoked()).isEmpty();

        clock.advanceBy(Duration.ofDays(5));
        assertThat(job.run().rolesRevoked()).containsExactly(ada.id());
    }

    // ---- serialization ---------------------------------------------------------------------

    @Test
    void aRunSkipsWhileAnotherRunHoldsTheJobsLock() {
        users.given(ScimIdentities.userAuthenticatedAt("ada", CREATED));
        clock.advanceBy(ROLE_REVOCATION.plusSeconds(1));
        lock.holdElsewhere(ScheduledJob.DORMANCY);

        DormancyRun run = job.run();

        assertThat(run.skipped()).isTrue();
        assertThat(run.locked()).isEmpty();
        assertThat(run.rolesRevoked()).isEmpty();
        assertThat(users.require("ada").login().isLocked()).isFalse();
        assertThat(audit.recorded()).isEmpty();
        assertThat(lock.attempts()).containsExactly(ScheduledJob.DORMANCY);
    }

    @Test
    void anotherJobHoldingItsLockDoesNotStopThisOne() {
        ScimUser ada = users.given(ScimIdentities.userAuthenticatedAt("ada", CREATED));
        clock.advanceBy(LOCKOUT.plusSeconds(1));
        lock.holdElsewhere(ScheduledJob.AUDIT_RETENTION);

        assertThat(job.run().locked()).containsExactly(ada.id());
    }

    /** A second run finds nothing left to do: no write, no event. */
    @Test
    void aSecondRunDoesNotProcessTheSameUserAgain() {
        ScimUser ada = users.given(ScimIdentities.userAuthenticatedAt("ada", CREATED));
        superuserGroupOf(bootstrap, ada);
        clock.advanceBy(ROLE_REVOCATION.plusSeconds(1));
        job.run();
        int writes = users.writes();
        audit.reset();

        DormancyRun second = job.run();

        assertThat(second.locked()).isEmpty();
        assertThat(second.rolesRevoked()).isEmpty();
        assertThat(users.writes()).isEqualTo(writes);
        assertThat(audit.recorded()).isEmpty();
    }

    // ---- logging ---------------------------------------------------------------------------

    /**
     * Inside a run the only records are one per User changed: a lockout at WARN, a role revocation
     * at INFO naming the Roles lost, both classified user-administration / change / success.
     */
    @Test
    void eachChangeIsLoggedOnceAtItsLevel() {
        ScimUser ada = users.given(ScimIdentities.userAuthenticatedAt("ada", CREATED));
        superuserGroupOf(bootstrap, ada);
        clock.advanceBy(ROLE_REVOCATION.plusSeconds(1));

        try (EcsLogCapture ecs = EcsLogCapture.attach(new StandardEnvironment())) {
            job.run();
            List<JsonNode> records = onThisThread(ecs);

            assertThat(records).extracting(record -> record.at("/app/event/action").asText())
                    .containsExactly(
                            "identity.dormancy_role_revocation", "identity.dormancy_lockout");
            JsonNode revocation = records.get(0);
            assertThat(revocation.at("/log/level").asText()).isEqualTo("INFO");
            assertThat(revocation.at("/app/authorization/role").asText()).isEqualTo("Superuser");
            JsonNode lockout = records.get(1);
            assertThat(lockout.at("/log/level").asText())
                    .as("inactivity, not an attack: never ERROR").isEqualTo("WARN");
            assertThat(records).allSatisfy(record -> {
                assertThat(record.at("/event/action").asText()).isEqualTo("user-administration");
                assertThat(record.at("/event/type")).hasSize(1);
                assertThat(record.at("/event/type").get(0).asText()).isEqualTo("change");
                assertThat(record.at("/event/outcome").asText()).isEqualTo("success");
                assertThat(record.at("/user/target/id").asText()).isEqualTo(ada.id().toString());
                assertThat(record.toString()).doesNotContain("\"ada\"");
            });
        }
    }

    /**
     * The revocation's log names every Role lost sorted and distinct, whatever order the Groups
     * were removed in: here the Superuser Group's membership is removed first.
     */
    @Test
    void theRevocationLogNamesTheRolesLostSorted() {
        ScimUser ada = users.given(ScimIdentities.userAuthenticatedAt("ada", CREATED));
        superuserGroupOf(bootstrap, ada);
        groups.create(ScimGroup.created(ACCOUNT_ADMINS, "Account admins", members(ada), CREATED));
        clock.advanceBy(ROLE_REVOCATION.plusSeconds(1));
        groups.answerDormantMemberCandidatesWith(List.of(
                new ScimGroupMembership(ada.id(), SUPERUSER_GROUP),
                new ScimGroupMembership(ada.id(), ACCOUNT_ADMINS)));

        try (EcsLogCapture ecs = EcsLogCapture.attach(new StandardEnvironment())) {
            job.run();
            JsonNode revocation = onThisThread(ecs).get(0);

            assertThat(revocation.at("/app/event/action").asText())
                    .isEqualTo("identity.dormancy_role_revocation");
            assertThat(revocation.at("/app/authorization/role").asText())
                    .isEqualTo("Account admin,Superuser");
        }
    }

    /**
     * Run as it is scheduled, a run is {@code job-start}, its per-User records and a
     * {@code job-end} carrying the counts, all under one {@code batch.job.run.id} and the job's own
     * action.
     */
    @Test
    void aScheduledRunEndsWithItsCountsUnderOneRunId() {
        ScimUser ada = users.given(ScimIdentities.userAuthenticatedAt("ada", CREATED));
        superuserGroupOf(bootstrap, ada);
        users.given(ScimIdentities.userAuthenticatedAt("bob", CREATED));
        clock.advanceBy(ROLE_REVOCATION.plusSeconds(1));

        try (EcsLogCapture ecs = EcsLogCapture.attach(new StandardEnvironment())) {
            scheduled().run();
            List<JsonNode> records = onThisThread(ecs);

            assertThat(records).extracting(record -> record.at("/message").asText()).containsExactly(
                    "Scheduled job started", "Roles revoked for dormancy",
                    "User locked for dormancy", "User locked for dormancy",
                    "Scheduled job completed");
            String runId = records.getFirst().at("/batch/job/run/id").asText();
            assertThat(runId).isNotBlank();
            assertThat(records).allSatisfy(record -> {
                assertThat(record.at("/batch/job/run/id").asText()).isEqualTo(runId);
                assertThat(record.at("/batch/job/name").asText()).isEqualTo("dormancy");
                assertThat(record.at("/trigger/type").asText()).isEqualTo("scheduled");
            });
            JsonNode start = records.getFirst();
            assertThat(start.at("/app/event/action").asText()).isEqualTo("identity.dormancy");
            JsonNode end = records.getLast();
            assertThat(end.at("/app/event/action").asText()).isEqualTo("identity.dormancy");
            assertThat(end.at("/event/type").get(0).asText()).isEqualTo("job-end");
            assertThat(end.at("/event/duration_ms").isNumber()).isTrue();
            assertThat(end.at("/dormancy/locked_count").asInt()).isEqualTo(2);
            assertThat(end.at("/dormancy/roles_revoked_count").asInt()).isEqualTo(1);
        }
    }

    /** A run that finds the lock held ends in the one {@code lock-held} record, with no counts. */
    @Test
    void aScheduledRunThatFindsTheLockHeldLogsTheSkipAndDoesNoWork() {
        users.given(ScimIdentities.userAuthenticatedAt("ada", CREATED));
        clock.advanceBy(LOCKOUT.plusSeconds(1));
        lock.holdElsewhere(ScheduledJob.DORMANCY);

        try (EcsLogCapture ecs = EcsLogCapture.attach(new StandardEnvironment())) {
            scheduled().run();

            List<JsonNode> records = onThisThread(ecs);
            assertThat(records).extracting(record -> record.at("/message").asText()).containsExactly(
                    "Scheduled job started", "Scheduled job skipped: another run holds its lock");
            assertThat(records.get(1).at("/event/reason").asText()).isEqualTo("lock-held");
            assertThat(records.get(1).at("/dormancy").isMissingNode()).isTrue();
        }
        assertThat(audit.recorded()).isEmpty();
    }

    private DormancyService job(DormancyPolicy policy) {
        return new DormancyService(
                users,
                groups,
                new ScimUserSessionRevocationService(accountSessions, transaction, audit),
                lock,
                policy,
                MAPPING,
                audit,
                clock);
    }

    private Runnable scheduled() {
        return new ScheduledJobMetrics(new SimpleMeterRegistry(), ObservationRegistry.NOOP, clock)
                .instrumentLocked("dormancy", DormancyService.OPERATION, job::run);
    }

    private static List<JsonNode> onThisThread(EcsLogCapture ecs) {
        String thread = Thread.currentThread().getName();
        return ecs.records().stream()
                .filter(record -> thread.equals(record.at("/process/thread/name").asText()))
                .toList();
    }

    private ScimGroup superuserGroupOf(ScimUser... members) {
        return groups.createReserved(
                ScimGroup.created(SUPERUSER_GROUP, "Admins", members(members), CREATED),
                ReservedResourceName.ADMIN_GROUP);
    }

    private static List<ScimGroupMember> members(ScimUser... users) {
        return Arrays.stream(users).map(user -> ScimGroupMember.reference(user.id())).toList();
    }
}
