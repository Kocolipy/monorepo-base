# Backend

A Java 25 / Spring Boot backend with HTTP sessions persisted in Redis and user
counts persisted in PostgreSQL. Spring
Session replaces the servlet container's in-memory session, so session state can
survive application restarts and be shared by multiple application instances.

## Prerequisites

- Java 25
- Maven — not required; the checked-in wrapper (`./mvnw`) downloads and
  checksum-verifies the pinned 3.9.11 release
- Docker with Docker Compose (recommended for local Redis and PostgreSQL)

## Run locally

Start Redis and PostgreSQL:

```bash
docker compose up -d redis postgres
```

Start the application:

```bash
./mvnw spring-boot:run
```

The service listens on `http://localhost:8080`. Its health endpoint is
`GET /actuator/health`.

## Log in

Authentication is database-backed, and the identity that logs in is a **SCIM
User** — there is no separate account table. On startup the service idempotently
seeds an ordinary User, the **Bootstrap Admin**, and the server-reserved **Admin
group** with the Bootstrap Admin as its immutable member, creating whichever is
absent. The development defaults are `user` / `P@ssw0rd` and `admin` /
`P@ssw0rd`; `APP_USERNAME` / `APP_PASSWORD` configure the ordinary User and
`APP_SECONDARY_USERNAME` / `APP_SECONDARY_PASSWORD` configure the Bootstrap
Admin. These published defaults must not be used in production.

There is no role column. Every active User holds `USER`; direct membership of the
Admin group additionally grants `ADMIN`. Authority is derived when a session is
created, so a User added to or removed from the Admin group gains or loses `ADMIN`
at their next login, never mid-session.

Three consecutive refused logins lock an account (`APP_LOCKOUT_MAX_ATTEMPTS`,
default 3), and the lock is **permanent**: it has no duration, nothing lifts it as time passes, and an `ADMIN` performing
Unlock is the only thing that ends it. Imposing it also revokes that account's
live sessions, so a locked account stops acting immediately rather than when the
session it already held expires. While the lockout holds the correct password is
refused too, and every refusal — unknown username, a credentialless account,
wrong password, locked account — answers with the same bare `401` after an
equivalent Argon2id verification, so the response cannot be used to find out
which accounts exist, which have a password set, or which are locked. An accepted
login resets the count. `APP_LOCKOUT_MAX_ATTEMPTS` configures the threshold, with
no enforced floor on the value; there is no duration setting to configure.

The Bootstrap Admin (`APP_SECONDARY_USERNAME`) is the deployment's recovery
identity and is the one User exempt from lockout: its failed attempts are
counted and audited, but it never locks. Without that exemption a permanent
lockout would let an unauthenticated attacker brick the deployment by guessing at
the recovery account until it closed. The accepted cost is unbounded online
guessing against that one account, answered by the Argon2id verification cost
every attempt pays, the uniform refusal, and the audit trail — not by a lock.

A session is bound by two independent limits. It is dropped after
`SESSION_TIMEOUT` (default 15 minutes) of inactivity — the servlet container's
own idle timeout, reset by every request — and separately terminated once it has
existed for `APP_SESSION_ABSOLUTE_LIFETIME` (default 8 hours), regardless of how
recently it was used. Both bounds apply to every authenticated session, `ADMIN`
included; whichever is reached first ends the session.

Every unsafe request (POST, PUT, PATCH, DELETE) needs the session's CSRF token in
the `X-CSRF-TOKEN` header. It is fetched, never read from a cookie: `GET
/api/auth/csrf` returns it in the body (and opens a session when there is none).
Login discards the pre-login token, so fetch again once signed in. Log in and keep
the returned session cookie in a cookie jar:

```bash
csrf() { curl -s -c cookies.txt -b cookies.txt http://localhost:8080/api/auth/csrf | jq -r .token; }

curl -c cookies.txt -b cookies.txt \
  -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -H "X-CSRF-TOKEN: $(csrf)" \
  -d '{"username":"admin","password":"P@ssw0rd"}'

token=$(csrf)

curl -b cookies.txt http://localhost:8080/api/auth/me
```

