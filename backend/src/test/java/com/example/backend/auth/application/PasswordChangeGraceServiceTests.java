package com.example.backend.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.audit.RecordingAuditTrail;
import com.example.backend.audit.domain.AuditOperation;
import com.example.backend.auth.InMemoryAccountSessions;
import com.example.backend.auth.InMemoryScheduledJobLock;
import com.example.backend.auth.MutableClock;
import com.example.backend.auth.PendingCommit;
import com.example.backend.auth.domain.ScheduledJob;
import com.example.backend.scim.InMemoryScimUserRepository;
import com.example.backend.scim.ScimIdentities;
import com.example.backend.scim.domain.PasswordChangeGracePolicy;
import com.example.backend.scim.domain.ReservedResourceName;
import com.example.backend.scim.domain.ScimLoginState;
import com.example.backend.scim.domain.ScimUser;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The grace-period job against the in-memory directory. The same job against Postgres, the real
 * job lock and the seeded Bootstrap Admin, under a simulated clock, is in
 * {@code PasswordChangeLifecycleIntegrationTests}.
 */
class PasswordChangeGraceServiceTests {

    private static final Duration WINDOW = PasswordChangeGracePolicy.DEFAULT_WINDOW;

    private final InMemoryScimUserRepository users = new InMemoryScimUserRepository();
    private final InMemoryAccountSessions accountSessions = new InMemoryAccountSessions();
    private final PendingCommit transaction = new PendingCommit();
    private final RecordingAuditTrail audit = new RecordingAuditTrail();
    private final InMemoryScheduledJobLock lock = new InMemoryScheduledJobLock();
    private final MutableClock clock = new MutableClock(ScimIdentities.NOW);

    private PasswordChangeGraceService job;

    @BeforeEach
    void setUp() {
        job = new PasswordChangeGraceService(
                users,
                new ScimUserSessionRevocation(accountSessions, transaction, audit),
                lock,
                PasswordChangeGracePolicy.defaults(),
                audit,
                clock);
    }

    @Test
    void aUserFlaggedLongerThanTheWindowIsDeactivatedWithSessionsRevokedAndAnEvent() {
        ScimUser ada = users.given(flagged("ada"));
        accountSessions.open(ada.id(), "ada-session");
        clock.advanceBy(WINDOW.plusSeconds(1));

        DormancyRun run = job.deactivateOverdueUsers();

        ScimUser after = users.require("ada");
        assertThat(run.skipped()).isFalse();
        assertThat(run.processed()).containsExactly(ada.id());
        assertThat(after.profile().active()).isFalse();
        assertThat(after.version()).isEqualTo(ada.version() + 1);
        assertThat(audit.of(AuditOperation.PASSWORD_CHANGE_GRACE_DEACTIVATION))
                .containsExactly(new RecordingAuditTrail.Recorded(
                        AuditOperation.PASSWORD_CHANGE_GRACE_DEACTIVATION, null, ada.id(), null));
        assertThat(accountSessions.sessionsOf(ada.id())).as("after the commit").hasSize(1);
        transaction.commit();
        assertThat(accountSessions.sessionsOf(ada.id())).isEmpty();
        assertThat(audit.of(AuditOperation.USER_SESSIONS_REVOKE))
                .singleElement()
                .extracting(RecordingAuditTrail.Recorded::detail)
                .isEqualTo("SUCCESS:ACTIVE");
    }

    @Test
    void aUserExactlyAtTheWindowIsStillWithinIt() {
        users.given(flagged("ada"));
        clock.advanceBy(WINDOW);

        assertThat(job.deactivateOverdueUsers().processed()).isEmpty();
        assertThat(users.require("ada").profile().active()).isTrue();
    }

    @Test
    void anUnflaggedUserIsNeverDeactivatedByThisJob() {
        users.given(ScimIdentities.user("bob"));
        clock.advanceBy(WINDOW.multipliedBy(10));

        assertThat(job.deactivateOverdueUsers().processed()).isEmpty();
        assertThat(users.require("bob").profile().active()).isTrue();
    }

