# monorepo-base

Monorepo holding the frontend SPA and the backend service.

## Layout

```
frontend/   Vite + React + TypeScript SPA
backend/    Spring Boot 4 service (Java 25, Maven)
```

Each app is self-contained: its own `README.md`, `AGENTS.md`, `.gitignore`,
dependency manifest, and test/quality tooling. Start there for anything
app-specific — this file only covers the repo as a whole.

Shared code, when it appears, goes in a top-level `packages/` directory.

## Toolchain pins

Every toolchain version is pinned in the repo, so a developer's machine and CI
resolve the same one:

| Tool  | Pinned in                                                      | Enforced by                                                                   |
| ----- | -------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| Node  | `/.nvmrc`, `/.tool-versions`, `frontend/package.json` `engines` | `frontend/.npmrc` (`engine-strict`), `scripts/lib.sh`                         |
| npm   | `frontend/package.json` `packageManager`                        | Corepack, plus `engines.npm`                                                  |
| Maven | `backend/.mvn/wrapper/maven-wrapper.properties`                 | `backend/mvnw` (checksum-verified download), Enforcer `requireMavenVersion`    |
| JDK   | `/.tool-versions`                                              | Enforcer `requireJavaVersion` in `backend/pom.xml` (`[25,26)`)                 |

Activate the Node and JDK pins with whichever manager you use — `nvm use` (reads
`/.nvmrc`), or `asdf install` / `mise install` (both read `/.tool-versions`) from
the repo root. Maven needs nothing installed: build through `./mvnw`.

Container images are pinned to an exact patch **and** a digest, in
`backend/compose.yaml` and `backend/Dockerfile`. The tag documents what the
image is; the digest is what actually gets pulled. To move one, bump the tag and
re-resolve the digest together:

```bash
docker buildx imagetools inspect postgres:18.6-alpine --format '{{println .Manifest.Digest}}'
```

A bump to Node, Maven, or the JDK has to land in every row of the table above in
the same commit — a pin that disagrees with its neighbour is worse than no pin,
because the failure surfaces as a build error somewhere unrelated.

## Working on the frontend

```bash
cd frontend
npm ci               # always ci, never install — see below
npm run dev          # Vite dev server
npm test             # vitest
npm run test:e2e     # Playwright
npm run lint         # eslint
npm run typecheck    # tsc -b
```

Full script list is in `frontend/package.json`.

**Use `npm ci`, not `npm install`.** `ci` installs exactly what
`package-lock.json` records and fails if the lockfile and `package.json` have
drifted apart; `install` resolves semver ranges afresh and rewrites the
lockfile, which is how two machines end up on different dependency trees. Run
`npm install` only when you are deliberately adding or upgrading a dependency,
and commit the resulting lockfile change.

## Working on the backend

```bash
cd backend
./mvnw spring-boot:run   # run the service
./mvnw clean verify      # build + tests
docker compose up        # Postgres + Redis dependencies
```

Copy `backend/.env.example` to `.env` before running. Requires a JDK 25 on
`PATH`; Maven itself does not need to be installed — `./mvnw` downloads and
checksum-verifies the pinned release. The build's Enforcer rules fail fast on a
wrong JDK or an older Maven.

## Frontend/backend integration

```
frontend source -> frontend/dist -> backend/target/classes/static -> executable JAR
```

No build output is committed: `frontend/dist/` and `backend/target/` are both
generated and ignored, and nothing generated is copied back into a source
directory.

```bash
make package     # build the SPA, then package it into the Spring Boot JAR
```

That is the release path. It runs the backend's `with-frontend` Maven profile,
which is off by default — `./mvnw clean verify` in `backend/` stays a pure
backend build with no Node and no SPA in the JAR. The profile fails the build
when `frontend/dist/index.html` is missing, rather than packaging a stale SPA.

`backend/FRONTEND.md` documents the runtime contract (CSRF, CSP, sessions) —
read it before changing either side's request handling.
