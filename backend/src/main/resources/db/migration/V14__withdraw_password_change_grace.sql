-- The password-change grace period is withdrawn: no IM8 control requires deactivating a User
-- that leaves a required change unmade, and the confinement of its sessions to the change flow
-- (ac-6, as-15) is what the flag exists for. An imposed credential nobody replaces is instead
-- bounded by the inactivity window, because a confined login no longer moves the dormancy basis.

-- 1. The grace-period job's lock row, which V13 wrote and no job now takes.
DELETE FROM scheduled_job_locks WHERE job_name = 'password-change-grace-deactivation';

-- 2. The flag's column stays — its presence IS the flag — but it is no longer a grace basis.
COMMENT ON COLUMN scim_users.password_change_required_since IS
    'When a password change was last required of the User; NULL when none is. '
    'Cleared only by a successful self-service change.';
