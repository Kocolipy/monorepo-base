-- Serialize the audit retention job across instances (issue #97, ADR 0005).
--
-- Without a lock row every instance ran the retention delete on the same cron. The
-- delete is idempotent, so that cost duplicated work, row contention and two
-- `job-end` records claiming the same rows rather than data loss. The job now takes
-- this row with `FOR UPDATE SKIP LOCKED` before it assumes the retention role, and a
-- second run that finds it held skips. The runtime role's `SELECT, UPDATE` grant on
-- the table (V12) already covers the new row.
INSERT INTO scheduled_job_locks (job_name) VALUES
    ('audit-retention');
