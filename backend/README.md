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

Three consecutive refused logins lock an account for five minutes. While the
lockout holds the correct password is refused too, and every refusal — unknown
username, wrong password, locked account — answers with the same bare `401`, so
the response cannot be used to find out which accounts exist. An accepted login
resets the count. `APP_LOCKOUT_MAX_ATTEMPTS` and `APP_LOCKOUT_DURATION` (a
duration such as `5m` or `30s`) configure the policy.

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
endpoints under `/api/admin/**` — and the reserved `/api/accounts/**` namespace —
require `ADMIN`. Continue sending the cookie when using the session API:

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
curl -b cookies.txt http://localhost:8080/api/admin/users
```

```json
[
  {
    "username": "admin",
    "email": "admin@example.com",
    "role": "ADMIN",
    "enabled": true,
    "locked": false,
    "lockedUntil": null,
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
  http://localhost:8080/api/admin/users/user/disable

curl -b cookies.txt -X POST -H "X-XSRF-TOKEN: $token" \
  http://localhost:8080/api/admin/users/user/enable

curl -b cookies.txt -X POST -H "X-XSRF-TOKEN: $token" \
  http://localhost:8080/api/admin/users/user/unlock
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
otherwise skip `src/test/java`.

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
