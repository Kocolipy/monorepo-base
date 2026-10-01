package com.example.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.ContainerTestConfiguration;
import com.example.backend.InMemorySessionRegistryConfiguration;
import com.example.backend.auth.application.DormancyRun;
import com.example.backend.auth.application.DormantAuthorityRevocationService;
import com.example.backend.auth.application.InactivityDeactivationService;
import com.example.backend.auth.application.LoginService;
import com.example.backend.auth.domain.ScheduledJob;
import com.example.backend.auth.domain.ScheduledJobLock;
import com.example.backend.scim.application.ConnectorAdministrationService;
import com.example.backend.scim.application.NewScimGroup;
import com.example.backend.scim.application.NewScimUser;
import com.example.backend.scim.application.ScimGroupPatchOperation;
import com.example.backend.scim.application.ScimGroupService;
import com.example.backend.scim.application.ScimUserReplacement;
import com.example.backend.scim.application.ScimUserService;
import com.example.backend.scim.domain.AuthenticatedConnector;
import com.example.backend.scim.domain.ConnectorTokenScope;
import com.example.backend.scim.domain.DormancyPolicy;
import com.example.backend.scim.domain.NormalizedUserName;
import com.example.backend.scim.domain.ReservedResourceName;
import com.example.backend.scim.domain.ScimGroupRepository;
import com.example.backend.scim.domain.ScimUser;
import com.example.backend.scim.domain.ScimUserPatchOperation;
import com.example.backend.scim.domain.ScimUserProfile;
import com.example.backend.scim.domain.ScimUserRepository;
import com.example.backend.scim.domain.ScimVersionPrecondition;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Inactivity governance end to end, against a real Postgres, the real seeded identities and the
 * real job locks, under a clock the test moves: Part A's 90-day deactivation, Part B's 180-day
 * Admin-authority revocation, the rule that only an explicit reactivation resets the window, the
 * Bootstrap Admin's exemption, and both kinds of serialization — no double-processing, and each
 * job independent of the other.
 *
 * <p>The clock is shared by every test in this class and only ever moves forward, so a User one
 * test created and did not clean up would be dormant in the next. Each test therefore creates its
 * own Users at the current instant, asserts only about those (and the seeded identities), and
 * removes what it created.
 */
@SpringBootTest
@Import({
        ContainerTestConfiguration.class,
        InMemorySessionRegistryConfiguration.class,
        DormancyTestClockConfiguration.class})
@TestPropertySource(properties = "app.scim.enabled=true")
class InactivityGovernanceIntegrationTests {

    private static final Duration PAST_DEACTIVATION =
            DormancyPolicy.DEFAULT_DEACTIVATION_WINDOW.plusDays(1);

    private static final Duration PAST_AUTHORITY_REVOCATION =
            DormancyPolicy.DEFAULT_AUTHORITY_REVOCATION_WINDOW.plusDays(1);

    @Autowired
    private InactivityDeactivationService deactivation;

    @Autowired
    private DormantAuthorityRevocationService authorityRevocation;

    @Autowired
    private ScheduledJobLock jobLock;

    @Autowired
    private LoginService logins;

    @Autowired
    private ScimUserService userService;

    @Autowired
    private ScimGroupService groupService;

    @Autowired
    private ScimUserRepository users;

    @Autowired
    private ScimGroupRepository groups;

    @Autowired
    private ConnectorAdministrationService connectors;

    @Autowired
    private InMemoryAccountSessions sessions;

    @Autowired
    private MutableClock clock;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private AuthenticatedConnector connector;

    private final List<UUID> created = new ArrayList<>();

    @BeforeEach
    void setUp() {
        UUID connectorId = connectors.create("Dormancy Okta", "test-admin").id();
        connector = new AuthenticatedConnector(
                connectorId, UUID.randomUUID(), ConnectorTokenScope.READ_WRITE);
    }

    @AfterEach
    void removeOnlyWhatThisTestCreated() {
        for (UUID id : created) {
            jdbc.update("DELETE FROM scim_resources WHERE id = ? AND reserved_name IS NULL", id);
        }
        created.clear();
    }

