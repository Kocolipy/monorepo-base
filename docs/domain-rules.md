# Domain rules

The behavior behind the terms in `/CONTEXT.md`: what each concept does, the
invariants it keeps, and why. `/CONTEXT.md` is the glossary and says only what a
term means; this file is where the rules live, under the same term names. Where
a rule is also a wire contract, `backend/docs/openapi.yaml` is the authority on
statuses and bodies, and the ADRs in `/docs/adr/` record the decisions.

## Request paths

**Reserved server path** — a request path the backend answers for itself:
`/api`, `/actuator` and `/scim`, each reserving both the exact path and everything
beneath it. Reserved paths are authenticated by the filter chain and keep their
own error responses. `SpaRoutes.isReservedServerPath` is the only place the list
lives. `/scim` is reserved for a reason worth stating: without it a mistyped SCIM
path would be read as a client-side route and answered with the SPA shell and a
`200`, which a provisioning client would parse as a successful empty response
rather than as an error.

**SPA shell** — `index.html`, the single document the single-page application
boots from. A path with no server-side handler is _forwarded to the shell_ when
it belongs to the client-side router, which is how a deep link such as
`/showcase` survives a page reload.

**File request** — a path whose **last** segment contains a dot
(`/assets/index-a1b2c3.js`, `/favicon.ico`). A missing file request stays a 404;
it is never answered with the SPA shell, or a broken asset URL would return HTML
with a 200. A dot in an earlier segment (`/v1.0/settings`) does not make a path
one.

Avoid "frontend route" as a term: it blurred two different questions — whether a
path may be served without authentication (true of file requests) and whether a
missing path should become the shell (false of them) — which is how two modules
came to implement it twice and disagree.

## Sessions

**Session status** — what the SPA currently knows about the visitor's session:
`checking` before the one start-up check has answered, then `authenticated` or
`guest`. It is a _condition_, not an event, which is why a session ending is not
a fourth member. `resolveSessionRoute` is the only place the status decides what
a route does.

**Guest** — the session status of a person with no session. The status a cold
arrival starts in, and
the one a sign-out returns to.

**Expired session** — a session that _was_ authenticated and which the backend
has since refused with a `401`. The status becomes `guest` either way; what
distinguishes an expired session is its provenance, carried as `sessionExpired`
and passed into the redirect so the login route can say the session ended rather
than greeting a stranger. A `403` is **not** an expired session: a stale CSRF
token is re-fetched and the request retried once, and a `403` that survives that
is an authorization refusal. The session survives both.

**Return destination** — the protected path a visitor asked for before being
redirected to sign in, recorded as `from` in router state and replayed once the
status turns `authenticated`. Owned by the route guards, so no page navigates on
its own behalf after signing in.

**Idle timeout** and **absolute session lifetime** — the two independent bounds
on every authenticated session, an Admin's included: 15 minutes without a request
(`SESSION_TIMEOUT`) and 8 hours from creation (`APP_SESSION_ABSOLUTE_LIFETIME`).
Whichever is reached first ends the session, and the SPA cannot tell which; both
present as an expired session.

**Idle sign-out** — the SPA's own end of an idle session, timed by the
`idleTimeoutSeconds` the backend reports rather than a constant of the SPA's.
Only user input counts as activity, never a request, and it is shared across tabs;
a warning a minute before the limit offers to stay signed in, and staying is a real
request that renews the backend's clock too. At the limit the SPA logs out and
returns to login marked `inactive` — the code's name for this provenance, not the
glossary's. `/frontend/AGENTS.md` ("Backend contract") is the contract.

**One session per User** — an accepted Login ends every other session the User
holds, so signing in from a second browser signs the first out; the first sees an
expired session. It is one of the **session revocation** triggers below.

## Accounts and identity provisioning

### SCIM target model

**SCIM service provider** — the role this application plays for identity
provisioning: an external identity provider calls the application's SCIM v2
interface to create and manage Users and Groups. SCIM provisioning does not
perform an end-user Login; password authentication and its session remain a
separate application capability.

**SCIM directory** — the single User-and-Group namespace owned by one application
deployment. It is not partitioned by tenant. Multiple independently authenticated
read-write connectors may operate on the same directory; resource versions and
conditional requests provide their shared concurrency seam.

