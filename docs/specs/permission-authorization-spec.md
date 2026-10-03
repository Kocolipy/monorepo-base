# Permission-based authorization

Status: draft, 2026-10-03. Supersedes the plan's §"Authorization matrix"
(`docs/specs/scim-v2-account-management-plan.md`) once accepted. No ADR has been
written yet; the ADRs this spec will need are listed under Further Notes.

## Problem Statement

Authority in this application is all-or-nothing. A User is either an Admin, because
it is a direct member of the Admin group and so holds `ROLE_ADMIN`, or it holds only
baseline access. A deployment that wants a helpdesk operator to unlock accounts has
to give that operator every administrative power: reading the audit stream, deleting
connectors, and minting connector tokens that can rewrite the whole directory. The
Prometheus scraper's account has to be a full Admin just to read `/actuator`.

Connector tokens have a separate, coarser vocabulary (`READ_ONLY` / `READ_WRITE`,
surfaced as `scim.read` / `scim.write`) that cannot say "this connector manages
Groups but not Users", and nothing stops an Admin minting a token more powerful than
anything the Admin is meant to hold.

Authorization is decided only by URL rules in the filter chain. There is no method
security, so a route added outside the matched prefixes is unprotected unless someone
remembers to add a rule.

The dormancy controls have the same coarseness. The inactivity job writes `active`, an
attribute the directory is meant to own, and the dormant-authority job removes Admin
group membership, which then drifts from what the identity provider believes.

## Solution

Every protected action requires a named, fine-grained Permission
(`user:read`, `connector:token`, …). A Role is a named set of Permissions. A User gets
Roles through its Group memberships: a read-only deployment configuration maps a Group's
stable id to a Role. The identity provider still decides who is in which Group, so it
remains the one place that decides who has access.

Connector tokens carry a list of Permissions from the same vocabulary. The names are
shared, but each request path accepts only its own kind of caller: sessions on the
application chain, tokens on the SCIM chain. A token can only carry Permissions its
creator holds.

One Superuser Group, which cannot be renamed or deleted, maps to a Role holding every
Permission. The Bootstrap Admin's membership in it is frozen, so the deployment can
always be recovered.

Dormancy becomes two steps for every User except the Bootstrap Admin. At 90 days
without authenticating the User is locked out, with the cause recorded, and only an
administrator's Unlock lifts it. At 180 days the User's Roles are revoked: its
memberships of every mapped Group are removed. The application stops writing `active`.

The SPA learns the caller's Permissions from `/api/auth/me` and shows each
administrative page only to Users holding the Permission it needs. The server remains
the only enforcement.

## User Stories

Roles and mapping

1. As a deployment operator, I want to define Roles as named sets of Permissions in configuration, so that I can grant exactly the powers a job needs.
2. As a deployment operator, I want to map a Group's stable id to a Role, so that the identity provider's Group assignments decide who holds which Role.
3. As a deployment operator, I want startup to fail on an unknown Permission name, so that a typo cannot silently grant nothing or the wrong thing.
4. As a deployment operator, I want startup to fail when two mapping entries name the same Group, so that a User's Roles are never ambiguous.
5. As a deployment operator, I want startup to fail when the mapping references a Group that does not exist, so that a stale mapping is caught at deploy time rather than discovered as a missing power.
6. As a deployment operator, I want startup to fail unless exactly one Superuser Group is designated and its Role holds every Permission, so that the deployment can always be administered.
7. As a security reviewer, I want the mapping to be unchangeable while the application runs, so that no endpoint can grant Permissions and no Permission holder can raise their own.
8. As a User in several mapped Groups, I want my Permissions to be the union of their Roles, so that holding an extra Role never takes a power away.
9. As a User in no mapped Group, I want to keep baseline access, so that ordinary self-service still works.

Permission vocabulary and enforcement

