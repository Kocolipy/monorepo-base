# 5. Serialize scheduled jobs on per-job lock rows

Date: 2026-09-30

## Status

Accepted.

## Context

The specification requires the scheduled dormancy jobs — inactivity deactivation and
dormant-authority revocation — to be "serialized per job name so that no two runs of the
same job overlap across instances", and the two jobs must not block each other. Spring's
scheduler only knows about its own process, so any number of instances would each fire the
same cron. Nothing in the codebase serialized work across instances before this.

## Decision

Each job has a row in `scheduled_job_locks`, written by the migration that introduces the
job. A run opens its transaction, takes its own row with
`SELECT … FOR UPDATE SKIP LOCKED`, and holds it until the transaction ends. An empty result
means another run holds it, and the run skips rather than waits. A missing row fails the run
loudly instead of reading as "someone else is running it" forever. The port is
`ScheduledJobLock` (`auth.domain`), the adapter `ScheduledJobLockAdapter`.

Inside a run, each candidate User is re-read under its resource lock
(`findByIdForUpdate`) and decided again, so a User is never processed twice even across the
window between the candidate query and the write.

## Consequences

- The lock is released by the commit or rollback that ends the run. There is no lease to
  expire and nothing to release by hand, and a crashed instance's lock goes with its
  connection.
- A run is one transaction. A failure on one User rolls the whole run back, and the next run
  repeats it. That is the fail-closed half of ADR 0004.
- The runtime role holds `SELECT, UPDATE` on the table, because `FOR UPDATE` requires it, and
  no `INSERT` or `DELETE`, so it cannot remove the row a job serializes on.
- A new job that must not overlap itself needs a `ScheduledJob` member and a migration
  inserting its row.

## Alternatives considered

**`pg_try_advisory_xact_lock(hashtext(name))`.** Same transaction scoping with no table. Not
chosen: two job names can collide on a 32-bit hash and would then block each other, which is
the one thing the requirement rules out. The set of serializable jobs would also not be
visible in the schema.

**ShedLock or a similar library.** It adds a dependency and a lease-based model. A lease
expiring during a long run lets two runs overlap.