**Connector external identifier** — one connector's `externalId` alias for a User
or Group. A resource has one stable, directory-wide SCIM `id` but may have a
different `externalId` for each connector. Reads and filters expose only the
calling connector's alias, so independent client namespaces cannot collide. When
a connector is deleted, all of its aliases are deleted too and its namespace may
be reused by a future connector.

**Deleted SCIM connector** — a connector removed by an Admin. Deletion revokes
every token it holds and deletes every one of its `externalId` aliases in a single
transaction, and is not a row removal: the connector record survives with a
deletion timestamp so an audit event that names it still resolves for as long as
the trail is retained. Users and Groups are untouched — an alias is a connector's
name for a resource, not the resource. A deleted connector stops being listed and
stops authenticating at once, and its `externalId` namespace becomes available
again. Deleting one twice is refused, because an Admin repeating a delete is
likelier to have the wrong id than to want a second no-op.

**SCIM connector token** — a high-entropy opaque bearer credential restricted to
the SCIM interface. The value is a non-secret lookup handle, a dot, and at least
256 bits of `SecureRandom` material; only a SHA-256 digest of the **complete**
value is stored, compared in constant time. A token is either directory-wide
read-only or directory-wide read-write, with write implying read — and scope is
enforced in the SCIM chain's filter from the request's method and path, so a
`.search` POST remains a read and no handler carries a scope check of its own.
Any Admin may mint, inspect, overlap, rotate, and revoke tokens; plaintext is
disclosed once, on the issue and rotation responses alone, under
`Cache-Control: no-store`. A token expires at most 365 days after issue, which is
both the default and the hard maximum — a shorter lifetime may be chosen, a longer
one is refused rather than silently clamped. Rotation mints a replacement of the
same scope with a fresh full lifetime and brings the old token's expiry **forward**
to the end of an overlap window of at most 14 days, never past the expiry the old
token already had; a second rotation therefore cannot undo the first one's
shortening. Revocation and expiry are immediate and indistinguishable to the
connector: the only credential refusals the interface makes are a bare `Bearer`
challenge when no credential was presented, `invalid_token` for a malformed,
unknown, expired or revoked one or one whose connector is deleted, and
`insufficient_scope` for a read-only token attempting a mutation. The token is
accepted from the `Authorization` header and from nowhere else — a query string, a
form body and a cookie are not rejected but never consulted.

**User** — the domain identity that replaces Account rather than wrapping it.
It owns the selected core User profile, stable SCIM id and version, active state,
encoded password, creation metadata, and recent login history. Its profile
round-trips `userName`, the calling connector's `externalId`, `active`, `name`,
`displayName`, `emails`, locale and time-zone attributes, Groups, and SCIM
metadata. The Enterprise User extension and application-specific extensions are
not supported in the first release. SCIM may set the password as a write-only
provisioning attribute; the application hashes it immediately and never returns
it. SCIM may rename `userName` under its uniqueness rule while preserving the
stable SCIM resource id. A password or username change, deactivation, deletion, or removal from the Admin
group revokes the User's existing sessions so a stale login principal never
survives a security change (see **Session revocation**). Failure
runs and lockouts remain application-owned authentication behavior on the User.

The User is the only identity: the `accounts` table and the `Account`
aggregate are gone, Login authenticates against `scim_users`, and Admin authority
is derived from direct membership of the server-seeded Admin group rather than
read from a role column, which no longer exists.

**Normalized SCIM storage** — the PostgreSQL representation of the target model.
Selected User fields use relational columns, while emails, Groups, memberships,
connector aliases, resource versions, connector tokens, audit events, and
tombstones use constrained related tables. JSON resource blobs are not the
source of truth; supported filters and uniqueness are backed by relational
indexes and constraints.

**Bootstrap Admin** — the local recovery User excluded from SCIM write
authority so an administrator can recover the application when external
provisioning is unavailable or has removed every SCIM-managed administrator. It
is visible through the SCIM interface as a read-only User and an immutable member
of the Admin group: clients may discover its current state and authority, but no
SCIM operation may mutate or delete the User or remove that membership.