    // ---- Part A: lastAuthenticatedAt ------------------------------------------------------

    /**
     * Every successful Login records the instant it happened, and recording it moves neither the
     * SCIM version nor {@code lastModified}.
     */
    @Test
    void everySuccessfulLoginRecordsLastAuthenticatedAtWithoutMovingTheVersion() {
        UUID seeded = activeSeededUser();
        long version = version(seeded);

        logins.logIn("test-user", "test-password");
        assertThat(lastAuthenticatedAt(seeded)).isEqualTo(clock.instant());

        clock.advanceBy(Duration.ofHours(5));
        logins.logIn("test-user", "test-password");

        assertThat(lastAuthenticatedAt(seeded)).isEqualTo(clock.instant());
        assertThat(version(seeded)).isEqualTo(version);
    }

    // ---- Part A: the demo oracle ----------------------------------------------------------

    /**
     * The seeded ordinary User logs in, goes more than 90 days without logging in again, and the
     * next run deactivates it: {@code active=false}, the version advanced once, its session ended,
     * and an actorless {@code INACTIVITY_DEACTIVATION} event plus the revocation's own event.
     * Login is then refused.
     */
    @Test
    void aSeededUserPastNinetyDaysIsDeactivatedWithVersionBumpSessionRevocationAndAudit() {
        UUID seeded = activeSeededUser();
        logins.logIn("test-user", "test-password");
        sessions.open(seeded, "seeded-session");
        long version = version(seeded);
        clock.advanceBy(PAST_DEACTIVATION);

        DormancyRun run = deactivation.deactivateDormantUsers();

        assertThat(run.skipped()).isFalse();
        assertThat(run.processed()).contains(seeded);
        assertThat(active(seeded)).isFalse();
        assertThat(version(seeded)).isEqualTo(version + 1);
        assertThat(sessions.sessionsOf(seeded)).isEmpty();
        // Since this run only: the seeded identity is shared, and an earlier test's run under the
        // same forward-moving clock may already have deactivated it once.
        Instant runAt = clock.instant();
        assertThat(auditRowsSince("INACTIVITY_DEACTIVATION", seeded, runAt)).singleElement()
                .satisfies(row -> assertThat(row)
                        .containsEntry("outcome", "SUCCESS")
                        .containsEntry("actor_id", null)
                        .containsEntry("resource_type", "User")
                        .containsEntry("changed_paths", "active"));
        assertThat(auditRowsSince("USER_SESSIONS_REVOKE", seeded, runAt)).singleElement()
                .satisfies(row -> assertThat(row)
                        .containsEntry("outcome", "SUCCESS")
                        .containsEntry("actor_id", null)
                        .containsEntry("changed_paths", "active"));

        // Put the shared seeded identity back for the rest of the class.
        activeSeededUser();
    }

    /** A User provisioned and never authenticated is measured from its creation. */
    @Test
    void aUserThatNeverAuthenticatedIsMeasuredFromItsCreation() {
        UUID never = createUser("dormancy-never", null);

        clock.advanceBy(DormancyPolicy.DEFAULT_DEACTIVATION_WINDOW.minusDays(1));
        assertThat(deactivation.deactivateDormantUsers().processed()).doesNotContain(never);
        assertThat(active(never)).isTrue();

        clock.advanceBy(Duration.ofDays(2));
        assertThat(deactivation.deactivateDormantUsers().processed()).contains(never);
        assertThat(active(never)).isFalse();
    }

    // ---- Part A: the connector does not get the last word ----------------------------------

