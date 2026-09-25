package com.example.backend.audit.application;

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
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The one way anything in this service records that something happened.
 *
 * <p>Every method below takes ids and members of closed sets, and <em>no public
 * method takes a {@code String}</em>. That is not a style preference: a username,
 * a password and a bearer value are all strings, so a boundary that admits no
 * string cannot be handed one by a caller who did not read the rule. The event's
 * own textual fields — the resource type, the status class, the error code, the
 * route template — are filled in here and by the request-context adapter, from
 * vocabularies this slice owns. {@code ArchitectureTest} holds the boundary to
 * that shape, and {@code semgrep/rules/service-security.yml} flags a call that
 * tries to pass a credential-named value through it.
 *
 * <h2>Two failure semantics, deliberately</h2>
 *
 * <p>An append is either <strong>fail-closed</strong> or <strong>fail-open with an
 * alert</strong>, and which one it is follows from what the triggering request was
 * going to return:
 *
 * <ul>
 *   <li><strong>Fail-closed</strong> for an operation that would otherwise
 *       succeed — an administrator's disable, enable or unlock, an accepted login,
 *       a logout. The append joins the caller's transaction and flushes, so an
 *       event that cannot be written rolls the mutation it was recording back. A
 *       change this service cannot account for does not happen.
 *   <li><strong>Fail-open with an alert</strong> for an event on a path that is
 *       already refusing the request — a rejected login and the lockout it may
 *       impose, and a lockout expiry, which nobody requested at all. Here the
 *       original outcome is a bare {@code 401}, and it must stay one: turning a
 *       refused login into a {@code 500} because the trail is unavailable would
 *       tell an attacker something about the state of the service, and would
 *       change the answer to a question that was already answered. The append
 *       runs in its own transaction so its rollback cannot take the caller's with
 *       it, and its failure raises an operational alert instead of propagating.
 * </ul>
 *
 * <p>See {@code /docs/adr/0004-audit-append-failure-semantics.md}.
 */
@Service
public class AuditTrailService implements AuditTrail {

    /** The lockout columns, as the paths an event reports as changed. */
    private static final List<String> LOCKOUT_PATHS =
            List.of("failedLoginAttempts", "lockedUntil");

    /** The administrative standing column. */
    private static final List<String> ENABLED_PATHS = List.of("enabled");

    /** A rejected login lengthens the failure run and nothing else. */
    private static final List<String> FAILURE_RUN_PATHS = List.of("failedLoginAttempts");

    private final AuditEventRepository events;
    private final AuditRequestContext requests;
    private final OperationalAlerts alerts;
    private final Clock clock;

    /**
     * Runs a fail-open append in a transaction of its own.
     *
     * <p>A {@link TransactionTemplate} rather than a second {@code @Transactional}
     * method on this class, because a self-invocation does not pass through the
     * proxy: the annotation would be silently ignored and the append would run in
     * the caller's transaction, where its rollback would take the caller's write
     * with it — the exact opposite of what fail-open means.
     */
    private final TransactionTemplate isolatedAppend;