**Group** — a SCIM resource whose membership replaces the former Account role as
the source of elevated application authorization. Every active User receives
baseline User access without requiring membership in a redundant Users group.
A Group may contain direct User members only; Group-valued members and transitive
membership are unsupported. Users and Groups enter the SCIM interface together;
they are not separate future capabilities.

**Admin group** — the server-seeded Group whose members receive the authorization
the removed role column's `ADMIN` value used to grant, still carried in a session
as `ROLE_ADMIN`, in addition to baseline User access. Its stable
resource id carries that authorization meaning: SCIM may change ordinary
membership but may neither rename nor delete the Group, nor remove the Bootstrap
Admin's membership.

**SCIM tombstone** — the privacy-minimal record retained after SCIM deletion: the
resource type, stable resource id and deletion time (UTC), and nothing else. Its
table has no column that could hold a profile, credential or membership value, and
the application may insert and read tombstones but never change or remove one. It
holds no hash of a former identifier, by design: historical correlation is by
stable id through the audit stream. Tombstones never
participate in uniqueness checks: a former `userName`, Group `displayName` or
connector-scoped `externalId` may be reused by a future resource. Readable profile
and audit detail expire under the configured audit-retention policy.

**Deleted User** — a User removed with `DELETE`. Deletion removes its
live rows — profile, emails, credential and password history, Group memberships
and connector aliases — advances the version of every Group it belonged to,
revokes its sessions once the deletion commits, and leaves a SCIM tombstone. Every
later operation on its id is `404`. It is not merely a deactivated User, and nothing
readable about it survives outside the audit stream; the tombstone is the only
row that does. The Bootstrap Admin never enters this state because it cannot be
deleted.

**Deleted SCIM Group** — an ordinary, non-Admin Group removed with `DELETE`.
Deletion removes its memberships, makes it unavailable through SCIM, and leaves
a SCIM tombstone. The Admin group never enters this state because it cannot be
deleted.

**Practical SCIM protocol profile** — public discovery at
`/ServiceProviderConfig`, `/ResourceTypes`, and `/Schemas`, followed by
token-authenticated User and Group CRUD, PATCH, filtering, sorting, pagination,
conditional writes with ETags, and standard SCIM errors. A User may be created
without `password`; its `active` value remains authoritative, but password Login
returns the same bare `401` as any rejected credentials until a later SCIM write
sets one. Collection requests default `count` to 100 and clamp it to 200, while
returning `totalResults`, one-based `startIndex`, and `itemsPerPage`. Filtering
implements the complete RFC 7644 grammar over supported attributes, including
comparison, presence, boolean, grouping, and value-path expressions; unsupported
paths fail predictably rather than being silently misread. Sorting takes one
attribute, puts missing values last ascending and first descending, and breaks ties
by `id`. The same query may be sent as a `SearchRequest` body to `/Users/.search`,
`/Groups/.search`, or the base `/.search`, which spans both types and treats an
attribute one type lacks as having no value there. `PUT`, `PATCH`, and
`DELETE` of an existing resource accept an optional `If-Match`, as RFC 7644
allows: without one the write is applied unconditionally, last writer wins; when
sent it must be exactly one strong ETag, checked after authorization and
existence: a wildcard, list or malformed one is `400 invalidValue`, and a stale
version is `412`. Concurrent writers holding the same ETag are serialized on the
resource, so exactly one succeeds; concurrent unconditional writers are serialized
too, so both succeed and neither is half-applied. Unconditional writes are counted
per connector so an operator can see which integrations run without lost-update
protection. `externalId` is read-write on Users and Groups and writes
only the calling connector's alias: a `PUT` sets it to the submitted value or, when
omitted, removes it, and `PATCH` `add`/`replace`/`remove` set or clear it. Another
connector's alias for the same resource is never read or written. A password set
through `PUT` or `PATCH` is refused
when it matches, after normalization, any of the User's three most recent passwords,
the current one included. The first release advertises Bulk as
unsupported rather than implementing a partial `/Bulk` endpoint. Acceptance is
defined by the RFC contracts rather than behavior specific to Microsoft Entra
ID, Okta, or another vendor. The application adds no rate limiter, SCIM or
otherwise: per-request safety bounds are not rate limits, and throttling
`/scim/v2/**`, Login and the self-service change is the deployment edge's job,
specified in `infra/README.md` ("Edge throttling").

