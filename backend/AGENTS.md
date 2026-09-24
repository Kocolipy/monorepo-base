# AGENTS.md — backend

Spring Boot 4 service on Java 25, built with Maven through the checked-in
wrapper. Always `./mvnw`, never a bare `mvn`: the wrapper downloads and
checksum-verifies the one Maven release pinned in
`.mvn/wrapper/maven-wrapper.properties`, and the build's Enforcer rules reject a
wrong JDK (`[25,26)`) or an older Maven. A JDK 25 must be on `PATH` — the
wrapper only launches Maven.

Monorepo-wide rules — layout, the path discipline, the SPA contract this service
serves, line endings, ignore rules, the long-gate sentinel pattern, and the
shared agent docs and skills — live in the root `AGENTS.md`. This file covers
only what is specific to this app. Run every command below from `backend/`.

## Verification

Before completing Java, dependency, or application-configuration changes, run `./mvnw clean verify`. Add or update a focused regression test for every behavior change. If verification cannot run, report the exact unverified scope and reason.

### Baseline gates

Implementation work is complete only when both baseline gates are green, alongside the build and the tests:

- ArchUnit rules in `src/test/java/arch/ArchitectureTest.java`, which run inside `./mvnw clean verify`. Iterate with `./mvnw -Dtest=ArchitectureTest test`.
- `./scripts/semgrep.sh`, which scans **this app only** and exits non-zero on any finding. It runs two halves: the checked-in local rules in `semgrep/rules/` (offline and deterministic, each rule carrying the reason this service cares about it) and the `p/*` registry packs for generic Java and OWASP coverage. The pack *list* is fixed in the script, but the packs' *contents* resolve from the registry at run time and track upstream, so a pack gaining a rule can turn this gate red with no commit here. The script owns both config lists; `.semgrepignore` owns the skipped paths. The frontend scans itself separately via `npm run test:security` — nothing scans the monorepo as a whole.

Run both as part of finishing the work, not as a separate pre-commit step. A red gate is a defect in the change, not in the gate. Move the class, adjust the design, or fix the flagged code. Suppress a Semgrep finding with `// nosemgrep: RULE_ID` plus a reason only when it is a false positive. Edit a rule, the ruleset list, or `.semgrepignore` only when the user asks for the architecture or the scan policy itself to change, and say so explicitly.

Changes to Redis-backed session persistence require an integration-level check against Redis; the controller tests use servlet mocks and do not exercise Redis.

### Reading a gate's result

Both gates here can outlast a shell's foreground window, so run them through the
sentinel-and-log pattern documented in the root `AGENTS.md`:

```bash
./scripts/semgrep.sh > "${TMPDIR:-/tmp}/gate.log" 2>&1; echo "GATE_EXIT=$?" >> "${TMPDIR:-/tmp}/gate.log"
until grep -q GATE_EXIT "${TMPDIR:-/tmp}/gate.log" 2>/dev/null; do sleep 5; done
grep -E "inding|GATE_EXIT" "${TMPDIR:-/tmp}/gate.log"
```

The gate is green on `GATE_EXIT=0` beside a zero findings count, both quoted
from the log.

### Mutation testing

Mutation testing checks that a test is **load-bearing**: that it fails when the behavior it names breaks. It sits outside the baseline gates, and runs when you write a unit test or change an existing one, scoped to the tests you touched. PIT is configured in `pom.xml` and bound to no lifecycle phase, so `./mvnw clean verify` never runs it.

Target the touched test class and the production class it covers:

```bash
./mvnw org.pitest:pitest-maven:mutationCoverage \
  -DtargetClasses="com.example.backend.<package>.<ClassUnderTest>*" \
  -DtargetTests="com.example.backend.<package>.<TouchedTests>"
```

`target/pit-reports/mutations.xml` carries the per-mutant status. `SURVIVED` means a test ran the line without asserting on the behavior, so strengthen the assertion; `NO_COVERAGE` means no test reached the line, so add the missing case. Narrowing `targetClasses`, dropping mutators, or asserting on a duplicated implementation constant moves the score without making the test load-bearing.

The tests are done when every mutant is `KILLED`, or a **survivor** carries a justification earned by reading the mutated line and confirming the mutation leaves observable behavior unchanged. The surviving mutant plus its reason belong in the change summary.

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
- Runtime credentials and environment-specific settings remain external configuration.

`src/test/java/arch/ArchitectureTest.java` is the executable form of the structural boundaries: onion layering, package placement, naming, constructor injection, JPA mapping, and package-cycle freedom. Read it before reshaping packages or adding a layer.

For domain terminology and architectural decisions, follow `/docs/agents/domain.md`. Record durable architectural choices as ADRs rather than expanding this file.

## Security-sensitive changes

Treat authentication, authorization rules, logout, session invalidation, cookie attributes, and credential handling as security-sensitive. Cover changed behavior with tests and keep production secrets out of tracked files.

## API contract

Before completing changes to controller routes, request or response bodies, status codes, authentication requirements, or validation constraints, update `docs/openapi.yaml`. Verify every affected operation and schema against the implementation.
