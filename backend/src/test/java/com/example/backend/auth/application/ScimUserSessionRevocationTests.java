package com.example.backend.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.backend.audit.RecordingAuditTrail;
import com.example.backend.audit.domain.AuditOperation;
import com.example.backend.auth.InMemoryAccountSessions;
import com.example.backend.auth.PendingCommit;
import com.example.backend.auth.domain.AccountSessions;
import com.example.backend.scim.domain.ScimUserSessions.Cause;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * The ordering ADR 0002 settled, for a SCIM write: sessions end after the commit, never on a
 * rollback, and the outcome is recorded once it is known.
 */
class ScimUserSessionRevocationTests {

    private static final UUID CONNECTOR = UUID.randomUUID();

    private final UUID user = UUID.randomUUID();

    private final InMemoryAccountSessions sessions = new InMemoryAccountSessions();

    private final PendingCommit commit = new PendingCommit();

    private final RecordingAuditTrail audit = new RecordingAuditTrail();

    private final ScimUserSessionRevocation revocation =
            new ScimUserSessionRevocation(sessions, commit, audit);

    @Test
    void nothing_is_revoked_or_recorded_until_the_transaction_commits() {
        sessions.open(user, "s-1");

        revocation.revokeAfterCommit(CONNECTOR, user, Set.of(Cause.PASSWORD_CHANGED));

        assertThat(sessions.sessionsOf(user)).containsExactly("s-1");
        assertThat(audit.recorded()).isEmpty();

        commit.commit();

        assertThat(sessions.sessionsOf(user)).isEmpty();
        assertThat(sessions.revocations()).containsExactly(user);
    }

    @Test
    void a_rolled_back_write_revokes_nothing_and_records_nothing() {
        sessions.open(user, "s-1");

        revocation.revokeAfterCommit(CONNECTOR, user, Set.of(Cause.DEACTIVATED));
        commit.rollback();

        assertThat(sessions.sessionsOf(user)).containsExactly("s-1");
        assertThat(sessions.revocations()).isEmpty();
        assertThat(audit.recorded()).isEmpty();
    }

    /** Each cause is recorded as the attribute whose change it is. */
    @Test
    void a_successful_revocation_is_recorded_with_the_connector_the_user_and_every_cause() {
        revocation.revokeAfterCommit(CONNECTOR, user, EnumSet.allOf(Cause.class));
        commit.commit();

        assertThat(audit.of(AuditOperation.USER_SESSIONS_REVOKE))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.actorId()).isEqualTo(CONNECTOR);
                    assertThat(event.subjectId()).isEqualTo(user);
                    assertThat(event.detail()).isEqualTo("SUCCESS:ACTIVE,GROUPS,PASSWORD,USER_NAME");
                });
    }

    /**
     * The write is already durable, so the failure cannot be undone — it is recorded as a failure
     * and then surfaces to the caller, so a connector is not told a change fully succeeded while
     * the sessions it should have ended survive.
     */
    @Test
    void a_failed_revocation_is_recorded_as_a_failure_and_propagates() {
        IllegalStateException storeDown = new IllegalStateException("session store unavailable");
        AccountSessions broken = accountId -> {
            throw storeDown;
        };
        ScimUserSessionRevocation failing = new ScimUserSessionRevocation(broken, commit, audit);

        failing.revokeAfterCommit(CONNECTOR, user, Set.of(Cause.USER_NAME_CHANGED));

        assertThatThrownBy(commit::commit).isSameAs(storeDown);
        assertThat(audit.of(AuditOperation.USER_SESSIONS_REVOKE))
                .singleElement()
                .extracting(RecordingAuditTrail.Recorded::detail)
                .isEqualTo("FAILURE:USER_NAME");
    }

    /**
     * A deletion changed no attribute — the whole User went — so its revocation names no path,
     * yet it still ends every session and is still recorded.
     */
    @Test
    void a_deletion_ends_the_sessions_and_is_recorded_with_no_changed_path() {
        sessions.open(user, "s-1");

        revocation.revokeAfterCommit(CONNECTOR, user, Set.of(Cause.DELETED));
        commit.commit();

        assertThat(sessions.sessionsOf(user)).isEmpty();
        assertThat(audit.of(AuditOperation.USER_SESSIONS_REVOKE))
                .singleElement()
                .extracting(RecordingAuditTrail.Recorded::detail)
                .isEqualTo("SUCCESS:");
    }

    @Test
    void a_revocation_with_no_cause_is_a_programming_error_and_schedules_nothing() {
        assertThatThrownBy(() -> revocation.revokeAfterCommit(CONNECTOR, user, Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(commit.pending()).isZero();
    }
}