Every capability above is implemented, and `ServiceProviderConfig` advertises
`patch`, `filter`, `sort`, `etag` and `changePassword` as supported. Bulk's
`supported: false` is permanent. The rule the profile was reached under still
binds anything added later: discovery advertises a capability only once it is
implemented, and a request for one that is not is refused rather than ignored —
an ignored `filter` is indistinguishable from a match, which is the one failure a
connector cannot detect.

**SCIM audit trail** — the append-only local history of provisioning and connector
token activity, and of the authentication, lockout, administrative, password-change
and scheduled-job events recorded in the same stream. An event records the actor's
stable id — the Admin or User for an authentication or administrative event, the
connector (never the token) for a SCIM or token-lifecycle one, nobody for a
scheduled job — plus operation, resource type and id, outcome, changed attribute
paths, error classification and timestamp; it never records a password, bearer
token, hash, `userName` or other profile value. Every collection query and search is
one bulk-read event carrying the number of resources returned and the filter's
shape (`userName eq ?`), never its values; a single-resource read is not recorded.
Admins read it through the **audit listing** (`GET /api/admin/audit-events`): newest
first, paginated, filterable by operation, outcome, actor, resource and time window.
The listing returns the stored events as they are — redaction lives in what an event
can hold, not in the read — and reading the trail is not itself recorded. The
Accounts page has no audit view yet; this listing is the read one would be built on.
Deployment configuration controls retention with a one-year default.

### Current account model

**Guest** — a person with no authenticated session. A Guest may use only the
login page; asking for a protected route records the return destination and
sends them there.

**User** — any identity in the SCIM directory, signed in or not. Every active
User with a session receives baseline access:
the counter page at `/showcase` and its own password change, but not account
administration, in the browser or
over the API. There is no role column: baseline access is `ROLE_USER`, granted to
every session not confined by the change-required flag.

**Admin** — a User that is a direct member of the **Admin group**. Admin authority
is derived from that membership at Login, not stored, and the session carries it as
`ROLE_ADMIN` alongside `ROLE_USER`. An Admin may use the counter page, the accounts page at `/accounts`,
and the administration API under `/api/admin/**`.

