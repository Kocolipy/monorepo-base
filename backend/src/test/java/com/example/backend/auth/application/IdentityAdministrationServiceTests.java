package com.example.backend.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import ch.qos.logback.classic.Level;
import com.example.backend.audit.CapturedLog;
import com.example.backend.audit.RecordingAuditTrail;
import com.example.backend.audit.RecordingAuditTrail.Recorded;
import com.example.backend.audit.domain.AuditAdministrativeRefusal;
import com.example.backend.audit.domain.AuditOperation;
import com.example.backend.auth.InMemoryAccountSessions;
import com.example.backend.auth.MutableClock;
import com.example.backend.auth.PendingCommit;
import com.example.backend.observability.LogEvent;
import com.example.backend.scim.InMemoryScimGroupRepository;
import com.example.backend.scim.InMemoryScimUserRepository;
import com.example.backend.scim.ScimIdentities;
import com.example.backend.scim.domain.NormalizedUserName;
import com.example.backend.scim.domain.ReservedResourceName;
import com.example.backend.scim.domain.ScimGroup;
import com.example.backend.scim.domain.ScimLoginState;
import com.example.backend.scim.domain.ScimUser;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * The identity use cases an administrator drives, against the unified SCIM identity.
 *
 * <p>Replaces {@code AccountAdministrationServiceTests}. Three things changed shape rather
 * than meaning and are worth naming, because every assertion below is the old one
 * translated through them:
 *
 * <ul>
 *   <li>{@code enabled} became SCIM's {@code active}, so disable/enable became
 *       deactivate/activate and {@code IdentitySummary.active} is what they report.
 *   <li>The role column became DERIVED membership of the reserved Admin group, so "the last
 *       enabled administrator" is now a question about that Group's members.
 *   <li>The Bootstrap Admin is recognised by its reservation marker rather than by a
 *       configured name — which is why it is seeded through {@code createReserved} and why
 *       the refusal cannot be moved by a rename.
 * </ul>
 *
 * <p>And one behaviour is genuinely new: every refusal is AUDITED as well as logged, because
 * a run of attempts to deactivate the recovery identity is a signal only the trail can
 * carry. The old suite asserted a refused change recorded nothing; it now asserts the
 * refusal event and its closed-set reason.
 */
class IdentityAdministrationServiceTests {

    /** One instant for the store and the clock, so a summary's timestamps are comparable. */
    private static final Instant NOW = ScimIdentities.NOW;

    /** Ten years: far past any window the former expiring lockout could have had. */
    private static final Duration A_LONG_TIME = Duration.ofDays(3650);

    /** The recovery identity's name, which is deliberately NOT what protects it. */
    private static final String BOOTSTRAP = "root";

    private final InMemoryScimUserRepository users = new InMemoryScimUserRepository();
    private final InMemoryScimGroupRepository groups = new InMemoryScimGroupRepository(users);
    private final InMemoryAccountSessions sessions = new InMemoryAccountSessions();
    private final PendingCommit transaction = new PendingCommit();
    private final MutableClock clock = new MutableClock(NOW);
    private final RecordingAuditTrail audit = new RecordingAuditTrail();

    private final IdentityAdministrationService service = new IdentityAdministrationService(
            users, groups, sessions, transaction, audit, clock);

    // Reviewing who has access

    @Test
    void listsEveryIdentityWithoutItsPasswordHash() {
        ScimUser ada = given("ada");
        ScimUser bob = given("bob");
        givenAdminGroup(ada);

        assertThat(service.listIdentities()).containsExactly(
                new IdentitySummary(ada.id(), "ada", true, true, false, true, NOW),
                new IdentitySummary(bob.id(), "bob", false, true, false, true, NOW));
    }

    @Test
    void listsIdentitiesOrderedByUserName() {
        given("zoe");
        given("ada");
        given("bob");

        assertThat(service.listIdentities())
                .extracting(IdentitySummary::userName)
                .containsExactly("ada", "bob", "zoe");
    }

    @Test
    void listsNoIdentitiesWhenNoneAreStored() {
        assertThat(service.listIdentities()).isEmpty();
    }

    /**
     * The listing reports the lock and no expiry, because there is none: the state does not
     * change with the clock, so a reader has nothing to compare and no reason to wait.
     */
    @Test
    void reportsALockoutAsInForceHoweverLongItHasStood() {
        givenLocked("ada");

        assertThat(service.listIdentities()).first()
                .extracting(IdentitySummary::locked)
                .isEqualTo(true);

        clock.advanceBy(A_LONG_TIME);

        assertThat(service.listIdentities()).first()
                .extracting(IdentitySummary::locked)
                .isEqualTo(true);
    }

