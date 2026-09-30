-- The change-required flag, and the lock row that serializes the grace-period job.

-- 1. When the User's current credential was imposed on it by somebody else.
--
-- The column's presence IS the flag, as `locked_at`'s presence is the lockout: a
-- User with a value here must replace its password before it may do anything but
-- submit that change or log out, and the value is the instant the grace period is
-- measured from. NULL means no change is required.
--
-- Application-owned authentication state beside the failure run and the lock
-- instant, and like them NOT a SCIM attribute: it is absent from `/Schemas`, is not
-- filterable, and writing it alone does not advance the resource's version. It is
-- set by every connector password write, by an Admin's forced change and by an
-- Unlock, and cleared by nothing but a successful self-service change.
ALTER TABLE scim_users
    ADD COLUMN password_change_required_since TIMESTAMPTZ;

COMMENT ON COLUMN scim_users.password_change_required_since IS
    'When a password change was last required of the User; NULL when none is. '
    'Cleared only by a successful self-service change; the grace-period basis.';

-- 2. The grace-period job's own lock row, for the reason V12 gave the dormancy jobs
-- theirs: a second run of this job skips, and neither dormancy job waits on it.
INSERT INTO scheduled_job_locks (job_name) VALUES
    ('password-change-grace-deactivation');
