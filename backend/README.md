# Backend

A Java 25 / Spring Boot backend with HTTP sessions persisted in Redis and the
identity directory, audit trail and per-User counters persisted in PostgreSQL.
Identities are provisioned by an external directory over a SCIM 2.0 interface
(`/scim/v2`); the session-authenticated application API lives under `/api`.
Spring Session replaces the servlet container's in-memory session, so session
state can survive application restarts and be shared by multiple application
instances.

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

All `/api` endpoints other than login and the CSRF token require that cookie;
those under `/api/admin/**` also require `ADMIN`, and a `USER` session is
answered with `403`. Keep sending the cookie, and the token on unsafe requests.
For example, an Admin lists the directory's Users (never with a password hash)
and Unlocks one by its stable id:

```bash
curl -b cookies.txt http://localhost:8080/api/admin/accounts

curl -b cookies.txt -X POST -H "X-CSRF-TOKEN: $token" \
  http://localhost:8080/api/admin/accounts/<id>/unlock
```

The administration API only reads what the directory owns (`userName`,
`active`, Group membership); its writes are Unlock and the forced password
change. Deactivation is the directory's, over SCIM.

`docs/openapi.yaml` documents every operation, status and body — the
authentication, session, counter, self-service (`/api/self`), administration
(accounts, groups, connectors and their tokens, audit events) and SCIM
surfaces — and the baseline gate holds it against the running code (see
`docs/api-contract-check.md`).

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
different facts. The job is serialized on its own `audit-retention` row in
`scheduled_job_locks`, as the dormancy jobs are (see below): with several instances
on the same cron one run deletes, and the others end `job-end` with
`event.reason: lock-held` and delete nothing.

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
  `spring.datasource.hikari.connection-init-sql`. It holds DML on the tables the
  application writes — the `scim_*` directory tables, `user_counters`,
  `scheduled_job_locks` — insert-only access to `scim_tombstones`, and
  `INSERT`/`SELECT` only on `audit_events`. An `UPDATE` or `DELETE` of a recorded
  event from application code is refused by the server. The migrations' `GRANT`
  statements are the exact list.
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
keeps a per-principal index. That index is what lets the service end a User's
sessions — on deactivation, lockout, a forced password change, or a new login
under one session per User — so the setting is a requirement rather than a preference:
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
`/showcase` to `index.html`; `/api/**` and `/actuator/**` remain
backend-only paths.

## Build and test

```bash
./scripts/verify.sh
```

That is the baseline gate: the build, the tests, the ArchUnit rules
(`src/test/java/arch/ArchitectureTest.java`) and the Semgrep scan
(`./scripts/semgrep.sh`, which exits non-zero on any finding). It needs Docker
for the Testcontainers-backed tests and Semgrep on `PATH` (`pip install
semgrep`); the first scan downloads the registry packs, so it needs network
access. Mutation testing with PIT is a conditional gate on top of it.
`AGENTS.md` is the authority on what each gate covers, when PIT runs and with
which mutators, and when a surviving mutant may be accepted.

Build the container after packaging the application:

```bash
./mvnw clean package
docker build -t backend:local .
```