10. As a helpdesk operator holding `user:read`, I want to list accounts, so that I can find the person who called me.
11. As an account administrator holding `user:write`, I want to unlock an account, so that a locked User can sign in again.
12. As an account administrator holding `user:write`, I want to force a password change, so that I can respond to a suspected compromise by revoking the User's sessions and confining it to a password change.
12a. As a security reviewer, I want unlock and force password change to be refused when they target the caller's own account, so that no administrator can act on their own account through the admin flow.
13. As an operator holding `group:read`, I want to list Groups, so that I can see who holds which Role.
13a. As an operator holding `group:read`, I want to read the Role definitions and the role mapping, so that I can see what each Role grants and which Group confers it without reading deployment configuration.
14. As an auditor holding `audit:read`, I want to read the audit stream without holding any other administrative power, so that review is separated from administration.
15. As a connector administrator holding `connector:read`, I want to list connectors, so that I can see which directories provision this application.
16. As a connector administrator holding `connector:write`, I want to create and delete connectors, so that I can onboard and retire a directory.
17. As a credential administrator holding `connector:token`, I want to create, rotate and revoke connector tokens, so that I can manage directory credentials separately from connector records.
18. As a monitoring operator, I want a Role holding only `ops:read`, so that the Prometheus scraper's account cannot do anything else.
19. As a User holding `counter:read` / `counter:write`, I want to read and change the demo counter, so that the scheme has an ordinary, non-administrative example.
20. As any active User without a required password change, I want `/api/auth/me`, change-password, logout, `/api/self` and `/api/session` to need no Permission, so that a misconfigured Role can never stop me changing my own password or signing out.
21. As a User with a required password change, I want to reach only the endpoints I can reach today, so that the confinement still holds.
22. As a caller lacking the required Permission, I want a `403`, so that I know the request was understood and refused.
23. As a developer, I want every protected method to declare its Permission where it is defined, so that the requirement sits next to the code it protects.
24. As a security reviewer, I want any `/api/admin/**` route without a declared Permission to be refused, so that a forgotten declaration fails closed.

Connector tokens

25. As a credential administrator, I want to choose a token's Permissions from `user:read`, `user:write`, `group:read` and `group:write`, so that a connector that manages only Groups cannot write Users.
26. As a security reviewer, I want token creation and rotation to refuse any Permission the creator does not hold, so that `connector:token` is not a back door to every Permission.
27. As a connector, I want `GET /scim/v2/Users…` to need `user:read` and Users writes to need `user:write`, so that the token's list means what it says.
28. As a connector, I want `GET /scim/v2/Groups…` to need `group:read` and Groups writes to need `group:write`, so that Group management can be granted on its own.
29. As a connector, I want a root `/.search` to return only the resource types I may read, rather than failing, so that a partially permitted token still gets a conformant answer.
30. As a connector, I want `ServiceProviderConfig`, `Schemas` and `ResourceTypes` to be readable with any valid token, so that discovery works before I know my Permissions.
31. As a security reviewer, I want the SCIM chain to accept bearer tokens only and the application chain never to route `/scim/**`, so that a User's session can never call SCIM even when the User holds `user:write`.
32. As a documentation reader, I want it stated that `group:write` on a mapped Group is Role assignment, so that nobody mistakes it for a harmless Permission.

Superuser Group

33. As a deployment operator, I want the Superuser Group to be impossible to rename or delete, so that the deployment's recovery path cannot be removed.
34. As a deployment operator, I want the Bootstrap Admin's Superuser Group membership to stay frozen, so that there is always one account holding every Permission.
35. As an identity provider administrator, I want every other mapped Group to stay fully writable and deletable, so that I can retire a helpdesk Group without touching this application's configuration.

Sessions and propagation

36. As a security reviewer, I want losing a mapped Group to revoke the User's live sessions after commit, so that a removed power stops immediately.
37. As a User newly added to a mapped Group, I want the new Permissions at my next sign-in, so that the rule is simple and predictable.
38. As a deployment operator, I want sessions created under a different mapping to be revoked at startup, so that a changed mapping takes effect whether or not the session store survived the redeploy.

SPA

39. As a User of the SPA, I want `/api/auth/me` to report my Permissions, so that the SPA can show me only what I can use.
40. As a User of the SPA, I want each administrative page shown only when I hold the Permission it needs, so that a helpdesk operator sees accounts but not connectors.
41. As a User without any administrative Permission, I want no administrative navigation at all, so that the SPA looks the way it does for baseline Users today.
42. As a User whose deep link to an administrative page needs a Permission I lack, I want to be routed away the same way the SPA routes non-Admins today, so that deep links cannot expose an unusable page.

Audit

