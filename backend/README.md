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

Set `APP_USERNAME`, `APP_PASSWORD`, `APP_SECONDARY_USERNAME`, and
`APP_SECONDARY_PASSWORD` to your desired credentials. The development defaults
are `admin` / `P@ssw0rd` and `admin2` / `P@ssw0rd`; they must not be used
in production.

Log in and keep the returned `JSESSIONID` in a cookie jar:

```bash
curl -c cookies.txt \
  -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"P@ssw0rd"}'

curl -b cookies.txt http://localhost:8080/api/auth/me
```

All API endpoints other than login and the health check require that cookie.
Continue sending it when using the session API:

```bash
curl -b cookies.txt http://localhost:8080/api/session

curl -b cookies.txt \
  -X PUT http://localhost:8080/api/session \
  -H 'Content-Type: application/json' \
  -d '{"displayName":"Ada"}'

curl -b cookies.txt http://localhost:8080/api/session

curl -b cookies.txt -X DELETE http://localhost:8080/api/auth/logout
```

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

## Baseline gates

Two gates finish a change, next to the build and the test suite. Run both
whenever you complete a piece of implementation work; both must be green before
the work counts as done.

### Architecture tests

`src/test/java/arch/ArchitectureTest.java` holds the ArchUnit rules that encode
the project's structure: onion layering (adapters depend on application,
application on domain, domain on nothing), package placement for entities and
configuration, naming conventions, constructor injection, JPA mapping, and
freedom from package cycles. They run with the rest of the suite under
`./mvnw clean verify`. Run them alone while iterating:

```bash
./mvnw -Dtest=ArchitectureTest test
```

A failure names the rule and the offending class. Move the class or adjust the
design to satisfy the boundary; change the rule only when the architecture
itself is meant to change.

### Semgrep scan

```bash
pip install semgrep          # once
./scripts/semgrep.sh
```

The script pins the rulesets the gate runs (`p/java`, `p/security-audit`,
`p/owasp-top-ten`) and passes `--error`, so it exits non-zero on any finding:
green means zero findings. Semgrep downloads registry rules on the first run and
caches them, so that run needs network access. Extra flags reach Semgrep
directly, for example `./scripts/semgrep.sh --json --output semgrep.json`.

`.semgrepignore` lists the paths the scan skips: generated output plus whatever
`.gitignore` already excludes. It replaces Semgrep's built-in default ignore
list, which would otherwise skip `src/test/java`.

To fix a finding, change the flagged code. When a finding is a false positive,
annotate the line with `// nosemgrep: RULE_ID` and say why in the same comment.

## Mutation testing

Mutation testing asks whether a test is load-bearing: PIT changes the production
bytecode one edit at a time and reruns the tests, so a mutant that survives marks
behavior the suite describes but never checks. Coverage says a line ran;
mutation testing says a line is defended.

This is not a third baseline gate. It runs when you write a unit test or change
an existing one, scoped to the tests you touched, which keeps a run to seconds.
Whole-codebase runs belong in a nightly schedule rather than a local loop.

The `pitest-maven` plugin is already declared in `pom.xml`, with
`pitest-junit5-plugin` for the JUnit 5 engine. Nothing to install; the first run
downloads both from Maven Central. The plugin is bound to no lifecycle phase, so
`./mvnw clean verify` never runs it.

Point it at the test class you touched and the production class it covers:

```bash
./mvnw org.pitest:pitest-maven:mutationCoverage \
  -DtargetClasses="com.example.backend.auth.AuthController*" \
  -DtargetTests="com.example.backend.auth.AuthControllerTests"
```

Reports land in `target/pit-reports/`: `index.html` for browsing, annotated with
each mutation against its source line, and `mutations.xml` for the per-mutant
status. Two statuses matter. `SURVIVED` means a test executed the line without
asserting on the behavior, so the assertion is too weak. `NO_COVERAGE` means no
test reached the line at all.

Aim for every mutant killed. Kill a survivor by strengthening the assertion or
adding the missing case; narrowing the scope or dropping mutators improves the
number without improving the test. A mutant that genuinely cannot change
observable behavior is an equivalent mutant, and it is worth accepting only
after reading the mutated line to confirm it.

One caveat: a 100% score under the default mutators is provisional. PIT's
`DEFAULTS` set leaves some lines unmutated — a call whose return value is
discarded, such as `request.changeSessionId()`, produces no mutant — so a line
can be both unmutated and untested while the score reads green. Line coverage
below 100% next to a 100% score points at those lines. Confirm with a wider set:

```bash
./mvnw org.pitest:pitest-maven:mutationCoverage \
  -DtargetClasses="com.example.backend.auth.AuthController*" \
  -DtargetTests="com.example.backend.auth.AuthControllerTests" \
  -Dmutators=STRONGER,NON_VOID_METHOD_CALLS,CONSTRUCTOR_CALLS,EXPERIMENTAL_NAKED_RECEIVER,EXPERIMENTAL_MEMBER_VARIABLE
```
