# 4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal

Date: 2026-09-25

## Status

Accepted.

## Context

The audit trail records what the application already does: accepted and refused
logins, logouts, lockouts imposed and lifted, accounts disabled and enabled. Two
requirements about what happens when an append itself fails were stated together,
and taken literally they contradict each other on one path:

1. a write whose audit insert cannot commit rolls the triggering mutation back
   (**fail-closed**);
2. a failure-event append that itself fails raises an operational alert **without
   altering the original request's outcome** (**fail-open**).

A rejected login is both. It is a refusal, so (2) applies. It also performs a write
— the failure run lengthens, and reaching the limit imposes a lockout — so (1)
looks like it applies as well. Applying (1) there would mean a rejected login whose
audit insert failed returns `500`, because the exception would leave
`LoginAttemptService` as something other than an `AuthenticationException`.

## Decision

Which semantics an append gets is decided by **what the triggering request was
going to return**, not by whether a row was written:

- **Fail-closed** where the request would otherwise **succeed**: an
  administrator's disable, enable or unlock, an accepted login, a logout. The
  append joins the caller's transaction and flushes, so an event that cannot be
  written takes the mutation with it. A change this service cannot account for does
  not happen, and a session it cannot account for is not issued.
- **Fail-open with an operational alert** where the request is **already being
  refused**: the `LOGIN_FAILURE` event and the `LOCKOUT_SET` it may be accompanied
  by. These appends run in a transaction of their own
  (`PROPAGATION_REQUIRES_NEW`), so a rollback there cannot take the caller's
  failure-run write with it, and a failure raises
  `OperationalAlerts.auditAppendFailed` instead of propagating.

  There is no fail-open lift. A lockout has no duration, so `LOCKOUT_LIFT` is only
  ever an administrator's unlock, which is a request that would otherwise succeed
  and is therefore fail-closed with everything else in that group.

The alert is a port with a logging adapter rather than a log call in the use case,
so "an alert was raised" is a claim a test can check without reading log bytes, and
so where an alert goes stays a deployment decision.

## Consequences

A rejected login always answers with the same bare `401`, whatever the state of the
audit trail. That is the point: turning it into a `500` would tell whoever submitted
the credentials something about the service's internal state, and would change the
answer to a question that had already been answered correctly.

The cost is a real one and worth stating plainly: while the trail is unavailable,
failed logins and the lockouts they cause continue to happen and are **not
recorded**. The only evidence is the `ERROR` alert per lost event, naming the
operation and the failure's type. An operator who ignores those alerts has a trail
with a hole in it and nothing in the trail itself will say so — which is why the
alert is `ERROR` rather than `WARN`, and why it names the missing operation.

The asymmetry is also a thing a reader has to know before changing either path. A
new audited event on the rejected-login path must be fail-open or a refused login
starts returning `500`; a new audited administrative write must be fail-closed or
an unrecorded change becomes possible. `AuditTrailService` is where both are
expressed, and `AuditTrailServiceTests` asserts the two behaviours against the same
forced failure so the difference cannot become an accident of which exception was
thrown.

## Alternatives considered

**Fail-closed everywhere.** Rejected: a rejected login would return `500` when the
trail is unavailable, which both leaks internal state and replaces a correct answer
with a wrong one.

**Fail-open everywhere.** Rejected: an administrator could then disable an account
with no record of who did it, which is the single event the trail exists for.

**Append after commit, everywhere.** Rejected: it makes every append fail-open by
construction, and the transactional guarantee in (1) becomes unexpressible.
