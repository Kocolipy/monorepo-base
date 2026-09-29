package com.example.backend.audit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.backend.audit.domain.AuditAdministrativeRefusal;
import com.example.backend.audit.domain.AuditEvent;
import com.example.backend.audit.domain.AuditEventRepository;
import com.example.backend.audit.domain.AuditGroupAttribute;
import com.example.backend.audit.domain.AuditOperation;
import com.example.backend.audit.domain.AuditOutcome;
import com.example.backend.audit.domain.AuditRefusalReason;
import com.example.backend.audit.domain.AuditRequest;
import com.example.backend.audit.domain.AuditRequestContext;
import com.example.backend.audit.domain.AuditScimRefusal;
import com.example.backend.audit.domain.AuditTrail;
import com.example.backend.audit.domain.OperationalAlerts;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * What the trail records, and what it does when it cannot record.
 *
 * <p>The two failure semantics are the point of this class. A fail-closed append
 * must propagate — that is the whole mechanism by which a mutation it could not
 * record is rolled back — and a fail-open one must not, having raised an alert
 * instead. Both are asserted against the same forced failure, so the difference
 * cannot be an accident of which exception was thrown.
 */
class AuditTrailServiceTests {

    private static final Instant NOW = Instant.parse("2026-09-25T07:00:00Z");

    private static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID SUBJECT = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID GROUP = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final RecordingRepository events = new RecordingRepository();
    private final RecordingAlerts alerts = new RecordingAlerts();
    private final AuditRequestContext requests =
            () -> new AuditRequest("POST", "/api/admin/accounts/{username}/disable", "req-1");

    private final AuditTrail trail = new AuditTrailService(
            events,
            requests,
            alerts,
            Clock.fixed(NOW, ZoneOffset.UTC),
            NO_TRANSACTION_MANAGER);

    // The shape of what is recorded

