# AGENTS.md — backend

Spring Boot 4 service on Java 25, built with Maven through the checked-in
wrapper. Always `./mvnw`: the release it pins lives in
`.mvn/wrapper/maven-wrapper.properties`, and the build's Enforcer rules reject a
wrong JDK (`[25,26)`) or an older Maven. A JDK 25 must be on `PATH` — the
wrapper only launches Maven.

Monorepo-wide rules — layout, the path discipline, the SPA build contract this
service serves, line endings, ignore rules, the long-gate sentinel pattern, and
the shared agent docs — live in the root `AGENTS.md`. This file covers only what
is specific to this app. Run every command below from `backend/`.

## Verification

### Baseline gate

`./scripts/verify.sh` is the baseline gate: one command, and a backend change is
complete only when it exits zero. It runs the build, the tests, the ArchUnit
rules and the Semgrep scan. **The script is the source of truth for that list** —
the root `Makefile`'s `verify-backend` target invokes it rather than re-listing
the steps, so the two cannot drift. Add or update a focused regression test for
every behavior change.

Two halves are worth knowing separately, because each has its own iteration loop:

- ArchUnit rules in `src/test/java/arch/ArchitectureTest.java`, which run inside `./mvnw clean verify`. Iterate with `./mvnw -Dtest=ArchitectureTest test`.
- `./scripts/semgrep.sh`, which scans **this app only** and exits non-zero on any finding. It runs two halves: the checked-in local rules in `semgrep/rules/` (offline and deterministic, each rule carrying the reason this service cares about it) and the `p/*` registry packs for generic Java and OWASP coverage. The pack _list_ is fixed in the script, but the packs' _contents_ resolve from the registry at run time and track upstream, so a pack gaining a rule can turn this gate red with no commit here. The script owns both config lists; `.semgrepignore` owns the skipped paths, and the script's `--project-root .` is what makes that file authoritative — Semgrep resolves the project root from git (the monorepo root), and without the pin the built-in default skip list stays in force and silently drops every file under `src/test/java`. The frontend scans itself separately via `npm run test:security` — nothing scans the monorepo as a whole.

Run the gate as part of finishing the work, not as a separate pre-commit step. A red gate is a defect in the change, not in the gate. Move the class, adjust the design, or fix the flagged code. Suppress a Semgrep finding with `// nosemgrep: RULE_ID` plus a reason only when it is a false positive. Edit a rule, the ruleset list, or `.semgrepignore` only when the user asks for the architecture or the scan policy itself to change, and say so explicitly.

Changes to Redis-backed session persistence require an integration-level check against Redis; the controller tests use servlet mocks and do not exercise Redis.

### Reading a gate's result

The baseline gate can outlast a shell's foreground window, so run it through the
sentinel-and-log pattern the root `AGENTS.md` documents:

```bash
./scripts/verify.sh > "${TMPDIR:-/tmp}/gate.log" 2>&1; echo "GATE_EXIT=$?" >> "${TMPDIR:-/tmp}/gate.log"
until grep -q GATE_EXIT "${TMPDIR:-/tmp}/gate.log" 2>/dev/null; do sleep 5; done
grep -E "inding|GATE_EXIT" "${TMPDIR:-/tmp}/gate.log"
```

### Conditional gate: mutation testing

Mutation testing checks that a test is **load-bearing**: that it fails when the behavior it names breaks. It sits outside the baseline gate, and its **trigger** is writing a unit test or changing an existing one — scoped to the tests you touched, never the whole module. PIT is configured in `pom.xml` and bound to no lifecycle phase, so the baseline gate never runs it.

Target the touched test class and the production class it covers:

```bash
./mvnw org.pitest:pitest-maven:mutationCoverage \
  -DtargetClasses="com.example.backend.<package>.<ClassUnderTest>*" \
  -DtargetTests="com.example.backend.<package>.<TouchedTests>"
```

`target/pit-reports/mutations.xml` carries the per-mutant status. `SURVIVED` means a test ran the line without asserting on the behavior, so strengthen the assertion; `NO_COVERAGE` means no test reached the line, so add the missing case. Narrowing `targetClasses`, dropping mutators, or asserting on a duplicated implementation constant moves the score without making the test load-bearing.