All API endpoints other than login, the CSRF token and the health check require
that cookie. Counter and session endpoints accept either authenticated role;
administration endpoints under `/api/admin/**` require `ADMIN`. Continue sending
the cookie, and the token on unsafe requests, when using the session API:

```bash
curl -b cookies.txt http://localhost:8080/api/session

curl -b cookies.txt \
  -X PUT http://localhost:8080/api/session \
  -H 'Content-Type: application/json' \
  -H "X-CSRF-TOKEN: $token" \
  -d '{"displayName":"Ada"}'

curl -b cookies.txt http://localhost:8080/api/session

curl -b cookies.txt -X DELETE -H "X-CSRF-TOKEN: $token" \
  http://localhost:8080/api/auth/logout
```

Review who has access. This needs an `ADMIN` session; a `USER` session is
answered with `403`, and the listing never contains a password hash:

```bash
curl -b cookies.txt http://localhost:8080/api/admin/accounts
```

```json
[
  {
    "id": "0b6f2c1e-8d7a-4f1e-9a3b-2c5d6e7f8a90",
    "userName": "admin",
    "admin": true,
    "active": true,
    "locked": false,
    "hasPassword": true,
    "createdAt": "2026-01-02T03:04:05.123456Z"
  }
]
```

`admin` is derived from Admin group membership at read time; it is not a stored
field.

Control an identity. Activating and unlocking are **separate capabilities**:
deactivating leaves the failure run standing, activating leaves a lockout
standing, and unlocking says nothing about `active`. Each is a POST, so each
needs the CSRF header (`$token`, fetched above):

```bash
curl -b cookies.txt -X POST -H "X-CSRF-TOKEN: $token" \
  http://localhost:8080/api/admin/accounts/user/disable

curl -b cookies.txt -X POST -H "X-CSRF-TOKEN: $token" \
  http://localhost:8080/api/admin/accounts/user/enable

curl -b cookies.txt -X POST -H "X-CSRF-TOKEN: $token" \
  http://localhost:8080/api/admin/accounts/user/unlock
```

Each answers `200` with the identity as it now stands, `404` for an unknown
username, and `409` when the change is unsafe — deactivating yourself, the
Bootstrap Admin, or the last active administrator. Deactivating revokes the
User's live sessions once the change commits, as well as stopping new logins.

Increment or reset the count belonging to the authenticated user:

```bash
curl -b cookies.txt http://localhost:8080/api/count
curl -b cookies.txt -X POST -H "X-CSRF-TOKEN: $token" http://localhost:8080/api/count/increment
curl -b cookies.txt -X POST -H "X-CSRF-TOKEN: $token" http://localhost:8080/api/count/reset
```

## Configuration

Copy `.env.example` to `.env` if your runtime loads dotenv files, or export the
variables in your shell. The defaults connect to Redis at `localhost:6379`,
PostgreSQL at `localhost:5432`, and expire sessions after 15 minutes of
inactivity — the same value every environment uses, including deployed ones
(`infra/infrastructure.yaml`), so the SPA can rely on a single window. Override
`DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` in deployed
environments.

Set `SESSION_COOKIE_SECURE=true` when serving the application over HTTPS. Store
real Redis credentials in your deployment's secret manager; do not commit them.

### Logging

| Variable                | Default  | Meaning                                                      |
| ----------------------- | -------- | ------------------------------------------------------------ |
| `LOG_FILE`              | (unset)  | Also write ECS JSON to this file, rolling; unset means no file |
| `APP_ENVIRONMENT`       | `local`  | `service.environment` on every record                        |
| `LOG_STRUCTURED_FORMAT` | `ecs`    | Console format; set empty for Boot's human-readable pattern  |

Records are ECS JSON on stdout. Each one carries `service.name` (`backend`),
`service.version` (the built project version), `service.environment`, and an
`@timestamp` in Singapore time (`2026-10-01T16:52:11.726+08:00`). Only the log is
in that zone: the JVM's default zone is not changed, so SCIM `meta` times and
audit times stay UTC (`...Z`). The scheduled jobs' cron expressions are evaluated
in the same `Asia/Singapore` zone, so a job's schedule and its records agree.

