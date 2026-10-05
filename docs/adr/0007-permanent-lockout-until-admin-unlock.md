# 7. Lockout is permanent until an Admin unlocks it

Date: 2026-10-01

## Status

Accepted. Records a departure from the original authentication slice: #7 shipped
a lockout that lifted on a timer. #30 replaced it with a permanent one, and #19,
#21 and #46 build on that change. ADR 0011 adds a second cause, dormancy, under the
same rule that only an administrator's Unlock lifts a lock. ADR 0010 replaced the
Admin authority with Permissions: Unlock now requires `user:write`.

## Context

The original authentication slice (#7) specified the standard values: a lock after 5 failed
attempts in a row, lifted automatically after 20 minutes. #7 shipped that, with
a configurable duration (`app.auth.lockout.duration`).

A lock that lifts on a timer allows a slow guessing rate: one burst of attempts
per window, indefinitely, and nobody is required to look. Once a lock instead
needs a human to lift it, two other things become necessary. A lock must also end
the sessions the User already holds, or an attacker holding one is unaffected.
And the recovery identity cannot be lockable, or an unauthenticated caller could
brick the deployment. The change reaches past #7: it shapes what Unlock means on the Accounts page (#21) and what
Unlock does to the credential (#19).

## Decision

- 3 consecutive failures lock the User (`app.auth.lockout.max-attempts`). No
  duration exists, no configuration key expresses one, and the passage of time
  never lifts the lock. `locked_at` records when the lock was imposed.
- An administrator's Unlock is the only exit, and it clears the failure run.
  Every `LOCKOUT_LIFT` audit event therefore names the administrator.
- Imposing the lockout revokes the User's live sessions after commit (ADR 0002).
- The Bootstrap Admin never locks. Its failures are counted and audited.
- Unlocking a User that has a password also sets the change-required flag (#19).
  The credential that hit the threshold may be the one being guessed.
- No administrator can unlock their own account. Recovering from a
  self-inflicted lock takes a second administrator or the Bootstrap Admin (#21).

## Consequences

- A locked User learns nothing by waiting, by design: the refusal is the same
  bare `401` as a wrong password, and the operational answer is an administrator.
- A guessing campaign spread across many accounts locks each of them, and
  unlocking them is administrator work. `LoginAuthenticationFailuresSustained` exists to
  catch that early, and edge throttling on Login caps its rate (`infra/README.md`).
- The Bootstrap Admin accepts unbounded online guessing. Argon2id cost, uniform
  refusal timing and audited failures mitigate it, not a lock.
- `LockoutHasNoDurationTests` proves that no deployed configuration file
  mentions a duration.

## Amendments

- 2026-10-02 (#98): the threshold is now 3 consecutive failures, down from the
  original 5. Only the default changed; it stays configurable through
  `APP_LOCKOUT_MAX_ATTEMPTS`, and everything else above holds as written.