    /**
     * {@code admin} is derived from Group membership rather than read from a column, so the
     * listing reports the same fact the login path derives and there is no column the two
     * could disagree about.
     */
    @Test
    void reportsTheAdminFlagFromMembershipOfTheReservedAdminGroup() {
        ScimUser ada = given("ada");
        given("bob");
        // A Group that merely displays as the administrators' one confers nothing.
        groups.createReserved(
                ScimIdentities.group("Reserved administrators", ada),
                ReservedResourceName.ADMIN_GROUP);
        groups.create(ScimIdentities.group("Admins", users.require("bob")));

        assertThat(service.listIdentities())
                .extracting(IdentitySummary::userName, IdentitySummary::admin)
                .containsExactly(tuple("ada", true), tuple("bob", false));
    }

    /**
     * A credentialless identity exists and cannot log in, which is otherwise
     * indistinguishable from a forgotten password — so the listing says which it is.
     */
    @Test
    void reportsWhetherACredentialIsSetAtAll() {
        users.given(ScimIdentities.credentiallessUser("nopass"));

        assertThat(service.listIdentities()).first()
                .extracting(IdentitySummary::hasPassword)
                .isEqualTo(false);
    }

    // Deactivating

    @Test
    void deactivatingClosesTheIdentityAndReportsItBack() {
        given("bob");

        IdentitySummary deactivated = service.deactivate("bob", "ada");

        assertThat(deactivated.active()).isFalse();
        assertThat(users.require("bob").profile().active()).isFalse();
    }

    /**
     * The two capabilities are separate, so deactivating must not double as a penalty reset:
     * the failure run is evidence, and it is most wanted at exactly the moment an identity
     * is being closed.
     */
    @Test
    void deactivatingLeavesTheFailureRunAndLockoutUntouched() {
        givenLocked("bob");

        service.deactivate("bob", "ada");

        ScimUser stored = users.require("bob");
        assertThat(stored.login().failedLoginAttempts()).isEqualTo(3);
        assertThat(stored.login().lockedAt()).isEqualTo(NOW);
        assertThat(stored.login().isLocked()).isTrue();
    }

    @Test
    void deactivatingAnAlreadyInactiveIdentityWritesNothing() {
        users.given(ScimIdentities.inactiveUser("bob"));
        int before = users.writes();

        assertThat(service.deactivate("bob", "ada").active()).isFalse();
        assertThat(users.writes()).isEqualTo(before);
    }

    /**
     * {@code active} IS a SCIM attribute, so unlike a failure run, writing it advances the
     * resource's version: a connector's cached copy of this User is genuinely stale.
     */
    @Test
    void deactivatingAdvancesTheResourceVersion() {
        ScimUser bob = given("bob");

        service.deactivate("bob", "ada");

        assertThat(users.require("bob").version()).isEqualTo(bob.version() + 1);
    }

    @Test
    void refusesToDeactivateTheIdentityMakingTheRequest() {
        ScimUser ada = given("ada");
        ScimUser zoe = given("zoe");
        givenAdminGroup(ada, zoe);

        assertThatThrownBy(() -> service.deactivate("ada", "ada"))
                .isInstanceOf(UnsafeIdentityChangeException.class)
                .hasMessage("An identity cannot deactivate itself");
        assertThat(users.require("ada").profile().active()).isTrue();
    }

    /**
     * The self-guard compares NORMALIZED names, because the subject was looked up on the
     * normalized form.
     *
     * <p>This was a real defect, found by migrating these tests: the guard compared the raw
     * strings, so an administrator whose session carried a differently-cased spelling of their
     * own name resolved to the same identity, failed the equality check, and could deactivate
     * themselves — with only the last-active-administrator guard left to catch it, which a
     * second active administrator satisfies. Two active administrators are arranged here for
     * exactly that reason: without the fix, this deactivation succeeds.
     */
    @Test
    void refusesToDeactivateTheRequesterWhateverCaseTheirNameWasSubmittedIn() {
        ScimUser ada = given("ada");
        ScimUser zoe = given("zoe");
        givenAdminGroup(ada, zoe);

        assertThatThrownBy(() -> service.deactivate("ada", "ADA"))
                .isInstanceOf(UnsafeIdentityChangeException.class)
                .hasMessage("An identity cannot deactivate itself");
        assertThat(users.require("ada").profile().active()).isTrue();
    }