**Account** — **gone.** There is no longer a separate login identity: the
`accounts` table and its aggregate were removed when the User became the one
identity this application has, owning the profile, the credential and the
authentication state together. The term survives only in two route paths
(`/api/admin/accounts`, the SPA's `/accounts`) that were not worth churning.

What replaced each of its parts: `username` → the User's `userName`,
`password` → its `password_hash`, `enabled` → SCIM's own `active`, `role` →
DERIVED membership of the **Admin group**, the login history → the failure run and
`locked_at` on the same row, and the creation timestamp → the resource row's
`created_at`, which is no longer nullable because a resource cannot exist without
one. Startup seeding creates the configured ordinary and recovery identities when
their `userName`s are absent and never overwrites one that exists — so a rotated
recovery password survives a restart.

**Login** — the one operation that turns submitted credentials into an
authentication or a refusal, and the only thing that records an attempt against
the failure run. It lives in `LoginService`, so an entry point that authenticates
submitted credentials without going through it has no **lockout** at all; the
`/api/auth/login` endpoint adds only the session, the CSRF token, and the bare
`401`. Why the counting is recorded here rather than driven by Spring Security's
authentication events is
`docs/adr/0001-count-login-attempts-on-the-login-path.md`.

**Failure run** — the consecutive rejected logins recorded against one User,
counted on the User's own row as `failed_login_attempts`. A login the backend
accepts ends the run and returns the count to zero; a login it rejects lengthens
it, and so does a wrong current password on the self-service password change. An unknown username has no run, because nothing is recorded for a name that
names no User.

**Lockout** — the state a User enters once its failure run reaches the
configured limit (`app.auth.lockout.max-attempts`, default 3), closing it to
logins **permanently**: there is no duration, no configuration key expressing one,
and no passage of time that lifts it. The only thing that ends it is an Admin
performing Unlock (ADR 0007). The User's row records `locked_at`, the instant the lock was
imposed, so "is it locked" is a question about the row rather than a comparison
against a clock. A locked User is refused **with its correct password**, and
refused the same way as a wrong one: a bare `401` with no body, so the response
never reveals that the User exists or that it is locked — a User who cannot get
in learns nothing by waiting, which is intended. Attempts made while it holds
neither count nor deepen it. Imposing it **revokes the User's live sessions**,
after the transaction commits, so a locked User stops acting immediately rather
than when the session it already held expires. Enforcement is Spring Security's,
which checks the User's status before it compares passwords; the counting is the login
path's. The lockout is the per-account half of brute-force deterrence on Login; the
deployment edge throttles the rest, because it cannot see the `userName` in a Login
body (`infra/README.md`, "Edge throttling").

**Bootstrap Admin exemption** — the seeded Admin
(`app.auth.secondary-username`) is the deployment's local recovery identity and is
the one principal lockout never applies to. Its failed attempts are counted and
audited as `LOGIN_FAILURE` like anyone's, but no run of them locks it. With no
automatic lift, a lockable recovery account would let an unauthenticated attacker
brick the deployment; the accepted cost is unbounded online guessing against that
single account, answered by the Argon2id verification cost every attempt pays, the
uniform refusal, and the audited failures — not by a lock.

**Deactivated User** — a User whose SCIM `active` attribute is false, set by a
connector's SCIM write or by the inactivity-deactivation job. The Accounts page reports it and cannot change
it: `active` is directory-owned. It is refused at login exactly as a locked User
is: a bare `401`, indistinguishable from a wrong password, so the response reveals
nothing. The flag is never merely reported.

There is one flag, not two. `active` was SCIM's and `enabled` was the account
aggregate's, and they meant the same thing — "may authenticate" — so keeping both
after the identities merged would have left one of them unreachable over the wire.

**Deactivating** and **unlocking** are **two separate capabilities**, and neither
performs the other. Deactivation settles whether a User is permitted at all, and
is the directory's; unlocking settles whether it is being penalised for failed
logins right now, and is an Admin's. So:

- Deactivating a User leaves its failure run and `locked_at` as they stand.
  The run is evidence, and it is most wanted at the moment a User is being
  closed.
- Reactivating a User leaves a lockout it is serving in force. Restoring access
  is not a finding that the failed logins did not happen; the lockout still ends
  only when an Admin unlocks it.
- Unlocking ends a lockout and clears the failure run with it, and says
  nothing about the `active` flag. A deactivated User can be unlocked and stays
  deactivated. Unlocking a User that has a password also sets its
  **change-required flag** (below); an Admin cannot unlock their own account.

**Change-required flag** — application-owned state on a User saying its current
password was imposed by somebody else and must be replaced before the User may do
anything else. Stored as `password_change_required_since`: its presence is the
flag, as `locked_at`'s is the lockout, and its value is when the change was last
required. There is no deadline for the change; see **Dormancy basis** and ADR 0008. It is not a SCIM attribute, so setting it does not advance the version.
It is **set** by every connector password write (create, PUT or PATCH carrying a
password), by a **forced password change**, by an **Unlock** of a User that has
a password — the credential that reached the lockout threshold may be the one an
attacker was guessing — by a reactivation of a User that has a password, since a
credential that sat unused across a deactivation is not trusted on return, and by
seeding the Bootstrap Admin, whose first password comes from deployment
configuration. A credentialless User is unlocked or reactivated without it, having
no password to replace. It is **cleared** only by a successful self-service change;
a connector write never clears it.

**Confined session** — a session issued while the change-required flag is set. It
holds no role, an Admin's included, so it may call only `GET /api/auth/me`, the
self-service change and logout; every other endpoint, `/api/admin/**` included,
answers `403`.

**Forced password change** — an Admin action setting the change-required flag on
another User and ending every session it holds. The Admin never sees, chooses or
transports the password. Refused on the acting Admin's own account, on the
Bootstrap Admin by anyone but itself, and (`409`) on a credentialless User.

**Self-service password change** — `POST /api/auth/change-password`, for the User
the session belongs to. A wrong current password lengthens the same failure run as
a rejected Login, so it leads to the same lockout; a new password that breaks the
password policy or repeats a recent one is refused by naming the rule, never
echoing either value. Success hashes the new password, clears the flag, advances
the version, records a `PASSWORD_CHANGE` audit event with no password value, and
revokes every session of the User, the submitter's included.

**Recovery guard** — what keeps the deployment recoverable now that Admins no
longer deactivate anyone. Two rules. An Admin may not Unlock or force-change their
own account (`403`), so recovering from a self-inflicted state takes a second
Admin — and the Bootstrap Admin may flag only its own password. And the Bootstrap
Admin can never be locked and is protected from every SCIM write, deactivation
included, so a deployment whose other Admins are all locked is still recoverable:
it signs in and unlocks them.

The self-target check compares NORMALIZED `userName`s: a session names its
principal by whatever spelling it logged in with, and a raw comparison would let
an Admin whose session carried a differently-cased spelling of their own name act
on themselves past the guard. The Accounts page hides both controls on the
signed-in Admin's own row, comparing the same way, so the refusal is visible before
the click.

The Bootstrap Admin's protections are recognised by the **reservation marker** on the User's own resource
row, not by comparing its name to the configured one. A name comparison could be
moved by a rename, and a second identity could acquire the exemption by taking the
configured name; a marker written once by seeding, in a column no UPDATE reaches,
can do neither. The same marker protects the resource from every SCIM write.

**Users projection** — what identity administration may know about a User, one
row per User on the Accounts page (`GET /api/admin/accounts`): its stable resource
id, `userName` and display name, whether it is the Bootstrap Admin, whether the
**Admin group** confers administrative authority on it, its `active` flag, whether
a credential is set at all, whether a lockout is in force, whether a change is
required, its last authentication, its creation timestamp and its direct Groups.
The directory-owned fields — identity, `active`, Groups — are read-only there.
Never the password hash, which no projection type has a field for. There is no field for when a lockout lifts,
because none does: the flag is the whole lock state, and what ends it is an Admin's
Unlock. Both refusal mechanisms appear because either alone would mislead — a User
locked out right now looks healthy if only `active` is shown, and nothing would say
which need unlocking.

The administrative flag is DERIVED at read time from Admin-group membership rather
than stored, so the projection reports the same fact the login path derives and there
is no column for the two to disagree about. Whether a credential exists is reported
because "no password was ever set" is otherwise indistinguishable from "the
password is wrong", and only the first is fixed by a SCIM write. The Bootstrap
Admin's row shows no lockout state at all: it can never be locked, so "not locked"
would describe a condition that could change.

**Groups projection** — what identity administration may know about a Group, one
row per Group on the Accounts page (`GET /api/admin/groups`): its name, its direct
member count, and whether it is the protected Admin group, decided by the
reservation marker. Read-only in its entirety; Groups and membership are the
directory's.

**Accounts page** — the SPA screen at `/accounts`, an Admin's operational view.
The route keeps its name from the removed account aggregate; what it shows is the
**Users projection** and the **Groups projection**, both read-only for everything
the directory owns, plus the application-owned operations: **Unlock** (offered only
while a lockout is in force, and described as the only way a lockout ends — one that
also requires the User to change its password), the **forced password change**
(offered only on a credentialed User not already flagged), and connector and token
management with one-time plaintext disclosure. Both User operations address the
User by its stable id. It offers neither operation on the signed-in Admin's own
row, and no Unlock on the Bootstrap Admin, so the backend's refusals are visible
before the click. There is no Deactivate or Activate: `active` is the directory's,
and the backend has no endpoint that would accept a write to any directory-owned
field. The page never decides authorization — it renders behind the `ADMIN` guard,
and the backend refuses `/api/admin/**` to any other role regardless.

**Dormancy basis** — the instant a User's dormancy is measured from: its
`lastAuthenticatedAt` — set by every successful Login made while no password
change is required, by a completed self-service change, and by an explicit
reactivation — or, for a User that has had neither, its creation time. The
fallback is what keeps a User provisioned without a password from being dormant
the moment it exists. A confined session's Login does not move it, so a
credential imposed on a User that is never replaced still ages into deactivation. Application-owned authentication state, like the failure
run: not a SCIM attribute, absent from `/Schemas`, and writing it moves no version.

**Dormant User** — a User whose dormancy basis is further in the past than a
configured window. Dormancy is relative to a window, not a stored state: the same
User can be dormant for deactivation (90 days by default,
`APP_DORMANCY_DEACTIVATION_WINDOW`) and not yet for authority revocation (180 days,
`APP_DORMANCY_AUTHORITY_REVOCATION_WINDOW`). The Bootstrap Admin is never treated
as dormant by either job, by its reservation marker, for the reason it is exempt
from lockout. Avoid "inactive" for this: a deactivated User is one whose `active`
flag is false, which a dormant User may or may not be.

**Inactivity deactivation** — the scheduled job that deactivates every active
dormant User: `active=false`, the version advanced, its sessions revoked after
commit, and an actorless `INACTIVITY_DEACTIVATION` event. It takes priority over
the directory. A connector re-asserting `active=true` resets nothing — a write
that changes nothing writes nothing — so it cannot hold a dormant User open; only
an explicit reactivation, a stored transition of `active` from false to true
through SCIM, resets the dormancy basis. A User reactivated
while still dormant by a later run is deactivated again, by design.

**Dormant-authority revocation** — the scheduled job that removes a dormant
User's direct membership of the Admin group, and nothing else: baseline access is
the inactivity job's business, and ordinary Group memberships confer no authority.
The Admin group's and the User's versions advance, the User's sessions are revoked
after commit, and an actorless `DORMANT_AUTHORITY_REVOCATION` event names the User.
A connector may re-add the membership; while the User stays dormant the next run
removes it again.

**Scheduled job lock** — how the two dormancy jobs and the audit retention job are
serialized: each run takes its own job's row in `scheduled_job_locks` with
`FOR UPDATE SKIP LOCKED` and holds it for the run's transaction. A second run of the
same job, on any instance, skips; another job holds a different row and never waits. Inside a run, each User is
re-read under its resource lock and decided again, so no User is processed twice.

**Session revocation** — ending the sessions a User is already holding, so its
next request arrives as a Guest and the SPA sends it back to login. Sessions are
found by the User's stable id, never its `userName`, so a rename cannot hide one.
The triggers in force:

- an Admin forcing its password change, and the login path imposing a lockout on it;
- a SCIM write that takes `active` from true to false;
- a SCIM write that sets, changes or removes its password;
- a SCIM write that changes its `userName`;
- a SCIM `DELETE` of the User;
- a SCIM write that removes it from the Admin group (an addition takes effect at
  its next Login, since authority is derived only then);
- its own successful Login, which ends every other session it holds — one
  session per User;
- its own successful self-service password change;
- inactivity deactivation and dormant-authority revocation, by their scheduled
  jobs — recorded with no actor, because the job is not a principal.

Every trigger defers the revocation until after its transaction commits, so a
write that was refused, stale or rolled back revokes nothing, and one SCIM write
that moves several of those attributes revokes once. A SCIM-triggered revocation is
audited as its own event, with its outcome, after the commit; if the session store
fails, the write stands and the connector receives an error. Nothing else revokes:
an ordinary profile or email change, a reactivation, an alias, a Group rename and
Unlock touch no session. Reactivation gives nothing back (a revoked session is
gone; the User signs in again). A refused forced change revokes nothing, which is
what keeps an Admin who mis-clicks their own row from signing themselves out. A
failure run that stops short of the limit revokes nothing either.

Revocation is possible only because sessions are indexed by principal
(`spring.session.data.redis.repository-type: indexed`, set in
`backend/src/main/resources/session.yaml`). Without that index a
session store can be read by id alone, so the ones belonging to a username cannot
be found; the application refuses to start rather than accept a revocation it cannot
enforce.

It is not a lock. A login already in flight when the deactivation commits can still
mint a session that the revocation did not see, because it read the User as
active. Once the write is committed no further login succeeds, so the gap is one
transaction wide rather than open-ended.
