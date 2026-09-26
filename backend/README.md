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

Authentication is database-backed. On startup the service idempotently seeds
one `USER` account and one `ADMIN` account when their usernames are absent. The
development defaults are `user` / `P@ssw0rd` and `admin` / `P@ssw0rd`;
`APP_USERNAME` / `APP_PASSWORD` configure the `USER` seed and
`APP_SECONDARY_USERNAME` / `APP_SECONDARY_PASSWORD` configure the `ADMIN` seed.
These published defaults must not be used in production.

Five consecutive refused logins lock an account, and the lock is **permanent**:
it has no duration, nothing lifts it as time passes, and an `ADMIN` performing
Unlock is the only thing that ends it. Imposing it also revokes that account's
live sessions, so a locked account stops acting immediately rather than when the
session it already held expires. While the lockout holds the correct password is
refused too, and every refusal — unknown username, a credentialless account,
wrong password, locked account — answers with the same bare `401` after an
equivalent Argon2id verification, so the response cannot be used to find out
which accounts exist, which have a password set, or which are locked. An accepted
login resets the count. `APP_LOCKOUT_MAX_ATTEMPTS` configures the threshold, with
no enforced floor on the value; there is no duration setting to configure.

The seeded `ADMIN` (`APP_SECONDARY_USERNAME`) is the deployment's **Bootstrap
Admin** and is the one account exempt from lockout: its failed attempts are
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

Log in and keep the returned `JSESSIONID` in a cookie jar:

```bash
curl -c cookies.txt \
  -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"P@ssw0rd"}'

curl -b cookies.txt http://localhost:8080/api/auth/me
```

All API endpoints other than login and the health check require that cookie.
Counter and session endpoints accept either authenticated role; administration
endpoints under `/api/admin/**` require `ADMIN`. Continue sending the cookie when
using the session API:

```bash
curl -b cookies.txt http://localhost:8080/api/session

curl -b cookies.txt \
  -X PUT http://localhost:8080/api/session \
  -H 'Content-Type: application/json' \
  -d '{"displayName":"Ada"}'

curl -b cookies.txt http://localhost:8080/api/session

curl -b cookies.txt -X DELETE http://localhost:8080/api/auth/logout
```

Review who has access. This needs an `ADMIN` session; a `USER` session is
answered with `403`, and the listing never contains a password hash:

```bash
curl -b cookies.txt http://localhost:8080/api/admin/accounts
```

```json
[
  {
    "username": "admin",
    "role": "ADMIN",
    "enabled": true,
    "locked": false,
    "createdAt": "2026-01-02T03:04:05.123456Z"
  }
]
```

Control an account. Enabling and unlocking are **separate capabilities**:
disabling leaves the failure run standing, enabling leaves a lockout standing,
and unlocking says nothing about `enabled`. Each is a POST, so each needs the
CSRF header:

```bash
token=$(awk '/XSRF-TOKEN/{print $7}' cookies.txt)

curl -b cookies.txt -X POST -H "X-XSRF-TOKEN: $token" \
  http://localhost:8080/api/admin/accounts/user/disable

curl -b cookies.txt -X POST -H "X-XSRF-TOKEN: $token" \
  http://localhost:8080/api/admin/accounts/user/enable

curl -b cookies.txt -X POST -H "X-XSRF-TOKEN: $token" \
  http://localhost:8080/api/admin/accounts/user/unlock
```

Each answers `200` with the account as it now stands, `404` for an unknown
username, and `409` when the change would leave nobody able to reverse it —
disabling your own account, or the last enabled administrator. Disabling does
**not** end a session the account already holds; it only stops new logins.

Increment or reset the count belonging to the authenticated user:

```bash
curl -b cookies.txt http://localhost:8080/api/count
curl -b cookies.txt -X POST http://localhost:8080/api/count/increment
curl -b cookies.txt -X POST http://localhost:8080/api/count/reset
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

### SCIM release gate

| Variable           | Default | Meaning                                    |
| ------------------ | ------- | ------------------------------------------ |
| `APP_SCIM_ENABLED` | `false` | Whether this deployment serves `/scim/v2`  |

**Off by default.** While it is off the whole `/scim/v2` namespace answers `404` —
public discovery included, and ahead of authentication, so a valid connector token
gets the same answer as none at all. A deployment serves the SCIM interface because
someone turned it on, never because they did not know it was there.

The reason it exists: SCIM Users and Groups are one release capability. A directory
that can create Users but has no Groups cannot express authority, so a connector
provisioning against it would build a directory that means something different from
the one it will provision against later. The gate lets the two halves be built and
merged in order without the half-built surface ever being reachable.

The value is not written in `application.yaml`: the default belongs to
`ScimSecurityConfig`, so an unset variable reaches the gate as closed rather than as
whatever a replaced config file happens to say. Set `APP_SCIM_ENABLED=true` to open
it — in a test, `@TestPropertySource(properties = "app.scim.enabled=true")`.

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
