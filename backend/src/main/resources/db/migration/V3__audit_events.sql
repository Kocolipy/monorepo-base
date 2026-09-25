-- The append-only audit event table, plus the two database roles that make
-- "append-only" a property of the database rather than of the application code
-- that writes to it.
--
-- Three separate mechanisms, because each covers a hole the others leave:
--
--   1. GRANTs. `backend_app` — the role the running application assumes on every
--      connection — holds INSERT and SELECT on this table and nothing else, so a
--      stray UPDATE or DELETE from application code is refused by the server with
--      SQLSTATE 42501 before any row is read.
--   2. A BEFORE UPDATE OR DELETE trigger. Grants are attached to a role, so they
--      say nothing about a connection that arrives as the table's owner — a
--      migration, a console session, a deployment that forgot to set the runtime
--      role. The trigger refuses the statement whatever role issues it, unless
--      that role is the retention role, so the append-only property survives a
--      misconfiguration instead of depending on one being absent.
--   3. `backend_audit_retention`, which holds the UPDATE/DELETE grant and is the
--      one role the trigger admits. The retention job assumes it for the length
--      of its own transaction and nothing else in the service names it.
--
-- Both roles are NOLOGIN group roles: they carry privileges, not credentials, so
-- nothing here creates a password and no secret enters version control. The
-- login role that runs the migration is granted membership in both, which is what
-- lets the application `SET ROLE backend_app` and the retention job
-- `SET LOCAL ROLE backend_audit_retention`.
--
-- A later migration that adds a table the application writes must grant
-- `backend_app` on it; without the grant the application cannot read it. That is
-- the cost of a least-privilege runtime role and is deliberate.

CREATE TABLE audit_events (
    -- Assigned by the application before the INSERT, like accounts.id, so every
    -- layer above the database already agrees on the event's identity.
    id            UUID         NOT NULL,

    occurred_at   TIMESTAMPTZ  NOT NULL,

    -- What happened, as this service's own vocabulary: LOGIN_SUCCESS,
    -- LOCKOUT_SET, ACCOUNT_DISABLE and so on. A closed set, never free text.
    operation     VARCHAR(64)  NOT NULL,

    -- SUCCESS or FAILURE. The outcome of the operation, not of recording it.
    outcome       VARCHAR(16)  NOT NULL,

    -- Who acted and who was acted on, both as the account's stable id. Null
    -- actor: nobody was authenticated (a rejected login). Null subject: the
    -- operation named no existing resource (a login against a username that
    -- names no account) — recording anything else there would be recording the
    -- submitted username, which is half a credential.
    actor_id      UUID,
    subject_id    UUID,

    resource_type VARCHAR(64)  NOT NULL,
    resource_id   UUID,

    -- The attribute paths the operation changed, comma-separated, drawn from a
    -- fixed vocabulary in the application. A list rather than a related table
    -- because the values are short constants and nothing queries them
    -- individually; if a query ever needs one path at a time, that is the
    -- migration that normalises it.
    changed_paths TEXT,

    -- How the triggering request ended, classified: ok, client_error,
    -- server_error. Paired with error_code, which is a type or reason name from
    -- the service's own code — never a message, which is written for a human and
    -- may grow a submitted value in it later.
    status_class  VARCHAR(32)  NOT NULL,
    error_code    VARCHAR(128),

    -- The triggering request, identified the way it can be without naming a
    -- subject: the method, the matched route TEMPLATE (`/api/admin/accounts/
    -- {username}/disable`, never the resolved path, which carries the username in
    -- it), and the correlation id minted for that request.
    http_method   VARCHAR(16),
    http_path     VARCHAR(512),
    request_id    VARCHAR(128),

    CONSTRAINT pk_audit_events PRIMARY KEY (id)
);

-- Retention deletes by age, and the history of one account is read by age too.
CREATE INDEX ix_audit_events_occurred_at ON audit_events (occurred_at);
CREATE INDEX ix_audit_events_subject_id ON audit_events (subject_id);

-- Roles are cluster-wide, so a second database in the same cluster reaches this
-- migration with them already created. Creating them conditionally makes the
-- migration idempotent against the cluster rather than only against this
-- database.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'backend_app') THEN
        CREATE ROLE backend_app NOLOGIN;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'backend_audit_retention') THEN
        CREATE ROLE backend_audit_retention NOLOGIN;
    END IF;
END
$$;

GRANT USAGE ON SCHEMA public TO backend_app, backend_audit_retention;

-- The application's own privileges: full DML on the tables it owns the lifecycle
-- of, and INSERT/SELECT only on the audit table.
GRANT SELECT, INSERT, UPDATE, DELETE ON accounts      TO backend_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON user_counters TO backend_app;
GRANT SELECT, INSERT                 ON audit_events  TO backend_app;
GRANT SELECT ON flyway_schema_history TO backend_app;

-- The retention role's privileges: the audit table alone, and the two verbs the
-- application is refused. SELECT because a deletion by age has to find the rows
-- first.
GRANT SELECT, UPDATE, DELETE ON audit_events TO backend_audit_retention;

GRANT backend_app, backend_audit_retention TO CURRENT_USER;

-- The append-only guard. `SET search_path` is pinned so the function cannot be
-- redirected at a shadowing object by a caller's own search_path.
CREATE FUNCTION audit_events_refuse_mutation() RETURNS trigger
    LANGUAGE plpgsql
    SET search_path = pg_catalog, public
AS $$
BEGIN
    IF current_user <> 'backend_audit_retention' THEN
        RAISE EXCEPTION
            'audit_events is append-only: % may not % a recorded event',
            current_user, TG_OP
            USING ERRCODE = 'insufficient_privilege';
    END IF;
    -- Returning OLD from a BEFORE DELETE lets the delete proceed; returning NEW
    -- from a BEFORE UPDATE leaves the submitted row untouched. Returning NULL
    -- would silently CANCEL the statement, which is the one outcome this guard
    -- must never produce: the retention job would report rows deleted that were
    -- still there.
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END
$$;

CREATE TRIGGER audit_events_append_only
    BEFORE UPDATE OR DELETE ON audit_events
    FOR EACH ROW EXECUTE FUNCTION audit_events_refuse_mutation();
