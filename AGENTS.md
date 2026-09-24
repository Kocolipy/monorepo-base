# AGENTS.md

Monorepo-wide rules. Two self-contained apps at the root, plus the shared agent
material both of them use.

App-specific rules live in the app's own `AGENTS.md` and take precedence inside
that directory:

- `frontend/AGENTS.md` — Vite + React + TypeScript
- `backend/AGENTS.md` — Spring Boot 4 / Java 25 / Maven

## Layout

```
frontend/         Vite + React + TypeScript SPA
backend/          Spring Boot 4 service (Java 25, Maven)
infra/            AWS CloudFormation template + deploy/cleanup scripts
packages/         shared code, when any appears
docs/agents/      agent documentation shared by both apps
.agents/skills/   agent skills shared by both apps
```

There is no top-level package manager, build file, or task runner. Nothing
builds "the monorepo" — each app is built and tested on its own terms.

`infra/` is deployment material, not an app: it is never built or tested by
either app's gates, and the Makefile's `infra-up` / `infra-down` / `infra-logs`
targets are **local Docker dependencies** (Postgres + Redis), unrelated to this
directory. See `/infra/README.md`.

## Paths

**Run every command from the app directory that owns it.** `npm` only works in
`frontend/`, `mvn` only in `backend/`, and both apps' configs, scripts, and
tooling resolve paths relative to their own root.

Two rules follow from that:

- **Never introduce a parent-relative reference** (`../`) from inside an app to
  the repo root or to the other app. An app that reaches outside itself stops
  being independently buildable, and the reference silently changes meaning if
  the directory moves. The one sanctioned cross-app path is the SPA contract
  below, which is expressed inside `backend/` as a path it owns.
- **Shared material is referenced from the repo root, not copied.** Paths to
  `docs/agents/` and `.agents/skills/` in an app's `AGENTS.md` are written
  root-relative (`/docs/agents/domain.md`) so they cannot be mistaken for a
  file inside the app.

## Build and validation

Each app owns its own gates. The entry points:

| App        | Build / verify                  | From        |
| ---------- | ------------------------------- | ----------- |
| `frontend` | `npm run typecheck && npm test` | `frontend/` |
| `frontend` | `npm run build`                 | `frontend/` |
| `backend`  | `./mvnw clean verify`           | `backend/`  |

`backend/` carries a **Maven wrapper** — always invoke `./mvnw`, never a bare
`mvn`: the wrapper downloads and checksum-verifies the one Maven release pinned
in `backend/.mvn/wrapper/maven-wrapper.properties`, and the build's Enforcer
rules reject a wrong JDK (`[25,26)`) or an older Maven. A JDK 25 must still be on
`PATH`; the wrapper only launches Maven.

`frontend/` needs `npm ci` before a first run (`node_modules` is often stale) —
**`ci`, not `install`**: `install` re-resolves semver ranges and rewrites the
lockfile, so it is only for a deliberate dependency change. Node is pinned in
`/.nvmrc` and `/.tool-versions`; run `nvm use` (or `mise install`) from the repo
root first, since `frontend/.npmrc` sets `engine-strict` and a wrong Node is a
hard install failure rather than a warning. The full pin table is in
`/README.md`.

**A change is complete only when the owning app's gates are green.** Both apps
define stricter per-change gates (architecture suites, security scans, mutation
scoping) in their own `AGENTS.md`; read that file before declaring work done.
If a gate cannot run, report the exact unverified scope and the reason rather
than reporting success.

A change touching only one app runs only that app's gates. A change touching
the SPA contract runs both.

### Reading a long gate's result

A full scan or build can outlast a shell's foreground window, so run either
app's gates with the result written where the shell cannot lose it: redirect to
a log and append a **sentinel** carrying the exit status.

```bash
./mvnw clean verify > "${TMPDIR:-/tmp}/gate.log" 2>&1; echo "GATE_EXIT=$?" >> "${TMPDIR:-/tmp}/gate.log"
```

Wait for the sentinel and read the log in one call, rather than polling for
output:

```bash
until grep -q GATE_EXIT "${TMPDIR:-/tmp}/gate.log" 2>/dev/null; do sleep 5; done
grep -E "inding|ERROR|GATE_EXIT" "${TMPDIR:-/tmp}/gate.log"
```

The gate is green on `GATE_EXIT=0` beside a zero findings count, both quoted
from the log; report those two lines as the evidence rather than the absence of
an error. When a call returns no output the run's state is unknown, so read the
log again — a relaunch only starts a second run competing for the same log.