43. As an auditor, I want every authorization refusal to record who was refused and on which operation, with a generic reason, so that I can tell misconfiguration from probing without the audit stream disclosing permission logic.
44. As an auditor, I want a membership change on a mapped Group to record the Role gained or lost, so that the audit stream shows changes of power, not only changes of membership.
45. As an auditor, I want token creation and rotation to record the Permissions the token carries, and a refused escalation to record the Permissions refused, so that credential issuance is traceable.

Dormancy

46. As a security reviewer, I want every User dormant for 90 days to be locked out, so that unused accounts stop being usable without anyone noticing.
47. As a helpdesk operator, I want to see whether a lockout came from failed logins or from dormancy, so that I know whether to verify the person before unlocking.
48. As a User locked for dormancy, I want the same bare `401` at login as for any other refusal, so that the response reveals nothing about the account.
49. As an account administrator holding `user:write`, I want Unlock to lift a dormancy lockout and restart the dormancy clock, so that the next job run does not lock the User again before it can sign in.
50. As a security reviewer, I want a User dormant for 180 days to lose every Role, so that a long-abandoned account holds no authority even if it is later unlocked.
51. As an identity provider administrator, I want a dormancy role revocation to be recorded in the audit stream, so that I can reconcile the directory's Group memberships.
52. As a deployment operator, I want the Bootstrap Admin exempt from both dormancy steps, so that recovery is never locked out or stripped of its Roles.
53. As a User already locked for failed logins, I want the dormancy job to keep that cause, so that the record says why I was first locked.
54. As an identity provider administrator, I want the application to stop writing `active`, so that the directory is the only owner of that attribute.
55. As a deployment operator, I want to configure the lockout and role-revocation windows, with startup failing unless the role-revocation window is longer, so that the two steps always happen in order.

## Implementation Decisions

Vocabulary

- Permissions are a closed set: `user:read`, `user:write`, `group:read`, `group:write`, `audit:read`, `connector:read`, `connector:write`, `connector:token`, `ops:read`, `counter:read`, `counter:write`. A new Permission is a code change, not configuration.
- Permission to endpoint, application chain:

  | Permission | Operations |
  |---|---|
  | `user:read` | list accounts |
  | `user:write` | unlock, force password change |
  | `group:read` | list Groups; read Role definitions and the role mapping |
  | `audit:read` | list audit events |
  | `connector:read` | list connectors |
  | `connector:write` | create and delete connectors |
  | `connector:token` | create, rotate and revoke tokens |
  | `ops:read` | every actuator endpoint on the management port |
  | `counter:read` / `counter:write` | read / increment and reset the counter |

- Permission to endpoint, SCIM chain: `user:read` for Users reads, `user:write` for Users `POST`/`PUT`/`PATCH`/`DELETE`, `group:read` / `group:write` likewise for Groups. A root `/.search` requires at least one read Permission and returns only the resource types the token may read. Discovery requires a valid token and no Permission. `/Me` keeps its current behaviour.
- A token can carry only the four directory Permissions. `audit:read`, `connector:*`, `ops:read` and `counter:*` are refused on a token.
- `ROLE_ADMIN` disappears. `ROLE_USER` stays as the baseline authority meaning "active and no password change required". It is not a Permission and is not configurable.

Role mapping

- A deployment configuration block defines Roles (name → Permissions) and the mapping (Group stable id → Role), and names one Group as the Superuser Group.
- Validation runs at startup, fails fast, and covers: unknown Permission, duplicate Group id, unknown Role, a Group id that does not resolve, no Superuser Group or more than one, a Superuser Role missing any Permission.
- A User's Permissions are the union of the Roles of the mapped Groups it is a direct member of. Group nesting is not modelled, as today.
- The application computes a hash of the validated mapping at startup.
- The development default mapping, shipped for local runs and tests and replaceable per deployment:

  | Role | Permissions | Mapped Group |
  |---|---|---|
  | Superuser | every Permission | the Superuser Group (the seeded Admin group) |
  | Account admin | `user:read`, `user:write`, `group:read` | a seeded account-admin Group |
  | Auditor | `audit:read` | a seeded auditor Group |
  | Connector admin | `connector:read`, `connector:write`, `connector:token`, `user:read`, `user:write`, `group:read`, `group:write` | a seeded connector-admin Group |
  | Monitoring | `ops:read` | a seeded monitoring Group |

  Connector admin holds the four directory Permissions because the no-escalation rule lets it mint only tokens it could itself hold.
