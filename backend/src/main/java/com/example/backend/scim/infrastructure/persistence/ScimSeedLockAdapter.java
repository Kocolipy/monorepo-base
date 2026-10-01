package com.example.backend.scim.infrastructure.persistence;

import com.example.backend.scim.domain.ScimSeedLock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The seeding lock as a Postgres transaction-scoped advisory lock.
 *
 * <p>Advisory rather than a row lock: there is no row to lock on a fresh database, which is exactly
 * when the lock matters, and an advisory lock needs no table, no migration and no privilege beyond
 * the connection the application already has.
 *
 * <p>{@code MANDATORY}: {@code pg_advisory_xact_lock} taken on an auto-commit connection is released
 * as soon as its own statement ends, so a call outside a transaction would succeed and protect
 * nothing. Refusing that call turns a silent loss of the guarantee into a startup failure.
 *
 * <p>JDBC rather than JPA, as in {@code ScheduledJobLockAdapter}: the statement is a lock, not a
 * read, and it joins the caller's transaction through the shared data source.
 */
@Repository
class ScimSeedLockAdapter implements ScimSeedLock {

    /**
     * Whole statement as a constant, never assembled; see the local Semgrep ruleset. The key is the
     * bytes of {@code "ScimSeed"} read as a big-endian {@code bigint} — arbitrary, but recognisable
     * in {@code pg_locks}; no other advisory lock is taken in this schema.
     */
    private static final String LOCK_SEEDING = "SELECT pg_advisory_xact_lock(6008762246113879396)";

    private final JdbcTemplate jdbc;

    ScimSeedLockAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void acquire() {
        jdbc.queryForList(LOCK_SEEDING);
    }
}
