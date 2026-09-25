package com.example.backend.audit.domain;

/**
 * Persistence port for the audit trail. Append only — there is no update and no
 * delete here, because the application holds no privilege to perform either and
 * the database refuses the statement regardless of what code asks for it.
 *
 * <p>{@link #append} joins whatever transaction its caller is in, which is what
 * makes a recorded mutation and its record one atomic thing: an append that
 * cannot commit takes the mutation it was recording down with it.
 */
public interface AuditEventRepository {

    /**
     * Writes the event, failing the surrounding transaction if it cannot be
     * written.
     *
     * <p>Flushes rather than deferring to the commit, so a constraint violation
     * or a refused privilege surfaces at this call rather than at a commit the
     * caller can no longer react to.
     */
    void append(AuditEvent event);
}
