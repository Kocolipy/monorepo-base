package com.example.backend.audit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.backend.audit.domain.AuditEvent;
import com.example.backend.audit.domain.AuditEventRepository;
import com.example.backend.audit.domain.AuditLockoutLift;
import com.example.backend.audit.domain.AuditOperation;
import com.example.backend.audit.domain.AuditOutcome;
import com.example.backend.audit.domain.AuditRefusalReason;
import com.example.backend.audit.domain.AuditRequest;
import com.example.backend.audit.domain.AuditRequestContext;
import com.example.backend.audit.domain.AuditTrail;
import com.example.backend.audit.domain.OperationalAlerts;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
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
        assertThat(event.resourceType()).isEqualTo("Account");
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

    @Test
    void theTwoLockoutLiftsAreOneOperationCarryingTheirCause() {
        trail.recordLockoutLiftedByExpiry(SUBJECT);
        trail.recordLockoutLiftedByUnlock(ACTOR, SUBJECT);

        assertThat(events.appended).hasSize(2);
        assertThat(events.appended).allSatisfy(event ->
                assertThat(event.operation()).isEqualTo(AuditOperation.LOCKOUT_LIFT));
        assertThat(events.appended.get(0).errorCode())
                .isEqualTo(AuditLockoutLift.EXPIRY.name());
        assertThat(events.appended.get(0).actorId()).isNull();
        assertThat(events.appended.get(1).errorCode())
                .isEqualTo(AuditLockoutLift.UNLOCK.name());
        assertThat(events.appended.get(1).actorId()).isEqualTo(ACTOR);
        assertThat(events.appended.get(1).changedPaths())
                .containsExactly("failedLoginAttempts", "lockedUntil");
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
            assertThat(event.changedPaths()).containsExactly("enabled");
        });
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

        assertThat(alerts.raised).isEmpty();
    }

    @Test
    void aFailOpenAppendRaisesAnAlertAndDoesNotPropagate() {
        events.failWith(new IllegalStateException("insert refused"));

        trail.recordLoginFailure(SUBJECT, AuditRefusalReason.BAD_CREDENTIALS);
        trail.recordLockoutSet(SUBJECT);
        trail.recordLockoutLiftedByExpiry(SUBJECT);

        assertThat(alerts.raised).containsExactly(
                AuditOperation.LOGIN_FAILURE,
                AuditOperation.LOCKOUT_SET,
                AuditOperation.LOCKOUT_LIFT);
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