Every record emitted inside a request, or inside a scheduled-job run, carries
`trace.id` and `span.id`. The ids are minted here: tracing is on for correlation
only, nothing is exported and an inbound `traceparent` is ignored
(`src/main/resources/telemetry.yaml`).

With `LOG_FILE` set (deployed: `/var/log/backend/backend.json`, see
`/infra/README.md` under "View Logs") the same records are also written to that
file, one JSON object per line, rolled daily or at 50 MB, kept 14 days, capped at
1 GB in total. Stdout keeps working. Local development and the tests leave it
unset and write no file. The settings live in `src/main/resources/logging.yaml`.

### SCIM release gate

| Variable           | Default | Meaning                                    |
| ------------------ | ------- | ------------------------------------------ |
| `APP_SCIM_ENABLED` | `true`  | Whether this deployment serves `/scim/v2`  |

**On by default.** The discovery documents (`ServiceProviderConfig`,
`ResourceTypes`, `Schemas`) are public; the `Users` and `Groups` resource
endpoints still answer `401` without a connector token. Set
`APP_SCIM_ENABLED=false` to turn the interface off: the whole `/scim/v2`
namespace then answers `404` — public discovery included, and ahead of
authentication, so a valid connector token gets the same answer as none at all.

The reason it exists: SCIM Users and Groups are one release capability. A directory
that can create Users but has no Groups cannot express authority, so the gate kept
the User half unreachable until the Group half was built. Both halves now exist.

The value is not written in `application.yaml`: the default belongs to
`ScimSecurityConfig`, so an unset variable reaches the gate as open rather than as
whatever a replaced config file happens to say.

### Audit trail retention

| Variable                       | Default        | Meaning                                       |
| ------------------------------ | -------------- | --------------------------------------------- |
| `APP_AUDIT_RETENTION_PERIOD`   | `365d` (1 year)| How long a recorded audit event is kept       |
| `APP_AUDIT_RETENTION_SCHEDULE` | `0 30 3 * * *` | When the retention job runs (Spring cron)     |

The **floor is 90 days**, and it is enforced rather than advised: a configured
period below it fails startup with the value in the message, instead of quietly
keeping less history than an investigation needs. `90d` itself is allowed. Neither
value appears in `application.yaml` — both defaults belong to
`AuditRetentionPolicy`, so an unset variable reaches the rule as unset and "the
default is one year" is a fact about the rule rather than about a config file.

Each run logs its schedule at startup and, per run, the rows it deleted and how
long it took (`event.action: audit.retention`). A run that deleted nothing is
logged too — "nothing had aged out" and "the job has not run for a month" are
different facts.

### Operational telemetry

| Variable                 | Default    | Meaning                                              |
| ------------------------ | ---------- | ---------------------------------------------------- |
| `MANAGEMENT_SERVER_PORT` | the app's  | Serve `/actuator/**` on this port instead of the app's |

`/actuator/prometheus` is the metrics scrape, and it is Admin-only: an ordinary
User gets `403`, and a connector token gets `401` because the SCIM bearer chain
does not cover `/actuator`. Setting `MANAGEMENT_SERVER_PORT` moves all of actuator,
including `/actuator/health`, to that port and keeps it behind the same Admin
rule. The exposure and histogram settings live in `src/main/resources/telemetry.yaml`,
which the test configuration imports too. The tag policy (what a metric may be
labelled with) lives in `ScimRequestObservationConvention`. The alert rules are
`ops/prometheus/alerts.yaml`. Deployment steps, the internal-port setup and the
alert table are in `/infra/README.md` under "Operational telemetry".

A scheduled job reports its runs through `ScheduledJobMetrics`
(`app_job_runs_total{job,outcome}`, `app_job_last_success_seconds{job}`). The audit
retention job is `job="audit-retention"`, the inactivity job `job="inactivity"` (which
the alert rules select on), and the dormant-authority job
`job="dormant-authority-revocation"`. Each run is also its own trace, so every record
a run emits carries one `trace.id`, and is timed as `app_job_run_seconds{job}`.

### Inactivity governance

