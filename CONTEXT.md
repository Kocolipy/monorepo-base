# CONTEXT

Glossary of the terms this codebase uses for its own concepts. Seeded lazily —
a term lands here when a decision actually resolves it, so absence of a term
means nobody has needed to pin it down yet, not that the concept is undefined.

Use these words in code, tests, commit messages and issues. Where a synonym is
listed as avoided, it is avoided because it already means something else here.

## Request paths

**Reserved server path** — a request path the backend answers for itself:
`/api` and `/actuator`, each reserving both the exact path and everything
beneath it. Reserved paths are authenticated by the filter chain and keep their
own error responses. `SpaRoutes.isReservedServerPath` is the only place the list
lives.

**SPA shell** — `index.html`, the single document the single-page application
boots from. A path with no server-side handler is _forwarded to the shell_ when
it belongs to the client-side router, which is how a deep link such as
`/account/settings` survives a page reload.

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

**Guest** — a visitor with no session. The status a cold arrival starts in, and
the one a sign-out returns to.

**Expired session** — a session that _was_ authenticated and which the backend
has since refused with a `401`. The status becomes `guest` either way; what
distinguishes an expired session is its provenance, carried as `sessionExpired`
and passed into the redirect so the login route can say the session ended rather
than greeting a stranger. A persistent `403` is **not** an expired session — that
is a stale CSRF token, and the session survives it.

**Return destination** — the protected path a visitor asked for before being
redirected to sign in, recorded as `from` in router state and replayed once the
status turns `authenticated`. Owned by the route guards, so no page navigates on
its own behalf after signing in.

## Accounts and roles

**Visitor** — an unauthenticated person. A Visitor may use only the login page;
asking for a protected route records the return destination and sends them there.

**User** — an authenticated account whose role is `USER`. A User may use the
counter page at `/showcase` but not account administration, in the browser or
over the API.

**Admin** — an authenticated account whose role is `ADMIN`. An Admin may use the
counter page, the accounts page at `/accounts`, and the administration API under
`/api/admin/**`.

**Account** — a database-backed login identity with one username, encoded
password, role, enabled flag, and creation timestamp, plus the state of its
recent login history. Startup seeding creates the configured User and Admin only
when their usernames are absent; it does not overwrite an existing account,
though it does fill in a creation timestamp a pre-existing account has none of.

**Login** — the one operation that turns submitted credentials into an
authentication or a refusal, and the only thing that records an attempt against
the failure run. It lives in `LoginService`, so an entry point that authenticates
submitted credentials without going through it has no **lockout** at all; the
`/api/auth/login` endpoint adds only the session, the CSRF token, and the bare
`401`. Why the counting is recorded here rather than driven by Spring Security's
authentication events is
`docs/adr/0001-count-login-attempts-on-the-login-path.md`.

**Failure run** — the consecutive rejected logins recorded against one account,
counted on the account itself as `failed_login_attempts`. A login the backend
accepts ends the run and returns the count to zero; a login it rejects lengthens
it. An unknown username has no run, because nothing is recorded for a name that
names no account.

**Lockout** — the state an account enters once its failure run reaches the
configured limit (`app.auth.lockout.max-attempts`, default 3), closing it to
logins until `locked_until` has passed (`app.auth.lockout.duration`, default 5
minutes). A locked account is refused **with its correct password**, and refused
the same way as a wrong one: a bare `401` with no body, so the response never
reveals that the account exists or that it is locked. The window is a fixed
penalty — attempts made during it neither count nor extend it — and once it
expires the next rejected login starts a fresh run rather than re-locking on the
old count. Enforcement is Spring Security's, which checks account status before
it compares passwords; the counting is the login path's.

**Disabled account** — an account whose `enabled` flag is false, set by an Admin
through account administration. It is refused at login exactly as a locked
account is: a bare `401`, indistinguishable from a wrong password, so the
response reveals nothing. The flag is never merely reported.

**Enabling** and **unlocking** are **two separate capabilities**, and neither
performs the other. Enabling settles whether an account is permitted at all;
unlocking settles whether it is being penalised for failed logins right now. So:

- Disabling an account leaves its failure run and `locked_until` as they stand.
  The run is evidence, and it is most wanted at the moment an account is being
  closed.
- Enabling an account leaves a lockout it is serving in force. Restoring access
  is not a finding that the failed logins did not happen; the lockout still ends
  when it expires, or when someone unlocks it.
- Unlocking ends a lockout early and clears the failure run with it, and says
  nothing about the `enabled` flag. A disabled account can be unlocked and stays
  disabled.

Restoring an account that was both suspended and locked out therefore takes two
deliberate calls. That is the point: an Admin should have to say which of the two
they mean.

**Recovery guard** — account administration refuses two disable requests
outright, with a `409`: an account disabling itself, and the last enabled Admin.
Both would leave nobody able to enable anything again, and nothing in the system
could undo either without direct database access. A _locked_ Admin still counts
as available, because that lockout ends on its own.

**Account listing** — what account administration may know about an account:
username, role, enabled flag, whether a lockout is in force, when that lockout
lifts, and the creation timestamp. Never the password hash, which no
listing type has a field for. Both refusal mechanisms appear because either alone
would mislead — an account locked out right now looks healthy if only `enabled`
is shown, and nothing would say which accounts need unlocking. Whether the
lockout is in force is the server's own evaluation at the moment it answers, not
a comparison the client makes against its own clock.

**Accounts page** — the SPA screen at `/accounts`, an Admin's view of the account
listing and the only place the two capabilities are exercised from a browser.
Each row reports both refusal mechanisms and offers the action that would change
it: Disable or Enable, and Unlock only while a lockout is in force. It offers no
Disable for the signed-in Admin's own account, so the recovery guard's refusal is
visible before the click rather than as a `409` after it. The page never decides
authorization — it renders behind the `ADMIN` guard, and the backend refuses
`/api/admin/**` to any other role regardless.

**Session revocation** — disabling an account ends the sessions it is already
holding, so its next request arrives as a Guest and the SPA sends it back to
login. Disabling is the only administrative action that does this: enabling gives
nothing back (a revoked session is gone; the account signs in again), and
unlocking touches no session at all. A refused disable — either arm of the
recovery guard — revokes nothing, which is what keeps an Admin who mis-clicks
their own row from signing themselves out.

Revocation is possible only because sessions are indexed by principal
(`spring.session.data.redis.repository-type: indexed`). Without that index a
session store can be read by id alone, so the ones belonging to a username cannot
be found; the application refuses to start rather than accept a disable it cannot
enforce.

It is not a lock. A login already in flight when the disable commits can still
mint a session that the revocation did not see, because it read the account as
enabled. Once the write is committed no further login succeeds, so the gap is one
transaction wide rather than open-ended.