- A read-only endpoint under `/api/admin/` returns every Role with its Permissions and the stable id and `displayName` of the Group mapped to it, and marks the Superuser Group. It requires `group:read`. No endpoint creates, changes or deletes a Role or a mapping entry.
- Role definitions and the mapping live in deployment configuration, not in the database. Role *assignment* is persisted in the database, as Group membership. This is a conscious deviation from the standalone standard's "role definitions are persisted in a relational database": configuration is already the single source of truth the standard requires, and a database copy would only add the synchronisation and archival the standard's role-sync recipe exists to manage.

Enforcement

- Each protected controller method declares its Permission with method security. Method security becomes enabled, which also closes issue #99.
- The application chain keeps URL rules as a backstop and is deny-by-default as a whole: its final rule denies every request no earlier rule matched. Self-service routes (`/api/auth/me`, `/api/auth/change-password`, `/api/auth/logout`, `/api/self`, `/api/session`) are listed explicitly as "authenticated, no Permission", public routes (`/api/auth/login`, `/api/auth/csrf`, health) explicitly as public, and every other route requires its declared Permission. The chain has no rule for `/scim/**`.
- Self-service routes act only on the session principal's own account. They take no account id from the request path, query or body, so no ownership check against a caller-supplied id is needed or possible.
- The SCIM chain stays stateless and bearer-only. Its authorization checks the token's Permissions, replacing the `scim.read` / `scim.write` scopes.
- A refusal is `403` with the existing refusal body on each chain.
- Unlock and force password change are refused with `403` when the target is the calling User's own account, whatever Permissions the caller holds.

Connector tokens

- Schema: a token's access level is replaced by a set of Permissions, in a new forward Flyway migration. There is no production deployment, so no data conversion is needed; development databases are migrated forward, never by editing an applied migration.
- The connector admin API takes and returns `permissions: string[]` in place of the access level, on token creation and rotation. Rotation keeps the token's Permissions unless new ones are given.
- No escalation: creation and rotation are refused when any requested Permission is not held by the calling User. An empty list is refused.

Superuser Group

- The Admin group's protections move to the Superuser Group: not renamable, not deletable, ordinary membership writable, the Bootstrap Admin's membership frozen.
- The "last enabled administrator" guard is removed. The frozen Bootstrap Admin, together with startup validation of the Superuser Role, guarantees an account holding every Permission.

Sessions

- A session holds the Permissions resolved at sign-in, and the mapping hash it was created under.
- When a membership change removes a mapped Group from a User, or a User's resolved Permissions shrink for any other reason, its sessions are revoked after commit (ADR 0002's pattern). The existing Admin-membership-removal hook is generalised to every mapped Group.
- Gaining a mapped Group does not touch live sessions.
- At startup, sessions carrying a different mapping hash are revoked.

Self-service contract

- `/api/auth/me` returns `permissions: string[]` sorted by name and no longer returns roles. It keeps every other field.
- The SPA's route guards and navigation decide on Permissions. Each administrative page names the Permission it needs: accounts → `user:read`, Groups → `group:read`, audit → `audit:read`, connectors → `connector:read`. Actions inside a page (unlock, token minting) are shown only with their own Permission.

Audit

- An authorization refusal records the caller (`user.id`, or the connector for a token), the operation and a generic reason ("insufficient permissions") on the existing administrative and SCIM refusal shapes, and at `WARN` in the ECS log. It never names the missing Permission, a Role or the policy, as the logging standard requires. The missing Permission is recoverable from the operation's declaration in the API document, since each operation requires exactly one.
- A membership change on a mapped Group records the Role gained or lost and the User it applies to.
- Token creation and rotation record the Permissions granted; a refused escalation records the Permissions refused.
- `ADMIN_MEMBERSHIP_REMOVED`, `INACTIVITY_DEACTIVATION` and `DORMANT_AUTHORITY_REVOCATION` are no longer written. `DORMANCY_LOCKOUT` and `DORMANCY_ROLE_REVOCATION` are added, both actorless.

Dormancy