    /**
     * An actor whose name cannot be normalized at all names nobody, rather than failing the
     * whole operation.
     *
     * <p>Also a real defect found by this migration: the actor lookup normalized its argument
     * unguarded, so a blank or absent requester threw out of a Group write and turned an
     * administrative change into a {@code 500} — in a class whose surrounding design
     * deliberately tolerates an unresolvable administrator and records the event with no actor.
     */
    @Test
    void recordsAnUnresolvableRequesterAsNoActorRatherThanFailing() {
        ScimUser ada = given("ada");
        ScimUser zoe = given("zoe");
        givenAdminGroup(ada, zoe);

        assertThat(service.deactivate("ada", "  ").active()).isFalse();
        assertThat(audit.of(AuditOperation.ACCOUNT_DISABLE)).singleElement()
                .satisfies(event -> {
                    assertThat(event.actorId()).isNull();
                    assertThat(event.subjectId()).isEqualTo(ada.id());
                });
    }

    /**
     * Nothing else could undo this: reactivating an identity needs an administrator who can
     * log in, and the last one deactivated cannot.
     */
    @Test
    void refusesToDeactivateTheLastActiveAdministrator() {
        ScimUser ada = given("ada");
        given("bob");
        givenAdminGroup(ada);

        assertThatThrownBy(() -> service.deactivate("ada", "zoe"))
                .isInstanceOf(UnsafeIdentityChangeException.class)
                .hasMessageContaining("last active administrator");
        assertThat(users.require("ada").profile().active()).isTrue();
    }

    @Test
    void allowsDeactivatingAnAdministratorWhileAnotherActiveOneRemains() {
        ScimUser ada = given("ada");
        ScimUser zoe = given("zoe");
        givenAdminGroup(ada, zoe);

        assertThat(service.deactivate("ada", "zoe").active()).isFalse();
    }

    /**
     * A locked administrator still counts as a means of recovery — not because the lockout
     * ends by itself, which it no longer does, but because the Bootstrap Admin can always
     * log in and unlock it. Excluding a locked administrator here would refuse
     * deactivations that leave the deployment perfectly recoverable.
     */
    @Test
    void countsALockedAdministratorAsAvailableForRecovery() {
        ScimUser ada = givenLocked("ada");
        ScimUser zoe = given("zoe");
        givenAdminGroup(ada, zoe);

        assertThat(service.deactivate("zoe", "ada").active()).isFalse();
    }

    /**
     * What makes the clause above safe. A locked administrator counts as available because
     * the Bootstrap Admin cannot be locked and cannot be closed out — an argument that holds
     * only while the second half is true, so the refusal is asserted rather than left to the
     * javadoc that relies on it.
     */
    @Test
    void refusesToDeactivateTheBootstrapAdmin() {
        ScimUser recovery = givenBootstrapAdmin();
        ScimUser ada = given("ada");
        ScimUser zoe = given("zoe");
        givenAdminGroup(recovery, ada, zoe);

        assertThatThrownBy(() -> service.deactivate(BOOTSTRAP, "ada"))
                .isInstanceOf(UnsafeIdentityChangeException.class)
                .hasMessageContaining("recovery identity");

        assertThat(users.require(BOOTSTRAP).profile().active()).isTrue();
    }

    /**
     * The refusal does not depend on how many administrators are active, which is the whole
     * difference between it and the last-active-administrator guard: two other active
     * administrators would satisfy that one, and the deployment is still unrecoverable once
     * both of them lock themselves out.
     */
    @Test
    void refusesToDeactivateTheBootstrapAdminEvenBesidePlentyOfOtherAdministrators() {
        ScimUser recovery = givenBootstrapAdmin();
        ScimUser ada = givenLocked("ada");
        ScimUser zoe = givenLocked("zoe");
        givenAdminGroup(recovery, ada, zoe);

        assertThatThrownBy(() -> service.deactivate(BOOTSTRAP, "ada"))
                .isInstanceOf(UnsafeIdentityChangeException.class);
    }