    /**
     * A connector re-asserting {@code active=true} on a dormant User — a PUT restating it, a PATCH
     * that changes something else — resets nothing, so the run still deactivates it. Once it is
     * deactivated, a write that does not take {@code active} to true leaves it deactivated. Only
     * an explicit inactive-to-active transition reactivates it and resets the window: the next run
     * leaves it alone, and one a full window later deactivates it again.
     */
    @Test
    void onlyAnExplicitInactiveToActiveTransitionReactivatesAndResetsTheWindow() {
        UUID ada = createUser("dormancy-reassert", "first-correct-horse");
        clock.advanceBy(PAST_DEACTIVATION);
        Instant basis = dormancyBasis(ada);

        userService.replace(connector, ada, ifMatch(ada), new ScimUserReplacement(
                profile("dormancy-reassert", null, true), null, null));
        userService.patch(connector, ada, ifMatch(ada), List.of(
                new ScimUserPatchOperation.SetActive(true),
                new ScimUserPatchOperation.SetText(
                        ScimUserPatchOperation.TextAttribute.DISPLAY_NAME, "Still Here")));
        assertThat(dormancyBasis(ada)).as("re-asserting active reset nothing").isEqualTo(basis);

        assertThat(deactivation.deactivateDormantUsers().processed()).contains(ada);
        assertThat(active(ada)).isFalse();

        userService.patch(connector, ada, ifMatch(ada), List.of(
                new ScimUserPatchOperation.SetText(
                        ScimUserPatchOperation.TextAttribute.DISPLAY_NAME, "Renamed")));
        assertThat(active(ada)).as("a write that is not a reactivation").isFalse();
        assertThat(deactivation.deactivateDormantUsers().processed())
                .as("an inactive User is not processed again").doesNotContain(ada);

        userService.patch(connector, ada, ifMatch(ada), List.of(
                new ScimUserPatchOperation.SetActive(true)));
        assertThat(active(ada)).isTrue();
        assertThat(lastAuthenticatedAt(ada))
                .as("the reactivation reset the window").isEqualTo(clock.instant());

        clock.advanceBy(Duration.ofDays(1));
        assertThat(deactivation.deactivateDormantUsers().processed()).doesNotContain(ada);
        assertThat(active(ada)).isTrue();

        clock.advanceBy(PAST_DEACTIVATION);
        assertThat(deactivation.deactivateDormantUsers().processed()).contains(ada);
        assertThat(active(ada)).isFalse();
    }

    /**
     * Reactivating a credentialed User requires a password change, by either SCIM path that can
     * reactivate — a PATCH of {@code active} and a PUT restating it — because a credential that
     * sat unused across a deactivation is not trusted on return. The flag is dated by the
     * reactivation that imposed it.
     *
     * <p>The User's connector-set first password flags it at creation, so the change is completed
     * first: what the test then observes is a flag only reactivation could have set.
     */
    @Test
    void reactivatingACredentialedUserRequiresAPasswordChangeByEitherPath() {
        UUID ada = createUser("reactivate-credentialed", "first-correct-horse");
        completePasswordChange(ada);
        assertThat(passwordChangeRequiredSince(ada)).isNull();

        userService.patch(connector, ada, ifMatch(ada), List.of(
                new ScimUserPatchOperation.SetActive(false)));
        clock.advanceBy(Duration.ofDays(1));
        userService.patch(connector, ada, ifMatch(ada), List.of(
                new ScimUserPatchOperation.SetActive(true)));
        assertThat(passwordChangeRequiredSince(ada))
                .as("PATCH active=true reactivated a credentialed User")
                .isEqualTo(clock.instant());

        completePasswordChange(ada);
        userService.patch(connector, ada, ifMatch(ada), List.of(
                new ScimUserPatchOperation.SetActive(false)));
        clock.advanceBy(Duration.ofDays(1));
        userService.replace(connector, ada, ifMatch(ada), new ScimUserReplacement(
                profile("reactivate-credentialed", null, true), null, null));
        assertThat(passwordChangeRequiredSince(ada))
                .as("PUT active=true reactivated a credentialed User")
                .isEqualTo(clock.instant());

        completePasswordChange(ada);
        userService.patch(connector, ada, ifMatch(ada), List.of(
                new ScimUserPatchOperation.SetActive(true)));
        userService.replace(connector, ada, ifMatch(ada), new ScimUserReplacement(
                profile("reactivate-credentialed", null, true), null, null));
        assertThat(passwordChangeRequiredSince(ada))
                .as("re-asserting active over an active User is not a reactivation")
                .isNull();

        // The repository's own reactivation statement, which no SCIM path uses today but whose
        // contract promises the same rule.
        userService.patch(connector, ada, ifMatch(ada), List.of(
                new ScimUserPatchOperation.SetActive(false)));
        clock.advanceBy(Duration.ofDays(1));
        reactivateThroughThePort(ada);
        assertThat(passwordChangeRequiredSince(ada))
                .as("updateActive(true) reactivated a credentialed User")
                .isEqualTo(clock.instant());
    }