    @Test
    void theBootstrapAdminIsExempt() {
        ScimUser root = users.createReserved(flagged("root"), ReservedResourceName.BOOTSTRAP_ADMIN);
        clock.advanceBy(WINDOW.multipliedBy(10));

        assertThat(job.deactivateOverdueUsers().processed()).isEmpty();
        assertThat(users.require("root").profile().active()).isTrue();
        assertThat(root.isExemptFromPasswordChangeGrace()).isTrue();
        assertThat(audit.recorded()).isEmpty();
    }

    @Test
    void aRunSkipsWhenAnotherRunOfThisJobHoldsTheLock() {
        users.given(flagged("ada"));
        clock.advanceBy(WINDOW.plusSeconds(1));
        lock.holdElsewhere(ScheduledJob.PASSWORD_CHANGE_GRACE_DEACTIVATION);

        DormancyRun run = job.deactivateOverdueUsers();

        assertThat(run.skipped()).isTrue();
        assertThat(users.require("ada").profile().active()).isTrue();
    }

    @Test
    void anAlreadyInactiveUserIsLeftAlone() {
        users.given(new ScimUser(
                UUID.randomUUID(),
                ScimIdentities.profile("ada", false),
                flaggedLogin(),
                null,
                ScimUser.INITIAL_VERSION,
                ScimIdentities.NOW,
                ScimIdentities.NOW));
        clock.advanceBy(WINDOW.plusSeconds(1));

        assertThat(job.deactivateOverdueUsers().processed()).isEmpty();
        assertThat(audit.recorded()).isEmpty();
    }

    @Test
    void theLockedReadDecidesAgainWhateverTheCandidateQueryAnswered() {
        // Each stale candidate fails exactly one of the locked-read checks, so every check is
        // needed on its own: the Bootstrap Admin (exempt), an inactive User, a User whose flag is
        // still within the window, an unflagged User, and an id that is gone.
        ScimUser root = users.createReserved(flagged("root"), ReservedResourceName.BOOTSTRAP_ADMIN);
        ScimUser inactive = users.given(new ScimUser(
                UUID.randomUUID(),
                ScimIdentities.profile("gone", false),
                flaggedLogin(),
                null,
                ScimUser.INITIAL_VERSION,
                ScimIdentities.NOW,
                ScimIdentities.NOW));
        clock.advanceBy(WINDOW.plusSeconds(1));
        ScimUser recent = users.given(ScimIdentities.userWithLoginState(
                "recent", new ScimLoginState("hash", 0, null, null, clock.instant())));
        ScimUser unflagged = users.given(ScimIdentities.user("bob"));
        users.answerPasswordChangeCandidatesWith(List.of(
                root.id(), inactive.id(), recent.id(), unflagged.id(), UUID.randomUUID()));
        int writesBefore = users.writes();

        DormancyRun run = job.deactivateOverdueUsers();

        assertThat(run.skipped()).isFalse();
        assertThat(run.processed()).isEmpty();
        assertThat(users.writes()).isEqualTo(writesBefore);
        assertThat(users.require("root").profile().active()).isTrue();
        assertThat(users.require("recent").profile().active()).isTrue();
        assertThat(users.require("bob").profile().active()).isTrue();
        assertThat(audit.recorded()).isEmpty();
        assertThat(transaction.pending()).isZero();
    }

    @Test
    void aStaleListStillDeactivatesTheCandidateThatIsDueOnTheLockedRead() {
        ScimUser ada = users.given(flagged("ada"));
        clock.advanceBy(WINDOW.plusSeconds(1));
        users.answerPasswordChangeCandidatesWith(List.of(UUID.randomUUID(), ada.id()));

        assertThat(job.deactivateOverdueUsers().processed()).containsExactly(ada.id());
        assertThat(users.require("ada").profile().active()).isFalse();
    }

    private static ScimUser flagged(String userName) {
        return ScimIdentities.userWithLoginState(userName, flaggedLogin());
    }

    private static ScimLoginState flaggedLogin() {
        return new ScimLoginState("hash", 0, null, null, ScimIdentities.NOW);
    }
}