    /**
     * The protection is the reservation marker, not the name — so an ordinary identity
     * holding a name a configured-string guard would have matched is deactivated like any
     * other, and the reserved identity is refused whatever it is called.
     */
    @Test
    void theProtectionFollowsTheReservationMarkerAndNotTheUserName() {
        ScimUser lookalike = users.createReserved(
                ScimIdentities.user("someone-else"), ReservedResourceName.BOOTSTRAP_ADMIN);
        ScimUser ada = given(BOOTSTRAP);
        ScimUser zoe = given("zoe");
        givenAdminGroup(lookalike, ada, zoe);

        assertThat(service.deactivate(BOOTSTRAP, "zoe").active()).isFalse();
        assertThatThrownBy(() -> service.deactivate("someone-else", "zoe"))
                .isInstanceOf(UnsafeIdentityChangeException.class)
                .hasMessageContaining("recovery identity");
    }

    /**
     * A refused deactivation revokes nothing, so the recovery identity keeps the session it
     * is holding: the refusal has to leave the deployment exactly as reachable as it found
     * it, including for a Bootstrap Admin that is already signed in.
     */
    @Test
    void aRefusedBootstrapAdminDeactivationLeavesItsSessionsAlone() {
        ScimUser recovery = givenBootstrapAdmin();
        ScimUser ada = given("ada");
        givenAdminGroup(recovery, ada);
        sessions.open(recovery.id(), "session-1");

        assertThatThrownBy(() -> service.deactivate(BOOTSTRAP, "ada"))
                .isInstanceOf(UnsafeIdentityChangeException.class);
        transaction.commit();

        assertThat(sessions.sessionsOf(recovery.id())).containsExactly("session-1");
    }

    /**
     * The guard is the reserved identity, not administrators in general: an ordinary
     * administrator is deactivated as before. Without this the refusal above would be
     * indistinguishable from one that had started refusing every administrative
     * deactivation.
     */
    @Test
    void stillDeactivatesAnOrdinaryAdministratorThatIsNotTheRecoveryIdentity() {
        ScimUser recovery = givenBootstrapAdmin();
        ScimUser ada = given("ada");
        ScimUser zoe = given("zoe");
        givenAdminGroup(recovery, ada, zoe);

        assertThat(service.deactivate("zoe", "ada").active()).isFalse();
    }

    @Test
    void doesNotCountAnInactiveAdministratorAsAvailableForRecovery() {
        ScimUser ada = given("ada");
        ScimUser zoe = users.given(ScimIdentities.inactiveUser("zoe"));
        givenAdminGroup(ada, zoe);

        assertThatThrownBy(() -> service.deactivate("ada", "bob"))
                .isInstanceOf(UnsafeIdentityChangeException.class);
    }

    /**
     * The recovery guard is about administrators, and asks whether the identity being
     * deactivated is one before it counts anyone. A non-member is never the last active
     * administrator, whatever the administrators' standing — and it takes asking: "every
     * active administrator is this identity" is vacuously true of no administrators at all,
     * so a check that counted first would start refusing every deactivation the moment the
     * last administrator was closed out of band.
     */
    @Test
    void deactivatesANonAdministratorEvenWhenNoAdministratorIsActive() {
        ScimUser ada = users.given(ScimIdentities.inactiveUser("ada"));
        given("bob");
        givenAdminGroup(ada);

        assertThat(service.deactivate("bob", "ada").active()).isFalse();
    }

    /**
     * The guard asks whether the identity is active <em>now</em>, which is what keeps a
     * deactivation idempotent on an administrator that is already closed: repeating it takes
     * no recovery route away, so there is nothing to refuse — even when this is the only
     * administrator there is. Without that clause the vacuous "every active administrator is
     * this one" would refuse it.
     */
    @Test
    void deactivatingAnAlreadyInactiveAdministratorIsAllowedEvenAsTheOnlyOne() {
        ScimUser ada = users.given(ScimIdentities.inactiveUser("ada"));
        givenAdminGroup(ada);

        assertThat(service.deactivate("ada", "zoe").active()).isFalse();
    }

    /**
     * The reason this is a use case and not a column write: closing an identity that is
     * signed in somewhere has to reach that session, or the decision does not take effect
     * until the session expires on its own.
     */
    @Test
    void deactivatingEndsTheSessionsTheIdentityAlreadyHolds() {
        ScimUser bob = given("bob");
        sessions.open(bob.id(), "session-1");
        sessions.open(bob.id(), "session-2");

        service.deactivate("bob", "ada");
        transaction.commit();

        assertThat(sessions.sessionsOf(bob.id())).isEmpty();
    }

