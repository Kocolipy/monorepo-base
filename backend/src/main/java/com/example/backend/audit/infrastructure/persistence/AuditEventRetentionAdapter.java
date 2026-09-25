package com.example.backend.audit.infrastructure.persistence;

import com.example.backend.audit.domain.AuditEventRetention;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Deletes aged-out audit events, and is the only place in this service that
 * assumes the retention database role.
 *
 * <p>JDBC rather than JPA: this is a set-based delete whose row count is the thing
 * the caller reports, and loading entities to remove them would read the whole
 * expired window into memory to learn a number the database already knows.
 *
 * <p>{@code SET LOCAL ROLE} and not {@code SET ROLE}: {@code LOCAL} reverts when
 * the surrounding transaction ends, so the pooled connection goes back to the role
 * the application assumed when it was opened. A plain {@code SET ROLE} would leave
 * every later request on that connection running as the retention role, which
 * would hand the whole application the {@code UPDATE} and {@code DELETE} privilege
 * this arrangement exists to withhold. There is deliberately no {@code RESET ROLE}
 * before it either, for the same reason: a reset is not transaction-scoped, so it
 * would outlive this transaction. None is needed — Postgres decides whether
 * {@code SET ROLE} is allowed from the session's login user, which is a member of
 * both roles, not from the role currently assumed.
 *
 * <p>Requires a transaction, which its caller
 * ({@code com.example.backend.audit.application.AuditRetentionService}) opens.
 * Without one {@code SET LOCAL} has nothing to be local to and Postgres discards
 * it with a warning, so the delete would run as the application role and be
 * refused — loudly, which is the right failure.
 */
@Repository
class AuditEventRetentionAdapter implements AuditEventRetention {

    /**
     * Whole statements as constants, never assembled: a role name spliced into SQL
     * is the shape of an injection even when today's value is a literal, and the
     * local Semgrep ruleset treats query concatenation as an error for that
     * reason.
     */
    private static final String ASSUME_RETENTION_ROLE = "SET LOCAL ROLE backend_audit_retention";

    private static final String DELETE_AGED_OUT =
            "DELETE FROM audit_events WHERE occurred_at < ?";

    private final JdbcTemplate jdbc;

    AuditEventRetentionAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long deleteOccurredBefore(Instant cutoff) {
        jdbc.execute(ASSUME_RETENTION_ROLE);
        return jdbc.update(DELETE_AGED_OUT, Timestamp.from(cutoff));
    }
}
