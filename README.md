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

## Working on the frontend

```bash
cd frontend
npm ci
npm run dev          # Vite dev server
npm test             # vitest
npm run test:e2e     # Playwright
npm run lint         # eslint
npm run typecheck    # tsc -b
```

Full script list is in `frontend/package.json`.

## Working on the backend

```bash
cd backend
mvn spring-boot:run   # run the service
mvn clean verify      # build + tests
docker compose up     # Postgres + Redis dependencies
```

Copy `backend/.env.example` to `.env` before running. Requires Java 25 and a
locally installed Maven — there is no `mvnw` wrapper checked in.

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
