-- One dormancy job and the cause of a lock (#118, ADR 0011).
--
-- A lock now has two causes: a failure run (ADR 0007) or dormancy, imposed by
-- the dormancy job at the lockout window. The cause sits beside `locked_at`,
-- set whenever it is set and cleared with it, so a helpdesk operator can tell a
-- forgotten password from an abandoned account before unlocking. The CHECK is
-- the last line: a write path that set one without the other cannot store it.
--
-- Every existing lock was imposed by a failure run, the only cause there was.
ALTER TABLE scim_users ADD COLUMN lock_cause VARCHAR(16);

UPDATE scim_users SET lock_cause = 'FAILURES' WHERE locked_at IS NOT NULL;

-- `NULL IN (...)` is NULL and a CHECK accepts NULL, so the locked branch spells
-- `lock_cause IS NOT NULL` explicitly: without it a lock with no cause would
-- evaluate to `FALSE OR NULL` and be stored.
ALTER TABLE scim_users
    ADD CONSTRAINT ck_scim_users_lock_cause
        CHECK ((locked_at IS NULL AND lock_cause IS NULL)
               OR (locked_at IS NOT NULL AND lock_cause IS NOT NULL
                   AND lock_cause IN ('FAILURES', 'DORMANCY')));

COMMENT ON COLUMN scim_users.locked_at IS
    'When the User was locked, by its failure run or by the dormancy job; NULL when it is not '
    'locked. A lock has no expiry — only an administrator''s Unlock clears this.';

COMMENT ON COLUMN scim_users.lock_cause IS
    'Why the User is locked: FAILURES or DORMANCY. NULL exactly when locked_at is.';

-- The two dormancy jobs are replaced by one, serialised on its own row (ADR
-- 0005). The runtime role cannot insert or delete these rows; the migration can.
DELETE FROM scheduled_job_locks
 WHERE job_name IN ('inactivity-deactivation', 'dormant-authority-revocation');

INSERT INTO scheduled_job_locks (job_name) VALUES ('dormancy');