    /**
     * The ordering the deactivation promises: the revocation is arranged, not performed,
     * while the transaction is open. Anything that reads the sessions before the commit
     * still finds them, which is what makes a rollback able to leave nothing behind.
     */
    @Test
    void deactivatingRevokesNothingUntilTheTransactionCommits() {
        ScimUser bob = given("bob");
        sessions.open(bob.id(), "session-1");

        service.deactivate("bob", "ada");

        assertThat(sessions.revocations()).isEmpty();
        assertThat(sessions.sessionsOf(bob.id())).containsExactly("session-1");
        assertThat(transaction.pending()).isEqualTo(1);

        transaction.commit();

        assertThat(sessions.revocations()).containsExactly(bob.id());
        assertThat(sessions.sessionsOf(bob.id())).isEmpty();
    }

    /**
     * Redis is not in the transaction, so a revocation performed before the commit could not
     * be taken back by a rollback: the identity would read active while its holder was
     * signed out, with nothing recording why. Deferring the revocation is what makes a
     * failed commit leave both halves untouched.
     */
    @Test
    void aDeactivationWhoseTransactionRollsBackRevokesNothing() {
        ScimUser bob = given("bob");
        sessions.open(bob.id(), "session-1");

        service.deactivate("bob", "ada");
        transaction.rollback();

        assertThat(sessions.revocations()).isEmpty();
        assertThat(sessions.sessionsOf(bob.id())).containsExactly("session-1");
    }

    /** Only that identity's. A deactivation is about one identity, and so is its blast radius. */
    @Test
    void deactivatingLeavesEveryOtherIdentitySignedIn() {
        ScimUser bob = given("bob");
        ScimUser zoe = given("zoe");
        sessions.open(bob.id(), "session-1");
        sessions.open(zoe.id(), "session-2");

        service.deactivate("bob", "ada");
        transaction.commit();

        assertThat(sessions.sessionsOf(zoe.id())).containsExactly("session-2");
    }

    /**
     * An identity already closed is still asked to give up its sessions. Nothing guarantees
     * the earlier deactivation revoked anything — it may predate this behaviour, or have
     * been written straight into the database — and a second deactivation is how an
     * administrator acts on that doubt.
     */
    @Test
    void deactivatingAnAlreadyInactiveIdentityStillEndsItsSessions() {
        ScimUser bob = users.given(ScimIdentities.inactiveUser("bob"));
        sessions.open(bob.id(), "session-1");

        service.deactivate("bob", "ada");
        transaction.commit();

        assertThat(sessions.sessionsOf(bob.id())).isEmpty();
    }

    /**
     * Every refusal leaves the store and the sessions exactly as it found them: the checks
     * all run before anything is written or ended.
     */
    @Test
    void aRefusedDeactivationEndsNoSessionsAndChangesNothing() {
        ScimUser recovery = givenBootstrapAdmin();
        ScimUser ada = given("ada");
        // Ada alone in the Admin group, so she IS the last active administrator; the
        // recovery identity is protected by its marker rather than by membership.
        givenAdminGroup(ada);
        sessions.open(ada.id(), "session-1");
        sessions.open(recovery.id(), "session-2");
        int writesBefore = users.writes();
        long adaVersionBefore = users.require("ada").version();

        // Self, the reserved recovery identity, and the last active administrator.
        assertThatThrownBy(() -> service.deactivate("ada", "ada"))
                .isInstanceOf(UnsafeIdentityChangeException.class);
        assertThatThrownBy(() -> service.deactivate(BOOTSTRAP, "ada"))
                .isInstanceOf(UnsafeIdentityChangeException.class);
        assertThatThrownBy(() -> service.deactivate("ada", "zoe"))
                .isInstanceOf(UnsafeIdentityChangeException.class);

        assertThat(sessions.revocations()).isEmpty();
        assertThat(transaction.pending()).isZero();
        assertThat(sessions.sessionsOf(ada.id())).containsExactly("session-1");
        assertThat(sessions.sessionsOf(recovery.id())).containsExactly("session-2");
        assertThat(users.writes()).isEqualTo(writesBefore);
        assertThat(users.require("ada").profile().active()).isTrue();
        assertThat(users.require(BOOTSTRAP).profile().active()).isTrue();
        assertThat(users.require("ada").version()).isEqualTo(adaVersionBefore);
    }

    @Test
    void deactivatingAnIdentitySignedInNowhereIsNotAFailure() {
        given("bob");

        assertThat(service.deactivate("bob", "ada").active()).isFalse();
    }

    // Activating

