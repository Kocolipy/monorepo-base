package com.example.backend.scim.domain;

/**
 * What a connector's token authorises across the whole directory.
 *
 * <p>Two members and not three: write implies read, so there is no scope that can
 * mutate a resource it cannot retrieve. That is expressed as
 * {@link #permitsWrite()} being the only question a caller ever asks — a read is
 * permitted by every scope, so a {@code permitsRead()} that always returned
 * {@code true} would only invite a call site to branch on it.
 *
 * <p>Scope is directory-wide. It is a property of the credential rather than of a
 * resource or a role, which is why the SCIM chain enforces it in a filter and no
 * handler under {@code /scim/v2} carries a scope check of its own.
 */
public enum ConnectorTokenScope {

    /** May discover, retrieve and search Users and Groups. May not mutate them. */
    READ_ONLY,

    /** Everything {@link #READ_ONLY} may do, plus create, replace, patch and delete. */
    READ_WRITE;

    /** Whether a mutating SCIM request made with this scope may proceed. */
    public boolean permitsWrite() {
        return this == READ_WRITE;
    }
}