    /**
     * A credentialless User cannot log in, so it has no credential to distrust: reactivating it
     * sets no flag, by either path. The flag arrives with its first password.
     */
    @Test
    void reactivatingACredentiallessUserRequiresNoPasswordChange() {
        UUID grace = createUser("reactivate-credentialless", null);

        userService.patch(connector, grace, ifMatch(grace), List.of(
                new ScimUserPatchOperation.SetActive(false)));
        userService.patch(connector, grace, ifMatch(grace), List.of(
                new ScimUserPatchOperation.SetActive(true)));
        assertThat(passwordChangeRequiredSince(grace)).as("after PATCH").isNull();

        userService.patch(connector, grace, ifMatch(grace), List.of(
                new ScimUserPatchOperation.SetActive(false)));
        userService.replace(connector, grace, ifMatch(grace), new ScimUserReplacement(
                profile("reactivate-credentialless", null, true), null, null));
        assertThat(passwordChangeRequiredSince(grace)).as("after PUT").isNull();

        userService.patch(connector, grace, ifMatch(grace), List.of(
                new ScimUserPatchOperation.SetActive(false)));
        reactivateThroughThePort(grace);
        assertThat(active(grace)).isTrue();
        assertThat(passwordChangeRequiredSince(grace)).as("after updateActive(true)").isNull();
    }