    public AuditTrailService(
            AuditEventRepository events,
            AuditRequestContext requests,
            OperationalAlerts alerts,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.events = events;
        this.requests = requests;
        this.alerts = alerts;
        this.clock = clock;
        this.isolatedAppend = new TransactionTemplate(transactionManager);
        this.isolatedAppend.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Records an accepted login. Fail-closed: a session this service cannot
     * account for is not issued.
     */
    @Transactional
    @Override
    public void recordLoginSuccess(UUID accountId) {
        append(event(
                AuditOperation.LOGIN_SUCCESS,
                AuditOutcome.SUCCESS,
                accountId,
                accountId,
                List.of(),
                AuditEvent.STATUS_OK,
                null));
    }

    /**
     * Records a refused login. Fail-open: the caller is already receiving a bare
     * {@code 401} and that answer does not change.
     *
     * @param subjectId stable id of the account the attempt named, or {@code null}
     *                  when the submitted username names no account — the one
     *                  thing that must not be recorded in its place
     */
    @Override
    public void recordLoginFailure(UUID subjectId, AuditRefusalReason reason) {
        appendRaisingAlertOnFailure(event(
                AuditOperation.LOGIN_FAILURE,
                AuditOutcome.FAILURE,
                null,
                subjectId,
                FAILURE_RUN_PATHS,
                AuditEvent.STATUS_CLIENT_ERROR,
                reason.name()));
    }

    /**
     * Records a session ended by its holder. Fail-closed, and recorded before the
     * session is invalidated, so a logout this service cannot account for does not
     * happen.
     */
    @Transactional
    @Override
    public void recordLogout(UUID accountId) {
        append(event(
                AuditOperation.LOGOUT,
                AuditOutcome.SUCCESS,
                accountId,
                accountId,
                List.of(),
                AuditEvent.STATUS_OK,
                null));
    }

    /**
     * Records a failure run reaching the limit. Fail-open for the same reason as
     * {@link #recordLoginFailure}: it happens on the rejected-login path, whose
     * answer is already a refusal.
     */
    @Override
    public void recordLockoutSet(UUID accountId) {
        appendRaisingAlertOnFailure(event(
                AuditOperation.LOCKOUT_SET,
                AuditOutcome.SUCCESS,
                null,
                accountId,
                LOCKOUT_PATHS,
                AuditEvent.STATUS_CLIENT_ERROR,
                null));
    }

    /**
     * Records a lockout that ran out, observed at the next login attempt against
     * the account. Fail-open: nobody performed this, so there is no request
     * outcome it would be honest to change.
     */
    @Override
    public void recordLockoutLiftedByExpiry(UUID accountId) {
        appendRaisingAlertOnFailure(event(
                AuditOperation.LOCKOUT_LIFT,
                AuditOutcome.SUCCESS,
                null,
                accountId,
                LOCKOUT_PATHS,
                AuditEvent.STATUS_OK,
                AuditLockoutLift.EXPIRY.name()));
    }

    /**
     * Records an administrator ending a lockout early. Fail-closed: the unlock is
     * a change an administrator asked for and it does not happen unrecorded.
     */
    @Transactional
    @Override
    public void recordLockoutLiftedByUnlock(UUID actorId, UUID subjectId) {
        append(event(
                AuditOperation.LOCKOUT_LIFT,
                AuditOutcome.SUCCESS,
                actorId,
                subjectId,
                LOCKOUT_PATHS,
                AuditEvent.STATUS_OK,
                AuditLockoutLift.UNLOCK.name()));
    }

    /** Records an account closed to logins. Fail-closed. */
    @Transactional
    @Override
    public void recordAccountDisabled(UUID actorId, UUID subjectId) {
        append(event(
                AuditOperation.ACCOUNT_DISABLE,
                AuditOutcome.SUCCESS,
                actorId,
                subjectId,
                ENABLED_PATHS,
                AuditEvent.STATUS_OK,
                null));
    }

    /** Records an account reopened to logins. Fail-closed. */
    @Transactional
    @Override
    public void recordAccountEnabled(UUID actorId, UUID subjectId) {
        append(event(
                AuditOperation.ACCOUNT_ENABLE,
                AuditOutcome.SUCCESS,
                actorId,
                subjectId,
                ENABLED_PATHS,
                AuditEvent.STATUS_OK,
                null));
    }

    private void append(AuditEvent event) {
        events.append(event);
    }

    /**
     * Appends in a transaction of its own and swallows a failure, having raised an
     * operational alert for it.
     *
     * <p>{@link RuntimeException} and not {@link Throwable}: a persistence failure
     * arrives as one, and an {@link Error} means the process is in a state where
     * continuing to serve the request is not the right call either.
     */
    private void appendRaisingAlertOnFailure(AuditEvent event) {
        try {
            isolatedAppend.executeWithoutResult(status -> events.append(event));
        } catch (RuntimeException appendFailed) {
            alerts.auditAppendFailed(event.operation(), appendFailed.getClass());
        }
    }

    private AuditEvent event(
            AuditOperation operation,
            AuditOutcome outcome,
            UUID actorId,
            UUID subjectId,
            List<String> changedPaths,
            String statusClass,
            String errorCode) {
        AuditRequest request = requests.current();
        return new AuditEvent(
                UUID.randomUUID(),
                clock.instant(),
                operation,
                outcome,
                actorId,
                subjectId,
                AuditEvent.ACCOUNT_RESOURCE_TYPE,
                subjectId,
                changedPaths,
                statusClass,
                errorCode,
                request.method(),
                request.pathTemplate(),
                request.requestId());
    }
}