    @Test
    void activatingReopensTheIdentity() {
        users.given(ScimIdentities.inactiveUser("bob"));

        assertThat(service.activate("bob", BOOTSTRAP).active()).isTrue();
        assertThat(users.require("bob").profile().active()).isTrue();
    }

    /**
     * Activating is not the inverse of deactivating. Reopening an identity says it may sign
     * in again, and a session is not something an administrator hands back.
     */
    @Test
    void activatingTouchesNoSessions() {
        ScimUser bob = users.given(ScimIdentities.inactiveUser("bob"));
        sessions.open(bob.id(), "session-1");

        service.activate("bob", BOOTSTRAP);

        assertThat(sessions.revocations()).isEmpty();
        assertThat(sessions.sessionsOf(bob.id())).containsExactly("session-1");
    }

    /**
     * The decision the user asked for: restoring access is not a finding that the failed
     * logins did not happen, so the lockout survives and needs its own call.
     */
    @Test
    void activatingDoesNotLiftALockout() {
        users.given(lockedAndInactive("bob"));

        IdentitySummary activated = service.activate("bob", BOOTSTRAP);

        assertThat(activated.active()).isTrue();
        assertThat(activated.locked()).isTrue();
        assertThat(users.require("bob").login().failedLoginAttempts()).isEqualTo(3);
    }

    @Test
    void activatingAnAlreadyActiveIdentityWritesNothing() {
        given("bob");
        int before = users.writes();

        assertThat(service.activate("bob", BOOTSTRAP).active()).isTrue();
        assertThat(users.writes()).isEqualTo(before);
    }

    // Unlocking

    @Test
    void unlockingEndsTheLockoutAndTheFailureRun() {
        givenLocked("bob");

        IdentitySummary unlocked = service.unlock("bob", BOOTSTRAP);

        assertThat(unlocked.locked()).isFalse();
        assertThat(users.require("bob").login().lockedAt()).isNull();
        assertThat(users.require("bob").login().failedLoginAttempts()).isZero();
    }

    /** The converse of the decision above: unlocking is not a reinstatement. */
    @Test
    void unlockingDoesNotActivateAnInactiveIdentity() {
        users.given(lockedAndInactive("bob"));

        IdentitySummary unlocked = service.unlock("bob", BOOTSTRAP);

        assertThat(unlocked.locked()).isFalse();
        assertThat(unlocked.active()).isFalse();
        assertThat(users.require("bob").profile().active()).isFalse();
    }

    @Test
    void unlockingAnIdentityThatIsNotLockedWritesNothing() {
        given("bob");
        int before = users.writes();

        assertThat(service.unlock("bob", BOOTSTRAP).locked()).isFalse();
        assertThat(users.writes()).isEqualTo(before);
    }

    /**
     * Unlocking clears the failure run, which is not a SCIM attribute — so it must not move
     * the resource's ETag either.
     */
    @Test
    void unlockingDoesNotAdvanceTheResourceVersion() {
        ScimUser bob = givenLocked("bob");

        service.unlock("bob", BOOTSTRAP);

        assertThat(users.require("bob").version()).isEqualTo(bob.version());
    }

    @Test
    void unlockingTouchesNoSessions() {
        ScimUser bob = givenLocked("bob");
        sessions.open(bob.id(), "session-1");

        service.unlock("bob", BOOTSTRAP);

        assertThat(sessions.revocations()).isEmpty();
        assertThat(sessions.sessionsOf(bob.id())).containsExactly("session-1");
    }

    /**
     * Time is not a lift, so an identity locked long ago is still locked and the unlock is
     * what clears it — both the recorded instant and the run behind it.
     */
    @Test
    void unlockingClearsALockoutHoweverLongItHasStood() {
        givenLocked("bob");
        clock.advanceBy(A_LONG_TIME);

        service.unlock("bob", BOOTSTRAP);

        assertThat(users.require("bob").login().lockedAt()).isNull();
        assertThat(users.require("bob").login().failedLoginAttempts()).isZero();
    }

    // Unknown identities

    @Test
    void refusesToActOnAnIdentityThatDoesNotExist() {
        assertThatThrownBy(() -> service.deactivate("nobody", "ada"))
                .isInstanceOf(UnknownIdentityException.class);
        assertThatThrownBy(() -> service.activate("nobody", BOOTSTRAP))
                .isInstanceOf(UnknownIdentityException.class);
        assertThatThrownBy(() -> service.unlock("nobody", BOOTSTRAP))
                .isInstanceOf(UnknownIdentityException.class);
        assertThat(users.findByNormalizedUserName(NormalizedUserName.of("nobody"))).isEmpty();
    }