- The two current jobs (inactivity deactivation, dormant-authority revocation) are replaced by a single dormancy job, serialised on its own scheduled job lock row (ADR 0005).
- Settings: `APP_DORMANCY_LOCKOUT_WINDOW` (default 90 days) and `APP_DORMANCY_ROLE_REVOCATION_WINDOW` (default 180 days), defaults owned by the dormancy policy as today. Startup fails on a non-positive window or a role-revocation window not longer than the lockout window. The old window settings are removed.
- The dormancy job runs daily at 04:00 `Asia/Singapore`, the service zone, on its own scheduled job lock row.
- The dormancy basis is unchanged, except that an Unlock now also resets it.
- Unlock of a `DORMANCY` lock behaves as Unlock does today: a User that holds a password is flagged for a mandatory password change, matching the standalone standard's rule for a re-enabled account. Unlock takes no reason field; the audit row records the acting administrator and the lock cause.
- A dormancy lock never lifts on its own. This is a conscious deviation from the standalone standard's 20-minute automatic lift, which is written for failure lockouts: a dormancy lock that expired by itself would undo the control. Failure lockouts keep their existing behaviour (ADR 0007).
- Lockout step: an unlocked, dormant User beyond the lockout window is locked with `lock_cause=DORMANCY`, its sessions revoked after commit. An already locked User keeps its lock and its cause.
- Role-revocation step: a User beyond the role-revocation window has its direct membership of every mapped Group removed. Each affected Group's version advances and the User's sessions are revoked after commit; one actorless `DORMANCY_ROLE_REVOCATION` event names the User and the Roles lost. Unmapped Group memberships are untouched. A connector may re-add a membership; while the User stays dormant the next run removes it again.
- The lockout step stands in for the standalone standard's 90-day "disable". This is a conscious deviation: a locked User cannot sign in and has its sessions revoked, which is what disabling achieves, while `active` stays owned by the directory.
- The Bootstrap Admin is exempt from both steps, by its reservation marker.
- Schema: `lock_cause` (`FAILURES` | `DORMANCY`) beside `locked_at`, set whenever `locked_at` is set and cleared with it. Failure lockouts write `FAILURES`.
- The admin account listing returns the lock cause. Login refusal is unchanged: a bare `401` for every cause.
- The application no longer writes `active` under any job.

Observability

- Each new record is classified with the existing ECS `event.*` vocabulary from the log schema, with no new `event.action` value:

  | Record | `event.action` | `event.type` | `event.outcome` | Level |
  |---|---|---|---|---|
  | Authorization refusal (either chain) | `access-control` | `denied` | `failure` | `WARN` |
  | Token issued or rotated with Permissions; escalation refused | `user-administration` | `creation` / `change` / `denied` | `success` / `failure` | `INFO` / `WARN` |
  | Role gained or lost through mapped-Group membership | `user-administration` | `change` | `success` | `INFO` |
  | Dormancy lockout | `user-administration` | `change` | `success` | `WARN` |
  | Dormancy role revocation | `user-administration` | `change` | `success` | `INFO` |
  | Mapping validated at startup (with its hash) / sessions revoked for a mapping-hash mismatch | `application-startup` | `info` / `change` | `success` | `INFO` |

  A dormancy lockout is logged at `WARN`, not the `ERROR` the authentication recipe uses for lockout, because it signals inactivity, not an attack. This is a conscious choice.
- The dormancy job follows the existing scheduled-job logging contract: its name, cron expression and timezone are logged at startup; each run sets `batch.job.name` and a fresh `batch.job.run.id` (used as `trace.id`) with `trigger.type=scheduled`; it logs `job-start` and `job-end` with `batch.job.status`, duration and the counts of Users locked and Users whose Roles were revoked, and all error fields on failure; job context is cleared after every run. It never logs inside the per-User loop except for the per-User audit records above.
- The dormancy job keeps the scheduled-job metrics the jobs it replaces had: runs by outcome, run duration, and counters of Users locked and Users whose Roles were revoked. An unexpected mass lockout or revocation is therefore visible as a counter spike.

Documentation

- `CONTEXT.md` gains Permission, Role, Role mapping and Superuser Group; redefines Lockout (a second cause), Dormant User and the Bootstrap Admin's guarantees; drops the Admin group as the source of `ROLE_ADMIN`, Inactivity deactivation and Dormant-authority revocation.
- The API document declares each operation's Permission in its `security` requirement, under the existing `sessionCookie` or `connectorBearer` scheme.