## Frontend/backend integration

One build contract, and no committed build output anywhere in it:

```
frontend source -> frontend/dist -> backend/target/classes/static -> executable JAR
```

`frontend/dist/` is generated and ignored. `backend/target/` is generated and
ignored. **Nothing generated is ever copied back into a source directory**, and
no compiled SPA is tracked — the source is the only source of truth.

The copy is done by the `with-frontend` Maven profile in `backend/pom.xml`,
which is **off by default**: `./mvnw clean verify` in `backend/` is a pure backend
build that needs no Node and packages no SPA. The release path is `make package`
from the repo root, which builds the SPA and then invokes the profile with an
explicit `-Dfrontend.dist.dir`. The profile's `validate`-phase enforcer fails the
build when `index.html` is absent from that directory, so a missing or half-built
frontend is an error rather than a silently stale SPA.

`frontend.dist.dir` defaults to `${project.basedir}/../frontend/dist`, the one
sanctioned parent-relative path in the tree: it is inert unless the profile is
active, so `backend/` stays independently buildable, and the root build step
overrides it explicitly rather than relying on it.

## Line endings

`.gitattributes` at the root is repo-wide policy and the only copy — the apps do
not carry their own. CRLF in the working tree breaks shell scripts
(`/bin/bash^M: bad interpreter`), makes `.env` files unsourceable, and corrupts
Maven tooling, so the working tree is pinned to LF regardless of the machine's
git config. Add new binary extensions there, not in an app.

## Ignore rules

`.gitignore` at the root is the only ignore file; both apps' rules were merged
into it. Mind git's anchoring rule when editing: a pattern containing a slash
anywhere but the end is relative to the file's own directory, so an app-specific
rule needs an explicit `/frontend/` or `/backend/` prefix, and a rule that must
apply inside both apps needs a `**/` prefix. A bare name like `node_modules`
already matches at any depth.

## Environment

- `frontend/` needs **no** `.env`. Nothing reads `import.meta.env`. New
  variables must carry the `VITE_` prefix and be documented in
  `frontend/README.md`.
- `backend/` requires `.env`; copy `backend/.env.example` first. It also needs
  Postgres and Redis — `docker compose up` in `backend/`.

`backend/src/main/resources/application.yaml` carries working default
credentials as env-var fallbacks. **This remote is public.** Any deployment
that does not set the env vars ships published credentials, so never rely on
the fallbacks and never add new ones.

## Agent

`.agents/` is gitignored, so a file added there needs `git add -f` to be
tracked.

### Agent documentation

- **`/docs/agents/issue-tracker.md`** — issue creation, lookup, triage,
  comments, labels, closure. Read before acting on an issue or PR. One GitHub
  repository backs both apps.
- **`/docs/agents/domain.md`** — domain-documentation layout and how to consume
  it before exploring or changing domain behavior, terminology, or
  architecture.

### Skills

`/.agents/skills/` holds skills that apply to both apps:

- **`mutation-testing`** — tool-agnostic; `references/tool-adapters.md` covers
  Stryker and PIT. Each app's `AGENTS.md` documents its own invocation.

### graphify

This project has a knowledge graph at `graphify-out/` with god nodes, community
structure, and cross-file relationships.

When the user types `/graphify`, use the installed graphify skill or
instructions before doing anything else.

Rules:

- Invoke every Graphify command through the recorded interpreter:
  `$(cat graphify-out/.graphify_python) -m graphify <command>`. If
  `.graphify_python` is missing, follow the Graphify skill's interpreter guard
  first.
- For codebase questions, first run the `query "<question>"` command when
  `graphify-out/graph.json` exists. Use `path "<A>" "<B>"` for relationships and
  `explain "<concept>"` for focused concepts. These return a scoped subgraph,
  usually much smaller than `GRAPH_REPORT.md` or raw grep output.
- Dirty `graphify-out/` files are expected after hooks or incremental updates;
  dirty graph files are not a reason to skip graphify. Only skip graphify if the
  task is about stale or incorrect graph output, or the user explicitly says not
  to use it.
- Read `graphify-out/GRAPH_REPORT.md` only for broad architecture review or when
  query/path/explain do not surface enough context.
- Before committing modified code, run the `update .` command through the
  recorded interpreter to keep the graph current (AST-only, no API cost).
  `graphify-out/` is tracked, so the refresh belongs in the same commit as the
  change that caused it — refreshing afterwards leaves the graph stranded
  outside the PR.