    /**
     * And says nothing about the value it could not find. A {@code userName} is half a
     * credential, and an exception message is the shortest path into a log line.
     */
    @Test
    void anUnknownIdentityRefusalDoesNotNameTheSubmittedUserName() {
        assertThatThrownBy(() -> service.deactivate("nobody", "ada"))
                .isInstanceOf(UnknownIdentityException.class)
                .hasMessageNotContaining("nobody");
    }

    // What the trail is told

    /**
     * Both parties by stable id: the administrator who acted, and the identity acted on. The
     * administrator's userName is what the caller passes in and what must not be what gets
     * recorded.
     */
    @Test
    void deactivatingIsRecordedNamingBothPartiesByStableId() {
        ScimUser ada = given("ada");
        ScimUser recovery = given(BOOTSTRAP);
        ScimUser bob = given("bob");
        givenAdminGroup(ada, recovery);

        service.deactivate("bob", BOOTSTRAP);

        assertThat(audit.recorded()).containsExactly(new Recorded(
                AuditOperation.ACCOUNT_DISABLE, recovery.id(), bob.id(), null));
    }

    @Test
    void activatingIsRecordedNamingBothPartiesByStableId() {
        ScimUser recovery = given(BOOTSTRAP);
        ScimUser bob = users.given(ScimIdentities.inactiveUser("bob"));

        service.activate("bob", BOOTSTRAP);

        assertThat(audit.recorded()).containsExactly(new Recorded(
                AuditOperation.ACCOUNT_ENABLE, recovery.id(), bob.id(), null));
    }

    @Test
    void unlockingIsRecordedAsALiftCausedByAnAdministrator() {
        ScimUser recovery = given(BOOTSTRAP);
        ScimUser bob = givenLocked("bob");

        service.unlock("bob", BOOTSTRAP);

        assertThat(audit.recorded()).containsExactly(new Recorded(
                AuditOperation.LOCKOUT_LIFT, recovery.id(), bob.id(), null));
    }

    /**
     * An unlock that writes nothing — the identity is serving no lockout — is still an action
     * an administrator took, so it is still recorded. The row is the evidence that someone
     * looked.
     */
    @Test
    void anIdempotentUnlockIsStillRecorded() {
        given(BOOTSTRAP);
        given("bob");

        service.unlock("bob", BOOTSTRAP);

        assertThat(audit.of(AuditOperation.LOCKOUT_LIFT)).hasSize(1);
    }

    /**
     * Each refusal is recorded with its closed-set reason and both parties' ids. This is the
     * behaviour that is genuinely new: the log line deliberately names neither party, so a
     * run of attempts to close the recovery identity is a signal only the trail can carry.
     */
    @Test
    void eachRefusedDeactivationIsRecordedWithItsReasonAndBothParties() {
        ScimUser recovery = givenBootstrapAdmin();
        ScimUser ada = given("ada");
        givenAdminGroup(ada);

        assertThatThrownBy(() -> service.deactivate("ada", "ada"))
                .isInstanceOf(UnsafeIdentityChangeException.class);
        assertThatThrownBy(() -> service.deactivate(BOOTSTRAP, "ada"))
                .isInstanceOf(UnsafeIdentityChangeException.class);
        assertThatThrownBy(() -> service.deactivate("ada", "zoe"))
                .isInstanceOf(UnsafeIdentityChangeException.class);

        assertThat(audit.recorded()).containsExactly(
                new Recorded(
                        AuditOperation.ACCOUNT_DISABLE,
                        ada.id(),
                        ada.id(),
                        AuditAdministrativeRefusal.SELF_DISABLE.name()),
                new Recorded(
                        AuditOperation.ACCOUNT_DISABLE,
                        ada.id(),
                        recovery.id(),
                        AuditAdministrativeRefusal.PROTECTED_RESOURCE.name()),
                // The actor's own name resolves to nobody here, which is recorded as no
                // actor rather than as the name.
                new Recorded(
                        AuditOperation.ACCOUNT_DISABLE,
                        null,
                        ada.id(),
                        AuditAdministrativeRefusal.LAST_ENABLED_ADMINISTRATOR.name()));
    }

