package com.example.backend.audit;

import com.example.backend.audit.domain.AuditLockoutLift;
import com.example.backend.audit.domain.AuditOperation;
import com.example.backend.audit.domain.AuditRefusalReason;
import com.example.backend.audit.domain.AuditTrail;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * An {@link AuditTrail} that keeps what it was told, so a unit test of a use case
 * can assert which events that use case records without a database.
 *
 * <p>Records the arguments rather than counting calls: "exactly one event, naming
 * this account by its stable id, for this reason" is the claim worth checking, and
 * a counter cannot express it.
 */
public final class RecordingAuditTrail implements AuditTrail {

    private final List<Recorded> recorded = new ArrayList<>();

    /** One recorded call: what it said happened, and the ids and reason it named. */
    public record Recorded(
            AuditOperation operation, UUID actorId, UUID subjectId, String detail) {
    }

    public List<Recorded> recorded() {
        return List.copyOf(recorded);
    }

    /** Every recorded call of one operation, for a per-operation count. */
    public List<Recorded> of(AuditOperation operation) {
        return recorded.stream().filter(event -> event.operation() == operation).toList();
    }

    public void reset() {
        recorded.clear();
    }

    @Override
    public void recordLoginSuccess(UUID accountId) {
        recorded.add(new Recorded(AuditOperation.LOGIN_SUCCESS, accountId, accountId, null));
    }

    @Override
    public void recordLoginFailure(UUID subjectId, AuditRefusalReason reason) {
        recorded.add(new Recorded(
                AuditOperation.LOGIN_FAILURE, null, subjectId, reason.name()));
    }

    @Override
    public void recordLogout(UUID accountId) {
        recorded.add(new Recorded(AuditOperation.LOGOUT, accountId, accountId, null));
    }

    @Override
    public void recordLockoutSet(UUID accountId) {
        recorded.add(new Recorded(AuditOperation.LOCKOUT_SET, null, accountId, null));
    }

    @Override
    public void recordLockoutLiftedByExpiry(UUID accountId) {
        recorded.add(new Recorded(
                AuditOperation.LOCKOUT_LIFT, null, accountId, AuditLockoutLift.EXPIRY.name()));
    }

    @Override
    public void recordLockoutLiftedByUnlock(UUID actorId, UUID subjectId) {
        recorded.add(new Recorded(
                AuditOperation.LOCKOUT_LIFT,
                actorId,
                subjectId,
                AuditLockoutLift.UNLOCK.name()));
    }

    @Override
    public void recordAccountDisabled(UUID actorId, UUID subjectId) {
        recorded.add(new Recorded(AuditOperation.ACCOUNT_DISABLE, actorId, subjectId, null));
    }

    @Override
    public void recordAccountEnabled(UUID actorId, UUID subjectId) {
        recorded.add(new Recorded(AuditOperation.ACCOUNT_ENABLE, actorId, subjectId, null));
    }
}