## Testing Decisions

A good test here goes through HTTP or a public application service against real Postgres, and asserts what a caller sees: status, body, audit rows, session state. It never asserts which annotation or filter made the decision. A security change also passes PIT and Semgrep, as the backend's conditional gates require.

- **Authorization contract (the main seam).** A contract test driven by the API document, next to the existing route contract test. For every documented operation it sends a caller holding every Permission except the declared one and expects `403`, and a caller holding only the declared one and expects neither `401` nor `403`. It also asserts that unlock and force password change on the caller's own account are refused, that a session cannot reach `/scim/**`, a token cannot reach `/api/**`, self-service operations need no Permission, an undeclared route under `/api/` is refused, and every documented operation declares a Permission or is explicitly listed as self-service or public. The existing route contract test keeps every mapped route documented, so a new route cannot escape the authorization contract. Prior art: the route contract and API contract fixture tests.
- **Token issuance.** Over HTTP: a token can only carry Permissions its creator holds; a non-directory Permission is refused; rotation keeps or replaces Permissions; a token with `group:write` alone is refused on Users writes. Prior art: the connector lifecycle integration test.
- **`/.search` filtering.** A `user:read`-only token gets Users and no Groups from a root search. Prior art: the SCIM query protocol integration test.
- **Superuser Group and propagation.** Over SCIM: the Superuser Group cannot be renamed or deleted, the Bootstrap Admin's membership cannot change, and other mapped Groups can be deleted. Removing a User from a mapped Group revokes its sessions; adding one does not. A session created under another mapping hash is revoked at startup. Prior art: the Group provisioning and seed integration tests, the in-memory session registry.
- **Dormancy job.** Against real Postgres with `MutableClock`, backdating the fixture's dormancy basis rather than moving the clock past live sessions. It covers: lock at the lockout window with `DORMANCY`; every mapped Group membership removed at the role-revocation window, unmapped ones kept; Bootstrap Admin exempt; Unlock resets the basis so the next run does not lock again; an existing `FAILURES` cause survives; `active` is never written; the audit events. Prior art: the inactivity governance integration test.
- **Startup validation.** Each invalid mapping and window combination fails startup with a message naming the problem. Prior art: the dormancy policy startup test.
- **Observability.** The dormancy job's startup schedule record, `job-start`/`job-end` records with counts, and metrics are asserted from captured ECS output and a metrics scrape; each new record carries the `event.*` values in the Observability table. Prior art: the ECS log capture and scheduled-job metrics tests.
- **Audit.** Refusals carry the caller, the operation and the generic reason, and neither the audit row nor the log line names a Permission or Role; mapped-Group membership changes carry the Role. Prior art: the audit event recording integration test.
- **SPA.** `/api/auth/me` decoding of `permissions[]`, route guards and navigation per Permission, and actions hidden without their Permission. Prior art: the existing route guard, API and accounts/connectors page tests. Mutation testing with Stryker applies to the guard code.

## Out of Scope

- Delivering security-event notifications (dormancy lockout, role revocation, unlock) anywhere other than the audit stream. The audit event is the record; outbound notification is App-Standards finding USR-8.
- Deleting dormant Users. Deletion remains the directory's job through SCIM `DELETE`.
- Editing Roles or the mapping at runtime, or any UI for it.
- Group nesting.
- Per-resource authorization (for example "may unlock Users in Group X only").
- Migrating existing data: there is no production deployment.
- The ADRs themselves, which are written after this spec is accepted.

## Further Notes

- ADRs: 0010 (fine-grained Permissions granted by Group, superseding the plan's authorization matrix) and 0011 (dormancy lockout and role revocation, superseding ADR 0008's dormancy jobs and extending ADR 0007's Lockout with a second cause), both Proposed.
- Whoever holds `group:write` on a mapped Group decides who holds that Role, and whoever holds `connector:token` together with `group:write` can issue a credential that does the same. Both are stated plainly in the documentation rather than split into a separate `role:assign`, because an identity provider sync must manage every Group.
- This work replaces issue #99 (method security) and closes App-Standards finding USR-2 (authorization matrix).
- Grilling record: `~/.kiro/crew/workspace/evidence/authz-redesign/decisions.md`.
