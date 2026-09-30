-- Inactivity governance: when a User last authenticated, and the lock rows that keep
-- the two scheduled dormancy jobs from overlapping themselves.

-- 1. When the User last logged in successfully.
--
-- Application-owned authentication state beside the failure run and the lock instant,
-- and like them NOT a SCIM attribute: writing it does not advance the resource's
-- version or `meta.lastModified`, it is absent from `/Schemas`, and it is not
-- filterable. Nullable, because a User that has never authenticated has no such
-- instant — the dormancy jobs then measure from `scim_resources.created_at`, so a
-- credentialless User is not treated as dormant the moment it is provisioned.
--
-- An explicit inactive-to-active transition also writes it (the reactivation time),
-- which is what resets the dormancy window for a reactivated User.
ALTER TABLE scim_users
    ADD COLUMN last_authenticated_at TIMESTAMPTZ;

COMMENT ON COLUMN scim_users.last_authenticated_at IS
    'When the User last logged in successfully, or was last explicitly reactivated; '
    'NULL when neither has happened. The dormancy basis, with created_at as the fallback.';

-- 2. One lock row per scheduled job.
--
-- A run takes its own row with `SELECT ... FOR UPDATE SKIP LOCKED` inside the run's
-- transaction and holds it until that transaction ends. A second run of the SAME job
-- — on this instance or another — finds the row locked and skips; a run of the OTHER
-- job locks a different row and is never blocked by it. That is "serialized per job
-- name" as a property of the database rather than of a scheduler that only knows
-- about its own process.
--
-- A row per job rather than an advisory lock keyed by a hash of the name: two names
-- can never collide on a primary key, and the set of jobs that can be serialized is
-- visible in the schema.
CREATE TABLE scheduled_job_locks (
    job_name VARCHAR(64) NOT NULL,
    CONSTRAINT pk_scheduled_job_locks PRIMARY KEY (job_name)
);

INSERT INTO scheduled_job_locks (job_name) VALUES
    ('inactivity-deactivation'),
    ('dormant-authority-revocation');

-- `FOR UPDATE` needs UPDATE privilege as well as SELECT. No INSERT or DELETE: the job
-- set is fixed by migrations, so the runtime role cannot remove the row a job
-- serializes on.
GRANT SELECT, UPDATE ON scheduled_job_locks TO backend_app;
