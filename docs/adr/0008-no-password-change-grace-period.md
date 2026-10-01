# 8. No password-change grace period; confined logins keep the dormancy clock

Date: 2026-10-01

## Status

Accepted. Records a departure from the SCIM plan that spans #19 (password-change
lifecycle, which shipped the grace period), #18 (inactivity governance) and the
confinement work in #46 and #47. Implemented in #48, for which no issue was filed.

## Context

#19 shipped a grace period. A User whose change-required flag stayed set past a
configured window was deactivated by a scheduled job,
`PASSWORD_CHANGE_GRACE_DEACTIVATION`. Separately, every accepted Login set
`lastAuthenticatedAt`, the dormancy basis that #18's inactivity job measures from.

The two rules together had a gap and an overlap. The gap: a flagged User who
kept logging in without changing the password never went dormant, because those
logins moved the clock, even though a confined session can do nothing except
change the password or log out. The overlap: the grace job deactivated Users the
inactivity job would also reach. A review against IM8 found no control that asks
for a grace deadline. ac-6 and as-15 ask for the flag on every path that imposes
a credential, a session confined to change-password and logout, and clearing
only on a successful change. #46 and #47 had already delivered all of that.

## Decision

- No grace period exists. The grace job, its schedule, its lock row, its
  configuration key and its audit operation are removed. The `V14` migration
  deletes the lock row.
- A Login made while the flag is set does not move `lastAuthenticatedAt`. Its
  failure run still clears, and `LOGIN_SUCCESS` is still audited.
- A successful self-service change records the authentication, since it is the
  account's first real use.
- `password_change_required_since` stays. Its presence is the flag, and its value
  says when the change was last required.

## Consequences

- An imposed credential that is never replaced is bounded by inactivity
  deactivation, 90 days by default, measured from the last real use, instead of
  by a separate deadline. One job owns "this User is not using the account".
- The Bootstrap Admin is seeded flagged and is exempt from inactivity
  deactivation, so an unchanged recovery credential never removes the recovery
  path. It stays confined until it is changed.
- Session revocation has one fewer trigger. The scheduler pool shrinks from 4 to 3.