    @Test
    void anAcceptedLoginIsRecordedAgainstTheAccountsStableId() {
        trail.recordLoginSuccess(SUBJECT);

        AuditEvent event = events.only();
        assertThat(event.operation()).isEqualTo(AuditOperation.LOGIN_SUCCESS);
        assertThat(event.outcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(event.actorId()).isEqualTo(SUBJECT);
        assertThat(event.subjectId()).isEqualTo(SUBJECT);
        assertThat(event.resourceId()).isEqualTo(SUBJECT);
        assertThat(event.resourceType()).isEqualTo("User");
        assertThat(event.statusClass()).isEqualTo("ok");
        assertThat(event.errorCode()).isNull();
        assertThat(event.occurredAt()).isEqualTo(NOW);
        assertThat(event.changedPaths()).isEmpty();
        assertThat(event.httpMethod()).isEqualTo("POST");
        assertThat(event.requestId()).isEqualTo("req-1");
        assertThat(event.id()).isNotNull();
    }

    @Test
    void aLogoutIsRecordedWithTheRequestThatEndedTheSession() {
        trail.recordLogout(SUBJECT);

        AuditEvent event = events.only();
        assertThat(event.operation()).isEqualTo(AuditOperation.LOGOUT);
        assertThat(event.actorId()).isEqualTo(SUBJECT);
        assertThat(event.subjectId()).isEqualTo(SUBJECT);
        assertThat(event.changedPaths()).isEmpty();
        assertThat(event.occurredAt()).isEqualTo(NOW);
        assertThat(event.httpMethod()).isEqualTo("POST");
        assertThat(event.httpPath()).isEqualTo("/api/admin/accounts/{username}/disable");
        assertThat(event.requestId()).isEqualTo("req-1");
    }

    /** Two events recorded in one turn get distinct identities. */
    @Test
    void everyEventCarriesItsOwnIdentity() {
        trail.recordLogout(SUBJECT);
        trail.recordLoginSuccess(SUBJECT);

        assertThat(events.appended).extracting(AuditEvent::id).doesNotContainNull();
        assertThat(events.appended.get(0).id()).isNotEqualTo(events.appended.get(1).id());
    }

    @Test
    void aRefusedLoginNamesTheReasonAndNoActor() {
        trail.recordLoginFailure(SUBJECT, AuditRefusalReason.BAD_CREDENTIALS);

        AuditEvent event = events.only();
        assertThat(event.operation()).isEqualTo(AuditOperation.LOGIN_FAILURE);
        assertThat(event.outcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(event.actorId()).isNull();
        assertThat(event.subjectId()).isEqualTo(SUBJECT);
        assertThat(event.errorCode()).isEqualTo("BAD_CREDENTIALS");
        assertThat(event.statusClass()).isEqualTo("client_error");
        assertThat(event.changedPaths()).containsExactly("failedLoginAttempts");
    }

    /**
     * The case that must not record the submitted username: no account carries it,
     * so there is nothing to name and the subject stays absent.
     */
    @Test
    void aRefusedLoginAgainstAnUnknownNameRecordsNoSubjectAtAll() {
        trail.recordLoginFailure(null, AuditRefusalReason.UNKNOWN_ACCOUNT);

        AuditEvent event = events.only();
        assertThat(event.subjectId()).isNull();
        assertThat(event.resourceId()).isNull();
        assertThat(event.errorCode()).isEqualTo("UNKNOWN_ACCOUNT");
    }

    /**
     * There is one lift and so no cause to carry: an unlock is the only way a
     * lockout ends, and the event names the administrator who performed it rather
     * than which kind of lift it was.
     */
    @Test
    void theOnlyLockoutLiftNamesItsAdministratorAndCarriesNoCause() {
        trail.recordLockoutLiftedByUnlock(ACTOR, SUBJECT);

        AuditEvent event = events.only();
        assertThat(event.operation()).isEqualTo(AuditOperation.LOCKOUT_LIFT);
        assertThat(event.errorCode()).isNull();
        assertThat(event.actorId()).isEqualTo(ACTOR);
        assertThat(event.subjectId()).isEqualTo(SUBJECT);
        assertThat(event.changedPaths()).containsExactly("failedLoginAttempts", "lockedAt");
    }

    @Test
    void anAdministrativeChangeNamesBothTheActorAndTheSubject() {
        trail.recordAccountDisabled(ACTOR, SUBJECT);
        trail.recordAccountEnabled(ACTOR, SUBJECT);

        assertThat(events.appended).extracting(AuditEvent::operation).containsExactly(
                AuditOperation.ACCOUNT_DISABLE, AuditOperation.ACCOUNT_ENABLE);
        assertThat(events.appended).allSatisfy(event -> {
            assertThat(event.actorId()).isEqualTo(ACTOR);
            assertThat(event.subjectId()).isEqualTo(SUBJECT);
            assertThat(event.changedPaths()).containsExactly("active");
        });
    }

    /**
     * Authentication, lockout, administrative standing and provisioning all name one
     * resource type now, because they act on one resource: the account aggregate is gone and
     * a SCIM User owns the profile and the authentication state together. That collapse is
     * the assertion — an administrator reading the trail groups a person's whole history
     * under one type and one id.
     */
    @Test
    void everyEventAboutTheLoginIdentityNamesTheScimUserResourceType() {
        trail.recordLoginSuccess(SUBJECT);
        trail.recordLoginFailure(SUBJECT, AuditRefusalReason.BAD_CREDENTIALS);
        trail.recordLogout(SUBJECT);
        trail.recordLockoutSet(SUBJECT);
        trail.recordLockoutLiftedByUnlock(ACTOR, SUBJECT);
        trail.recordAccountDisabled(ACTOR, SUBJECT);
        trail.recordAccountEnabled(ACTOR, SUBJECT);
        trail.recordScimUserCreated(ACTOR, SUBJECT);
        trail.recordScimUsersListed(ACTOR);

        assertThat(events.appended)
                .extracting(AuditEvent::resourceType)
                .containsOnly(AuditEvent.USER_RESOURCE_TYPE);
    }

    /**
     * The route template reaches the event and the resolved path never does — the
     * context this test supplies returns a template, and the recorded value is it
     * verbatim. What guarantees the context cannot return a resolved path is
     * {@code HttpAuditRequestContextTests}.
     */
    @Test
    void theRequestIsRecordedAsItsRouteTemplate() {
        trail.recordAccountDisabled(ACTOR, SUBJECT);

        AuditEvent event = events.only();
        assertThat(event.httpMethod()).isEqualTo("POST");
        assertThat(event.httpPath()).isEqualTo("/api/admin/accounts/{username}/disable");
        assertThat(event.requestId()).isEqualTo("req-1");
    }

    // The Group and seeding vocabulary

    /**
     * A Group event names the Group's own id as both subject and resource, and the connector
     * as the actor. Which User was added or removed is deliberately absent: a membership has
     * no id of its own, and naming the other party would put a second identity's history
     * inside this event.
     */
    @Test
    void aGroupEventNamesTheGroupAndTheActingConnector() {
        trail.recordScimGroupCreated(ACTOR, GROUP);

        AuditEvent event = events.only();
        assertThat(event.operation()).isEqualTo(AuditOperation.SCIM_GROUP_CREATE);
        assertThat(event.outcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(event.resourceType()).isEqualTo(AuditEvent.GROUP_RESOURCE_TYPE);
        assertThat(event.actorId()).isEqualTo(ACTOR);
        assertThat(event.subjectId()).isEqualTo(GROUP);
        assertThat(event.resourceId()).isEqualTo(GROUP);
        assertThat(event.statusClass()).isEqualTo("ok");
        assertThat(event.errorCode()).isNull();
    }

    /**
     * A refused create names no subject: the Group was not created, and the existing resource
     * that caused the refusal is not what the event is about.
     */
    @Test
    void aRefusedGroupCreateNamesTheReasonAndNoSubject() {
        trail.recordScimGroupCreateRejected(ACTOR, AuditScimRefusal.UNIQUENESS);

        AuditEvent event = events.only();
        assertThat(event.outcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(event.subjectId()).isNull();
        assertThat(event.resourceId()).isNull();
        assertThat(event.errorCode()).isEqualTo("UNIQUENESS");
        assertThat(event.statusClass()).isEqualTo("client_error");
    }

    /**
     * A refused WRITE does name the Group, unlike a refused create: the resource exists, the
     * write was aimed at it, and an attempt to provision the recovery authority away is
     * exactly what an administrator searches for by that Group's id.
     */
    @Test
    void aRefusedGroupWriteNamesTheGroupItWasAimedAt() {
        trail.recordScimGroupWriteRejected(ACTOR, GROUP, AuditScimRefusal.MUTABILITY);

        AuditEvent event = events.only();
        assertThat(event.operation()).isEqualTo(AuditOperation.SCIM_GROUP_REPLACE);
        assertThat(event.outcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(event.subjectId()).isEqualTo(GROUP);
        assertThat(event.errorCode()).isEqualTo("MUTABILITY");
    }

    /**
     * The changed attributes become this slice's own path names, in a fixed order — so a
     * caller cannot assemble the recorded list and a reader can filter on it.
     */
    @Test
    void aGroupReplacementRecordsWhichAttributesMovedAsThisSlicesOwnPaths() {
        trail.recordScimGroupReplaced(
                ACTOR,
                GROUP,
                Set.of(AuditGroupAttribute.MEMBERS, AuditGroupAttribute.DISPLAY_NAME));

        assertThat(events.only().changedPaths()).containsExactly("displayName", "members");
    }

    /**
     * A User deletion names the deleted User and changes no attribute path: the whole
     * resource went, which the operation says on its own.
     */
    @Test
    void aUserDeletionNamesTheDeletedUserAndRecordsNoPaths() {
        trail.recordScimUserDeleted(ACTOR, SUBJECT);

        AuditEvent event = events.only();
        assertThat(event.operation()).isEqualTo(AuditOperation.SCIM_USER_DELETE);
        assertThat(event.outcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(event.actorId()).isEqualTo(ACTOR);
        assertThat(event.subjectId()).isEqualTo(SUBJECT);
        assertThat(event.statusClass()).isEqualTo("ok");
        assertThat(event.errorCode()).isNull();
        assertThat(event.changedPaths()).isEmpty();
    }

    /** A refused deletion names the User it was aimed at and why it was refused. */
    @Test
    void aRefusedUserDeletionNamesTheUserAndTheRefusal() {
        trail.recordScimUserDeleteRejected(ACTOR, SUBJECT, AuditScimRefusal.MUTABILITY);

        AuditEvent event = events.only();
        assertThat(event.operation()).isEqualTo(AuditOperation.SCIM_USER_DELETE);
        assertThat(event.outcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(event.subjectId()).isEqualTo(SUBJECT);
        assertThat(event.statusClass()).isEqualTo("client_error");
        assertThat(event.errorCode()).isEqualTo("MUTABILITY");
        assertThat(event.changedPaths()).isEmpty();
    }

    /**
     * The deletion's append is fail-closed, so a deletion the trail cannot record rolls
     * back; its refusal's append is fail-open, as every refused write's is.
     */
    @Test
    void aUserDeletionFailsClosedAndItsRefusalFailsOpen() {
        events.failWith(new IllegalStateException("insert refused"));

        assertThatThrownBy(() -> trail.recordScimUserDeleted(ACTOR, SUBJECT))
                .isInstanceOf(IllegalStateException.class);
        trail.recordScimUserDeleteRejected(ACTOR, SUBJECT, AuditScimRefusal.MUTABILITY);

        assertThat(alerts.raised).containsExactly(AuditOperation.SCIM_USER_DELETE);
    }

    @Test
    void aGroupReplacementThatMovedNothingRecordsNoChangedPath() {
        trail.recordScimGroupReplaced(ACTOR, GROUP, Set.of());

        assertThat(events.only().changedPaths()).isEmpty();
    }

    /**
     * Seeding has no actor, because nobody acted: it is the deployment establishing its own
     * recovery path at startup, and inventing an actor would make the trail claim somebody
     * did this. The seeded resource is the subject, so the event reads as "this identity came
     * into existence".
     */
    @Test
    void aSeededReservedResourceNamesNoActorAndTypesItselfByWhatWasSeeded() {
        trail.recordReservedResourceSeeded(SUBJECT, false);
        trail.recordReservedResourceSeeded(GROUP, true);

        assertThat(events.appended).allSatisfy(event -> {
            assertThat(event.operation()).isEqualTo(AuditOperation.SCIM_RESOURCE_SEED);
            assertThat(event.actorId()).isNull();
        });
        assertThat(events.appended.get(0).resourceType())
                .isEqualTo(AuditEvent.USER_RESOURCE_TYPE);
        assertThat(events.appended.get(0).subjectId()).isEqualTo(SUBJECT);
        assertThat(events.appended.get(1).resourceType())
                .isEqualTo(AuditEvent.GROUP_RESOURCE_TYPE);
        assertThat(events.appended.get(1).subjectId()).isEqualTo(GROUP);
    }

    /**
     * A restored Admin-group membership is a seeding event about the Group, with no actor, and
     * records its changed path in the same vocabulary a connector's membership change uses — so a
     * reader filtering the trail on {@code members} finds the restore beside the writes.
     */
    @Test
    void aRestoredReservedMembershipIsASeedEventOnTheGroupNamingTheMembersPath() {
        trail.recordReservedMembershipRestored(GROUP, SUBJECT);

        AuditEvent event = events.only();
        assertThat(event.operation()).isEqualTo(AuditOperation.SCIM_RESOURCE_SEED);
        assertThat(event.outcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(event.actorId()).isNull();
        assertThat(event.subjectId()).isEqualTo(GROUP);
        assertThat(event.resourceType()).isEqualTo(AuditEvent.GROUP_RESOURCE_TYPE);
        assertThat(event.changedPaths()).containsExactly("members");
    }

    /**
     * A refused administrative change is recorded as a failure naming the reason, and names
     * both parties: the administrator who asked and the identity it was aimed at.
     */
    @Test
    void aRefusedAdministrativeChangeNamesBothPartiesAndTheReason() {
        trail.recordAdministrativeChangeRefused(
                ACTOR, SUBJECT, AuditAdministrativeRefusal.LAST_ENABLED_ADMINISTRATOR);

        AuditEvent event = events.only();
        assertThat(event.outcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(event.actorId()).isEqualTo(ACTOR);
        assertThat(event.subjectId()).isEqualTo(SUBJECT);
        assertThat(event.errorCode()).isEqualTo("LAST_ENABLED_ADMINISTRATOR");
        assertThat(event.statusClass()).isEqualTo("client_error");
        assertThat(event.changedPaths()).isEmpty();
    }

    // The two failure semantics

    @Test
    void aFailClosedAppendPropagatesSoTheMutationRollsBack() {
        events.failWith(new IllegalStateException("insert refused"));

        assertThatThrownBy(() -> trail.recordAccountDisabled(ACTOR, SUBJECT))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> trail.recordLoginSuccess(SUBJECT))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> trail.recordLogout(SUBJECT))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> trail.recordLockoutLiftedByUnlock(ACTOR, SUBJECT))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> trail.recordScimGroupCreated(ACTOR, GROUP))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> trail.recordScimGroupReplaced(
                        ACTOR, GROUP, Set.of(AuditGroupAttribute.MEMBERS)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> trail.recordScimGroupDeleted(ACTOR, GROUP))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> trail.recordScimGroupsListed(ACTOR))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> trail.recordReservedResourceSeeded(SUBJECT, false))
                .isInstanceOf(IllegalStateException.class);

        assertThat(alerts.raised).isEmpty();
    }

    @Test
    void aFailOpenAppendRaisesAnAlertAndDoesNotPropagate() {
        events.failWith(new IllegalStateException("insert refused"));

        trail.recordLoginFailure(SUBJECT, AuditRefusalReason.BAD_CREDENTIALS);
        trail.recordLockoutSet(SUBJECT);
        trail.recordScimUserCreateRejectedAsDuplicate(ACTOR);
        trail.recordAdministrativeChangeRefused(
                ACTOR, SUBJECT, AuditAdministrativeRefusal.SELF_DISABLE);
        trail.recordScimGroupCreateRejected(ACTOR, AuditScimRefusal.UNIQUENESS);
        trail.recordScimGroupWriteRejected(ACTOR, GROUP, AuditScimRefusal.MUTABILITY);

        assertThat(alerts.raised).containsExactly(
                AuditOperation.LOGIN_FAILURE,
                AuditOperation.LOCKOUT_SET,
                AuditOperation.SCIM_USER_CREATE,
                AuditOperation.ACCOUNT_DISABLE,
                AuditOperation.SCIM_GROUP_CREATE,
                AuditOperation.SCIM_GROUP_REPLACE);
    }

    @Test
    void anAlertNamesTheFailuresOwnTypeAndNothingElse() {
        events.failWith(new IllegalArgumentException("would name the submitted value"));

        trail.recordLoginFailure(SUBJECT, AuditRefusalReason.BAD_CREDENTIALS);

        assertThat(alerts.failures).containsExactly(IllegalArgumentException.class);
    }

    private static final class RecordingRepository implements AuditEventRepository {

        private final List<AuditEvent> appended = new ArrayList<>();
        private RuntimeException failure;

        void failWith(RuntimeException failure) {
            this.failure = failure;
        }

        AuditEvent only() {
            assertThat(appended).hasSize(1);
            return appended.get(0);
        }

        @Override
        public void append(AuditEvent event) {
            if (failure != null) {
                throw failure;
            }
            appended.add(event);
        }
    }

    private static final class RecordingAlerts implements OperationalAlerts {

        private final List<AuditOperation> raised = new ArrayList<>();
        private final List<Class<? extends Throwable>> failures = new ArrayList<>();

        @Override
        public void auditAppendFailed(
                AuditOperation operation, Class<? extends Throwable> failure) {
            raised.add(operation);
            failures.add(failure);
        }
    }

    /**
     * A transaction manager that starts and ends nothing. The service uses a
     * {@link org.springframework.transaction.support.TransactionTemplate} to run a
     * fail-open append in its own transaction; what this test is about is whether
     * the exception escapes, and a real manager would only add a database.
     */
    /**
     * A fail-open append runs in a transaction of its own, not the caller's.
     *
     * <p>The behavioural claim — the row outlives a caller that rolls back — is
     * asserted against real Postgres in
     * {@code AuditAppendOnlyIntegrationTests.aFailOpenAppendCommitsEvenWhenTheCallersTransactionRollsBack}.
     * That test cannot reach this constructor under mutation testing: the service is
     * a singleton built once while the Spring context boots, so PIT attributes the
     * constructor's coverage to whichever test method happened to trigger the boot
     * and runs only that one. Hence this unit-level assertion on the propagation the
     * template actually asks for — the only form in which the wiring is visible to a
     * mutation of the constructor.
     */
    @Test
    void aFailOpenAppendAsksForATransactionOfItsOwn() {
        RecordingTransactionManager transactions = new RecordingTransactionManager();
        AuditTrail isolated = new AuditTrailService(
                events, requests, alerts, Clock.fixed(NOW, ZoneOffset.UTC), transactions);

        isolated.recordLoginFailure(SUBJECT, AuditRefusalReason.BAD_CREDENTIALS);

        assertThat(transactions.definitions)
                .as("the fail-open append's transaction definitions")
                .singleElement()
                .satisfies(definition -> assertThat(definition.getPropagationBehavior())
                        .as("PROPAGATION_REQUIRES_NEW, so the caller's rollback cannot "
                                + "take the audit row with it")
                        .isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW));
    }

    /** Records the transaction definitions it is asked for, and does nothing else. */
    private static final class RecordingTransactionManager implements PlatformTransactionManager {

        private final List<TransactionDefinition> definitions = new ArrayList<>();

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            definitions.add(definition);
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }

    private static final PlatformTransactionManager NO_TRANSACTION_MANAGER =
            new PlatformTransactionManager() {

                @Override
                public TransactionStatus getTransaction(TransactionDefinition definition) {
                    return new SimpleTransactionStatus();
                }

                @Override
                public void commit(TransactionStatus status) {
                }

                @Override
                public void rollback(TransactionStatus status) {
                }
            };
}