    /**
     * A refusal is recorded as a refusal and never as a change: the detail carries the
     * reason, where a change carries none.
     */
    @Test
    void aRefusedDeactivationIsNotRecordedAsAChange() {
        given("ada");

        assertThatThrownBy(() -> service.deactivate("ada", "ada"))
                .isInstanceOf(UnsafeIdentityChangeException.class);

        assertThat(audit.recorded())
                .singleElement()
                .satisfies(event -> assertThat(event.detail())
                        .isEqualTo(AuditAdministrativeRefusal.SELF_DISABLE.name()));
    }

    /**
     * An administrator whose own resource cannot be resolved — renamed between
     * authenticating and acting — still produces an event, with no actor rather than with
     * the name.
     */
    @Test
    void anUnresolvableAdministratorIsRecordedAsNoActorRatherThanAName() {
        ScimUser bob = given("bob");

        service.activate("bob", "vanished");

        assertThat(audit.recorded()).containsExactly(new Recorded(
                AuditOperation.ACCOUNT_ENABLE, null, bob.id(), null));
    }

    /**
     * Each administrative write reports itself to the log stream as a named action with a
     * success outcome, separately from the audit row. The two serve different readers — an
     * operator watching for unexpected activity, and an auditor asking who changed what — so
     * a change that produced the row but no record, or the record but no row, is a defect in
     * one of them rather than a duplication.
     *
     * <p>Asserted here rather than left to review because it is the only proof that the call
     * is made at all: a removed log call changes nothing a test that reads only the returned
     * summary or the recorded event can see.
     */
    @Test
    void eachAdministrativeWriteReportsItsActionAndSuccessToTheLogStream() {
        ScimUser recovery = given(BOOTSTRAP);
        ScimUser ada = given("ada");
        givenAdminGroup(recovery, ada);
        givenLocked("bob");

        try (CapturedLog captured = CapturedLog.attach()) {
            service.deactivate("bob", BOOTSTRAP);
            service.activate("bob", BOOTSTRAP);
            service.unlock("bob", BOOTSTRAP);

            assertThat(List.of("identity.deactivate", "identity.activate", "identity.unlock"))
                    .allSatisfy(action -> assertThat(
                                    captured.withAction(Level.INFO, LogEvent.ACTION, action))
                            .singleElement()
                            .satisfies(record -> assertThat(CapturedLog.fields(record))
                                    .containsEntry(LogEvent.OUTCOME, LogEvent.SUCCESS)));
        }
    }

    /**
     * A refusal reports the action, a failure outcome and the closed-set reason — and
     * nothing that names either party, which is what the audit trail carries instead.
     */
    @Test
    void aRefusedWriteReportsItsActionReasonAndFailureToTheLogStream() {
        given("ada");

        try (CapturedLog captured = CapturedLog.attach()) {
            assertThatThrownBy(() -> service.deactivate("ada", "ada"))
                    .isInstanceOf(UnsafeIdentityChangeException.class);

            assertThat(captured.withAction(Level.WARN, LogEvent.ACTION, "identity.deactivate"))
                    .singleElement()
                    .satisfies(record -> assertThat(CapturedLog.fields(record))
                            .containsEntry(LogEvent.OUTCOME, LogEvent.FAILURE)
                            .containsEntry(
                                    LogEvent.REASON,
                                    AuditAdministrativeRefusal.SELF_DISABLE.name()));
        }
    }

    private ScimUser given(String userName) {
        return users.given(ScimIdentities.user(userName));
    }

    private ScimUser givenLocked(String userName) {
        return users.given(ScimIdentities.userWithLoginState(
                userName, new ScimLoginState("hash", 3, NOW)));
    }

    /**
     * Locked <em>and</em> deactivated, which the shared fixture has no single factory for
     * because the two states are independent — the point of the pair of tests below is that
     * lifting one leaves the other standing.
     */
    private static ScimUser lockedAndInactive(String userName) {
        return new ScimUser(
                UUID.randomUUID(),
                ScimIdentities.profile(userName, false),
                new ScimLoginState("hash", 3, NOW),
                null,
                ScimUser.INITIAL_VERSION,
                NOW,
                NOW);
    }

    /**
     * The reservation is applied through the port, because production has no other way to
     * produce one: there is deliberately no factory that mints a reserved resource.
     */
    private ScimUser givenBootstrapAdmin() {
        return users.createReserved(
                ScimIdentities.user(BOOTSTRAP), ReservedResourceName.BOOTSTRAP_ADMIN);
    }

    private ScimGroup givenAdminGroup(ScimUser... members) {
        return groups.createReserved(
                ScimIdentities.group("Admins", members), ReservedResourceName.ADMIN_GROUP);
    }
}