| Variable                                   | Default          | Meaning                                                                 |
| ------------------------------------------ | ---------------- | ----------------------------------------------------------------------- |
| `APP_DORMANCY_DEACTIVATION_WINDOW`         | `90d` (90 days)  | How long a User may go without logging in before it is deactivated     |
| `APP_DORMANCY_AUTHORITY_REVOCATION_WINDOW` | `180d` (180 days)| How long before its direct Admin group membership is removed            |

Two daily jobs apply them, both measured from the User's last successful login —
or from its creation, if it has never logged in. A login made while a password
change is required of the User does not count; the completed change does (see
[Required password change](#required-password-change)):

- **Inactivity deactivation** (04:00 daily) sets `active=false` on every dormant
  User, advances its SCIM version, ends its sessions and records an
  `INACTIVITY_DEACTIVATION` audit event.
- **Dormant-authority revocation** (04:30 daily) removes a dormant User's direct
  membership of the Admin group — nothing else: baseline `USER` access and
  ordinary Group memberships stay — advances the Admin group's and the User's
  versions, ends the User's sessions and records a
  `DORMANT_AUTHORITY_REVOCATION` audit event.

The Bootstrap Admin is never processed by either. A connector re-asserting
`active=true` does not reset the window; only an explicit reactivation (a write
that takes `active` from false to true, over SCIM or the Accounts page) does. A
connector that re-adds the Admin membership of a User that is still dormant
sees it removed again on the next run.

Each job is serialized on its own row in `scheduled_job_locks`
(`SELECT … FOR UPDATE SKIP LOCKED`, held for the run's transaction), so two runs
of the same job never overlap across instances and a run that finds its job
already running skips; the two jobs never wait for each other. A zero or negative
window fails startup. Neither default appears in `application.yaml` — both belong
to `DormancyPolicy`. Every run logs its outcome (`event.action:
identity.inactivity_deactivation` / `identity.dormant_authority_revocation`),
including a run that skipped or changed nobody.

### Required password change

A password change is required of a User — the change-required flag — by every
connector password write (SCIM create, PUT or PATCH carrying `password`), by an
Admin's **Force password change** and by an Admin **Unlock** of an account that
has a password. Only a successful self-service change
(`POST /api/auth/change-password`) clears it; a connector write never does. While
flagged, a session may call `GET /api/auth/me`, the change and
`DELETE /api/auth/logout`, and nothing else — `/api/admin/**` included, for a
flagged Admin. Any User may change its password at any time through the same
endpoint.

There is no deadline for the change. Instead, a flagged User's logins do not move
its dormancy basis, so a User that keeps logging in with an imposed credential
and never replaces it is deactivated by the inactivity job once the window has
passed since its creation, last real login or reactivation. The completed change
moves the basis.

### Audit trail database roles

The audit table is append-only, and that is a property of the database rather than
of the code writing to it. The `V3` migration creates two roles:

- **`backend_app`** — what every runtime connection assumes, through
  `spring.datasource.hikari.connection-init-sql`. It holds full DML on `accounts`
  and `user_counters`, and `INSERT`/`SELECT` only on `audit_events`. An `UPDATE` or
  `DELETE` of a recorded event from application code is refused by the server.
- **`backend_audit_retention`** — holds `UPDATE`/`DELETE` on `audit_events` and is
  reserved for the retention job, which assumes it with a transaction-scoped
  `SET LOCAL ROLE` and reverts on commit.

Beside the grants the table carries a `BEFORE UPDATE OR DELETE` trigger that
refuses the statement whatever role issues it, the owning role included, unless
that role is the retention role. Grants say nothing about a connection that arrives
as the owner — a console session, or a deployment that never set the runtime role —
so the trigger is what makes append-only survive a misconfiguration.

Because the runtime role cannot create tables, and does not exist until the
migration that creates it has run, **Flyway connects separately**:
`spring.flyway.user`/`password` default to the same `DATABASE_USERNAME` /
`DATABASE_PASSWORD` credentials, giving migrations a connection outside the pool
that keeps its privileges. Override them if your deployment migrates as a different
role than it serves as. A later migration that adds a table the application writes
must grant `backend_app` on it.

Sessions are stored through Spring Session's **indexed** Redis repository, which
keeps a per-principal index. That index is what lets disabling an account revoke
the sessions it holds, so the setting is a requirement rather than a preference:
with the default repository the application does not start. It is configured in
`src/main/resources/session.yaml`, imported by both the main and the test
`application.yaml` so the two cannot drift. Two consequences for
a deployment — the Redis instance is not interchangeable with a plain cache
(session keys and one index set per signed-in account), and startup does not try
to `CONFIG SET notify-keyspace-events`, because ElastiCache refuses `CONFIG`. Set
`notify-keyspace-events` in the cache parameter group if you want expiry events;
without them expired sessions are reaped by the repository's cleanup cron.

## Bundle a frontend

The SPA is never committed here. It is copied straight from the frontend's build
output into `target/classes/static` when the executable JAR is built:

```text
frontend source -> frontend/dist -> backend/target/classes/static -> JAR
```

That copy is the `with-frontend` Maven profile, and it is **off by default**, so
`./mvnw clean verify` and `./mvnw package` here are pure backend builds requiring no
Node and packaging no SPA. Turn it on for a release:

```bash
# from the repository root — builds the SPA first, then packages
make package

# or directly, against an already-built SPA
./mvnw -Pwith-frontend -Dfrontend.dist.dir=/abs/path/to/frontend/dist package
```

`frontend.dist.dir` defaults to `../frontend/dist` (the sibling app in this
monorepo) and must contain `index.html` at its root, for example:

```text
dist/
├── index.html
└── assets/
    ├── app.js
    └── app.css
```

If `index.html` is not there, the profile's `validate`-phase enforcer fails the
build — it will not package a missing or half-built frontend. `.br`/`.gz`
siblings emitted by the build are copied verbatim (resource filtering is off, so
binaries are not corrupted).

The backend serves static files and forwards client-side routes such as
`/account/settings` to `index.html`; `/api/**` and `/actuator/**` remain
backend-only paths.

## Build and test

```bash
./mvnw clean verify
```

Build the container after packaging the application:

```bash
./mvnw clean package
docker build -t backend:local .
```

## Quality gates

Two gates finish a change, next to the build and the test suite:

```bash
./mvnw -Dtest=ArchitectureTest test   # ArchUnit rules; also run by clean verify
pip install semgrep                   # once
./scripts/semgrep.sh                  # exits non-zero on any finding
```

`src/test/java/arch/ArchitectureTest.java` encodes the project's structure —
onion layering (adapters depend on application, application on domain, domain on
nothing), package placement for entities and configuration, naming conventions,
constructor injection, JPA mapping, and freedom from package cycles. A failure
names the rule and the offending class; satisfy the boundary rather than
relaxing the rule.

`./scripts/semgrep.sh` pins the rulesets the gate runs and passes `--error`, so
green means zero findings. The first run downloads registry rules and caches
them, so it needs network access. Extra flags reach Semgrep directly, for
example `./scripts/semgrep.sh --json --output semgrep.json`. `.semgrepignore`
lists the skipped paths; it replaces Semgrep's built-in default list, which would
otherwise skip `src/test/java`. That replacement is why the script passes
`--project-root .`: Semgrep resolves the project root from git, which is the
monorepo root, and an ignore file below that root is read but no longer cancels
the defaults — so without the flag the whole test tree is skipped silently. The
scan covers 94 files, 73 of them Java (39 main, 34 test).

Mutation testing is a third, non-gate check that PIT runs on demand:

```bash
./mvnw org.pitest:pitest-maven:mutationCoverage \
  -DtargetClasses="com.example.backend.auth.controller.AuthController*" \
  -DtargetTests="com.example.backend.auth.controller.AuthControllerTests"
```

Nothing to install — `pitest-maven` is declared in `pom.xml` and bound to no
lifecycle phase, so `./mvnw clean verify` never runs it. Reports land in
`target/pit-reports/`: `index.html` to browse, `mutations.xml` for the per-mutant
status.

`AGENTS.md` is the authority on when each of these runs, how to read a result,
and when a surviving mutant may be accepted.