The tests are done when every mutant is KILLED, or a survivor carries a justification that names the test asserting the mutated behavior and says why that test still passes with the mutant alive — the mutation is masked by something the code does anyway, as when a domain record coerces the caller's null back to List.of(). A justification with no such test to name has found an unasserted line, not an equivalent mutant: three succeeded(...) calls on AccountAdministrationService read as equivalent because a void call has no return value to trace, while in fact no test asserted the log at all. The surviving mutant plus the test that pins it belong in the change summary.

Treat a clean score under the default mutators as provisional. PIT's `DEFAULTS` set leaves some lines unmutated, so a line can be both unmutated and untested while the score reads 100% — a call whose return value is discarded, such as `request.changeSessionId()`, yielded no mutant while session-fixation rotation went unexercised. Line coverage below 100% beside a 100% score points at those lines. Confirm with the expanded set before reporting a score as clean:

```bash
-Dmutators=STRONGER,NON_VOID_METHOD_CALLS,CONSTRUCTOR_CALLS,EXPERIMENTAL_NAKED_RECEIVER,EXPERIMENTAL_MEMBER_VARIABLE
```

## Architecture constraints

Preserve these boundaries:

- Spring Security owns authentication.
- Authentication state is stored in the HTTP session.
- Spring Session persists sessions in Redis.
- Login and health endpoints are public; application endpoints require authentication.
- Authorization by role lives in the filter chain, not in annotations on handlers: `/api/admin/**` requires `ADMIN` there, so every access rule is readable in one place. A handler under that path therefore carries no role check of its own. Account administration has one namespace; a rule with no handler behind it is not added, because its only consumer would be a test supplying its own endpoint.
- A credential never reaches a web adapter. The application layer hands out projections (`AccountSummary`) that have no field a password hash could be written into, so exposure is prevented structurally rather than by review.
- The login path and the administration path do not share an application service. `AccountService` serves authentication (seeding, `UserDetails`); `AccountAdministrationService` serves an administrator and is the only one that mutates an account. Do not add account writes to the former.
- Authenticating submitted credentials goes through `LoginService`, which records the attempt against the account as part of doing it. A web adapter never calls `AuthenticationManager` itself: an entry point that did would authenticate with no lockout, and nothing would fail. See `/docs/adr/0001-count-login-attempts-on-the-login-path.md`.
- Enabling and unlocking are separate capabilities and neither performs the other — see `/CONTEXT.md`. An account may be disabled, locked, both, or neither, and restoring one says nothing about the other.
- An administrative write uses a narrow port operation (`updateEnabled`, `updateLockout`), never `save`. The login path writes the same row whole on every rejected login, so a full-row administrative write would race it and could revert a decision made in between.
- Disabling an account revokes the sessions it holds, through the `AccountSessions` port — see `/CONTEXT.md`. The port names one capability ("this account's sessions end") and the `..infrastructure.session..` adapter owns everything Spring Session about it; do not reach for a session repository from an application service. `spring.session.data.redis.repository-type: indexed` is load-bearing: the default repository cannot be searched by principal, has no bean for that adapter, and startup fails rather than accepting a disable it cannot enforce. It is set in `src/main/resources/session.yaml`, which the main and test `application.yaml` both import so they cannot disagree — change it there, not in either of them.
- Runtime credentials and environment-specific settings remain external configuration.

`src/test/java/arch/ArchitectureTest.java` is the executable form of the structural boundaries: onion layering, package placement, naming, constructor injection, JPA mapping, and package-cycle freedom. The onion model treats `..config..` as an inbound adapter: configuration may wire an application seam, but application and domain code never depend on configuration. Read it before reshaping packages or adding a layer.

For domain terminology and architectural decisions, follow `/docs/agents/domain.md`. Record durable architectural choices as ADRs rather than expanding this file.

## Security-sensitive changes

Treat authentication, authorization rules, logout, session invalidation, cookie attributes, and credential handling as security-sensitive. Cover changed behavior with tests and keep production secrets out of tracked files.

The SPA depends on several of these at runtime: CSRF double-submit, the `401` versus `403` split, the session window, and the CSP. `frontend/AGENTS.md`'s "Backend contract" section states what it relies on — read it before changing any of the four, and update it in the same change.

## API contract

Before completing changes to controller routes, request or response bodies, status codes, authentication requirements, or validation constraints, update `docs/openapi.yaml`. Verify every affected operation and schema against the implementation.