    private void reactivateThroughThePort(UUID user) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                users.updateActive(user, true, clock.instant()));
    }

    /** As a successful self-service change leaves it, keeping the same credential. */
    private void completePasswordChange(UUID user) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                users.completePasswordChange(user, passwordHashOf(user), clock.instant()));
    }

    // ---- Part A: concurrency --------------------------------------------------------------

    /**
     * Two runs started together process each dormant User exactly once: one version bump and one
     * event per User, whichever run did it and however they interleaved.
     */
    @Test
    void twoConcurrentDeactivationRunsDoNotDoubleProcessAUser() throws Exception {
        List<UUID> dormant = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            dormant.add(createUser("dormancy-concurrent-" + i, null));
        }
        Map<UUID, Long> versions = versionsOf(dormant);
        clock.advanceBy(PAST_DEACTIVATION);

        List<DormancyRun> runs = concurrently(deactivation::deactivateDormantUsers);

        for (UUID user : dormant) {
            assertThat(runs.stream().filter(run -> run.processed().contains(user)).count())
                    .as("runs that processed %s", user).isEqualTo(1);
            assertThat(version(user)).isEqualTo(versions.get(user) + 1);
            assertThat(auditRows("INACTIVITY_DEACTIVATION", user)).hasSize(1);
            assertThat(active(user)).isFalse();
        }
    }

    // ---- Part B: the demo oracle ----------------------------------------------------------

    /**
     * A dormant Admin past 180 days loses its direct Admin-group membership and nothing else: it
     * stays active, stays in its ordinary Group (whose version does not move), and its session
     * ends. The Admin group's and the User's versions advance once, and an actorless
     * {@code DORMANT_AUTHORITY_REVOCATION} event names the User. A connector that then re-adds
     * the membership sees it removed again on the next run while the User stays dormant.
     */
    @Test
    void aDormantAdminLosesOnlyItsAdminAuthorityAndAReAddedMembershipIsRemovedAgain() {
        UUID ada = createUser("dormancy-admin", "first-correct-horse");
        UUID ordinary = createGroup("Dormancy Engineering", ada);
        UUID adminGroup = adminGroupId();
        addToAdminGroup(ada);
        long adminGroupVersion = version(adminGroup);
        long ordinaryVersion = version(ordinary);
        long userVersion = version(ada);
        sessions.open(ada, "admin-session");
        clock.advanceBy(PAST_AUTHORITY_REVOCATION);

        DormancyRun run = authorityRevocation.revokeDormantAuthority();

        assertThat(run.skipped()).isFalse();
        assertThat(run.processed()).contains(ada);
        assertThat(isAdmin(ada)).isFalse();
        assertThat(isMember(ordinary, ada)).as("ordinary membership intact").isTrue();
        assertThat(version(ordinary)).isEqualTo(ordinaryVersion);
        assertThat(active(ada)).as("baseline access intact").isTrue();
        assertThat(version(adminGroup)).isEqualTo(adminGroupVersion + 1);
        assertThat(version(ada)).isEqualTo(userVersion + 1);
        assertThat(isAdmin(bootstrapAdmin())).isTrue();
        assertThat(sessions.sessionsOf(ada)).isEmpty();
        assertThat(auditRows("DORMANT_AUTHORITY_REVOCATION", ada)).singleElement()
                .satisfies(row -> assertThat(row)
                        .containsEntry("outcome", "SUCCESS")
                        .containsEntry("actor_id", null)
                        .containsEntry("resource_type", "User")
                        .containsEntry("changed_paths", "groups"));
        assertThat(auditRows("USER_SESSIONS_REVOKE", ada)).singleElement()
                .satisfies(row -> assertThat(row)
                        .containsEntry("actor_id", null)
                        .containsEntry("changed_paths", "groups"));

        addToAdminGroup(ada);
        assertThat(isAdmin(ada)).as("the connector re-added it").isTrue();
        clock.advanceBy(Duration.ofDays(1));

        assertThat(authorityRevocation.revokeDormantAuthority().processed()).contains(ada);
        assertThat(isAdmin(ada)).isFalse();
        assertThat(isMember(ordinary, ada)).isTrue();
        assertThat(auditRows("DORMANT_AUTHORITY_REVOCATION", ada)).hasSize(2);
    }

    /** Two runs started together remove each dormant Admin's membership exactly once. */
    @Test
    void twoConcurrentAuthorityRunsDoNotDoubleProcessAUser() throws Exception {
        List<UUID> admins = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            UUID admin = createUser("dormancy-concurrent-admin-" + i, null);
            addToAdminGroup(admin);
            admins.add(admin);
        }
        Map<UUID, Long> versions = versionsOf(admins);
        clock.advanceBy(PAST_AUTHORITY_REVOCATION);

        List<DormancyRun> runs = concurrently(authorityRevocation::revokeDormantAuthority);

        for (UUID admin : admins) {
            assertThat(runs.stream().filter(run -> run.processed().contains(admin)).count())
                    .as("runs that processed %s", admin).isEqualTo(1);
            assertThat(version(admin)).isEqualTo(versions.get(admin) + 1);
            assertThat(auditRows("DORMANT_AUTHORITY_REVOCATION", admin)).hasSize(1);
            assertThat(isAdmin(admin)).isFalse();
        }
    }

    // ---- serialization --------------------------------------------------------------------

    /**
     * While another transaction holds the inactivity job's lock, a run of that job skips and
     * changes nothing — and the dormant-authority job, holding a different lock, runs regardless.
     * Once the holder commits, the inactivity job runs again. Then the same the other way round.
     */
    @Test
    void eachJobIsSerializedOnItsOwnLockAndNeverBlocksTheOther() throws Exception {
        UUID ada = createUser("dormancy-serialized", null);
        addToAdminGroup(ada);
        clock.advanceBy(PAST_AUTHORITY_REVOCATION);

        whileHolding(ScheduledJob.INACTIVITY_DEACTIVATION, () -> {
            DormancyRun blocked = deactivation.deactivateDormantUsers();
            assertThat(blocked.skipped()).isTrue();
            assertThat(active(ada)).isTrue();

            DormancyRun other = authorityRevocation.revokeDormantAuthority();
            assertThat(other.skipped()).as("the other job is not blocked").isFalse();
            assertThat(other.processed()).contains(ada);
        });
        DormancyRun afterRelease = deactivation.deactivateDormantUsers();
        assertThat(afterRelease.skipped()).isFalse();
        assertThat(afterRelease.processed()).contains(ada);

        whileHolding(ScheduledJob.DORMANT_AUTHORITY_REVOCATION, () -> {
            assertThat(authorityRevocation.revokeDormantAuthority().skipped()).isTrue();
            assertThat(deactivation.deactivateDormantUsers().skipped()).isFalse();
        });
        assertThat(authorityRevocation.revokeDormantAuthority().skipped()).isFalse();
    }

    // ---- the Bootstrap Admin --------------------------------------------------------------

    /**
     * The real seeded Bootstrap Admin, dormant far past both windows, is processed by neither
     * job: still active, still in the Admin group, its version unmoved, its session intact, and no
     * event naming it.
     */
    @Test
    void theSeededBootstrapAdminIsNeverProcessedByEitherJob() {
        UUID bootstrap = bootstrapAdmin();
        jdbc.update("UPDATE scim_users SET last_authenticated_at = NULL WHERE resource_id = ?",
                bootstrap);
        sessions.open(bootstrap, "bootstrap-session");
        long version = version(bootstrap);
        long adminGroupVersion = version(adminGroupId());
        clock.advanceBy(PAST_AUTHORITY_REVOCATION.multipliedBy(2));
        assertThat(users.findById(bootstrap).orElseThrow()
                .isDormantAt(DormancyPolicy.defaults().authorityRevocationCutoff(clock.instant())))
                .as("dormant by its basis, so only the exemption saves it").isTrue();

        DormancyRun deactivated = deactivation.deactivateDormantUsers();
        DormancyRun revoked = authorityRevocation.revokeDormantAuthority();

        assertThat(deactivated.processed()).doesNotContain(bootstrap);
        assertThat(revoked.processed()).doesNotContain(bootstrap);
        assertThat(active(bootstrap)).isTrue();
        assertThat(isAdmin(bootstrap)).isTrue();
        assertThat(version(bootstrap)).isEqualTo(version);
        assertThat(version(adminGroupId())).isEqualTo(adminGroupVersion);
        assertThat(sessions.sessionsOf(bootstrap)).containsExactly("bootstrap-session");
        assertThat(auditRows("INACTIVITY_DEACTIVATION", bootstrap)).isEmpty();
        assertThat(auditRows("DORMANT_AUTHORITY_REVOCATION", bootstrap)).isEmpty();
        sessions.revokeAll(bootstrap);
    }

    // ---- helpers --------------------------------------------------------------------------

    /**
     * The seeded ordinary User, active and with a fresh dormancy basis — reactivated first if an
     * earlier test's run deactivated it, which the shared forward-moving clock makes likely.
     */
    private UUID activeSeededUser() {
        UUID id = users.findByNormalizedUserName(NormalizedUserName.of("test-user"))
                .orElseThrow().id();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            if (!active(id)) {
                users.updateActive(id, true, clock.instant());
                // A reactivation requires a password change; the shared seeded User's baseline is
                // unflagged, so later tests can log in as it.
                users.completePasswordChange(id, passwordHashOf(id), clock.instant());
            }
        });
        return id;
    }

    private UUID bootstrapAdmin() {
        return users.findByReservedName(ReservedResourceName.BOOTSTRAP_ADMIN).orElseThrow().id();
    }

    private UUID adminGroupId() {
        return groups.findByReservedName(ReservedResourceName.ADMIN_GROUP).orElseThrow().id();
    }

    private UUID createUser(String userName, String password) {
        UUID id = userService.create(connector, new NewScimUser(
                profile(userName, null, true), password, null)).id();
        created.add(id);
        return id;
    }

    private UUID createGroup(String displayName, UUID member) {
        UUID id = groupService.create(
                connector, new NewScimGroup(displayName, List.of(member), null)).id();
        created.add(id);
        return id;
    }

    /** Adds the User to the Admin group the way a connector does: a conditional SCIM PATCH. */
    private void addToAdminGroup(UUID user) {
        UUID adminGroup = adminGroupId();
        groupService.patch(connector, adminGroup, ifMatch(adminGroup),
                List.of(new ScimGroupPatchOperation.AddMembers(List.of(user))));
    }

    private static ScimUserProfile profile(String userName, String displayName, boolean active) {
        return new ScimUserProfile(userName, null, displayName, null, null, null, active, List.of());
    }

    private ScimVersionPrecondition ifMatch(UUID resource) {
        return ScimVersionPrecondition.ofIfMatch(List.of("\"" + version(resource) + "\""));
    }

    /** Runs the job on two threads released together, and returns both runs. */
    private static List<DormancyRun> concurrently(java.util.concurrent.Callable<DormancyRun> job)
            throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<DormancyRun>> runs = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                runs.add(pool.submit(() -> {
                    start.await();
                    return job.call();
                }));
            }
            start.countDown();
            List<DormancyRun> results = new ArrayList<>();
            for (Future<DormancyRun> run : runs) {
                results.add(run.get(60, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * Holds the job's lock in a transaction on another thread — as a run on another instance
     * would — while {@code work} runs here, then commits it.
     */
    private void whileHolding(ScheduledJob job, ThrowingRunnable work) throws Exception {
        CountDownLatch held = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService holder = Executors.newSingleThreadExecutor();
        try {
            Future<Boolean> acquired = holder.submit(() ->
                    new TransactionTemplate(transactionManager).execute(status -> {
                        boolean got = jobLock.tryAcquire(job);
                        held.countDown();
                        try {
                            release.await(60, TimeUnit.SECONDS);
                        } catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                        }
                        return got;
                    }));
            assertThat(held.await(30, TimeUnit.SECONDS)).isTrue();
            try {
                work.run();
            } finally {
                release.countDown();
            }
            assertThat(acquired.get(60, TimeUnit.SECONDS)).as("the holder had the lock").isTrue();
        } finally {
            holder.shutdownNow();
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private Map<UUID, Long> versionsOf(List<UUID> ids) {
        Map<UUID, Long> versions = new java.util.HashMap<>();
        ids.forEach(id -> versions.put(id, version(id)));
        return versions;
    }

    private long version(UUID resource) {
        return jdbc.queryForObject(
                "SELECT version FROM scim_resources WHERE id = ?", Long.class, resource);
    }

    private boolean active(UUID user) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT active FROM scim_users WHERE resource_id = ?", Boolean.class, user));
    }

    private Instant lastAuthenticatedAt(UUID user) {
        Timestamp at = jdbc.queryForObject(
                "SELECT last_authenticated_at FROM scim_users WHERE resource_id = ?",
                Timestamp.class, user);
        return at == null ? null : at.toInstant();
    }

    private Instant passwordChangeRequiredSince(UUID user) {
        Timestamp at = jdbc.queryForObject(
                "SELECT password_change_required_since FROM scim_users WHERE resource_id = ?",
                Timestamp.class, user);
        return at == null ? null : at.toInstant();
    }

    private String passwordHashOf(UUID user) {
        return users.findById(user).orElseThrow().login().passwordHash();
    }

    private Instant dormancyBasis(UUID user) {
        ScimUser stored = users.findById(user).orElseThrow();
        return stored.dormancyBasis();
    }

    private boolean isAdmin(UUID user) {
        return groups.isMemberOfReservedGroup(user, ReservedResourceName.ADMIN_GROUP);
    }

    private boolean isMember(UUID group, UUID user) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM scim_group_members WHERE group_id = ? AND user_id = ?",
                Integer.class, group, user) == 1;
    }

    private List<Map<String, Object>> auditRows(String operation, UUID subject) {
        return auditRowsSince(operation, subject, Instant.EPOCH);
    }

    private List<Map<String, Object>> auditRowsSince(String operation, UUID subject, Instant since) {
        return jdbc.queryForList("""
                SELECT outcome, actor_id, resource_type, changed_paths FROM audit_events
                 WHERE operation = ? AND subject_id = ? AND occurred_at >= ?
                 ORDER BY occurred_at""", operation, subject, Timestamp.from(since));
    }
}
