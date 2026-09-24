# front-end

React + TypeScript + Vite + Tailwind CSS v4, with the full tooling gate wired
up around a small session-authenticated app.

This is a **baseline repo**. The application content is deliberately thin — a
login page and one protected page with a counter on it, both talking to the
Spring Boot backend over session cookies. What is actually built out is the
toolchain: type checking, linting, unit tests, architecture tests, E2E, static
security analysis, dead-code/complexity analysis, and mutation testing.

## Setup

Node is pinned at the repo root (`/.nvmrc`, `/.tool-versions`) and declared in
`package.json`'s `engines`; `.npmrc` sets `engine-strict`, so a wrong Node fails
the install instead of warning. Activate the pin first:

```bash
nvm use                           # from the repo root; or: mise install
```

```bash
npm ci                            # exactly what package-lock.json records
npx playwright install chromium   # first time only, for npm run test:e2e
npm run dev                       # http://localhost:5173
```

Use `npm ci`, not `npm install` — the root `README.md` explains why, and
`npm install` belongs only to a deliberate dependency change whose lockfile
update you commit.

**No `.env` is required.** Nothing reads `import.meta.env` yet. When that
changes, the variable must be `VITE_`-prefixed (Vite only exposes that prefix
to the client) and documented here.

`npm run test:security` needs Semgrep on your `PATH`
(`pipx install semgrep`, or `pip install semgrep`).

## Scripts

`npm run` prints the full list. The ones whose name does not give them away:

| Command                 | Description                                             |
| ----------------------- | ------------------------------------------------------- |
| `npm run build`         | Type-check all three TS projects (`tsc -b`), then build |
| `npm run typecheck`     | `tsc -b` over app, node, and test projects              |
| `npm run test:arch`     | dependency-cruiser + the `test/arch` vitest suite       |
| `npm run test:security` | Semgrep, local ruleset in `semgrep/rules/`              |
| `npm run test:mutation` | Stryker mutation testing (whole repo — slow)            |
| `npm run analyze`       | Bundle visualizer → `dist/stats.html`                   |

Not npm scripts, but part of the gate:

```bash
npx fallow audit                                  # dead code / complexity / duplication
npx fallow dead-code --trace <file>:<export>      # a symbol's real consumers
```

Run a single test file or a single test by name:

```bash
npm test src/pages/showcase.test.tsx
npm test -- -t "counts each click"
```

## Project structure

```
src/
  main.tsx                mounts React, imports index.css
  App.tsx                 app root — BrowserRouter + AuthProvider + the routes
  index.css               Tailwind entry + the design tokens
  vite-env.d.ts
  auth/                   session state: api.ts, auth-context, protected-route
  pages/login.tsx         the public login page at /
  pages/showcase.tsx      the protected page and counter requests at /showcase
  components/ui/          shadcn primitives (placeholder — see below)
  lib/utils.ts            cn()
  lib/http.ts             typed API results — credentials + CSRF + status + decoding
test/
  setup.ts                jest-dom
  .dependency-cruiser.cjs
  arch/                   architecture rules the module graph can't express
  e2e/smoke.spec.ts       guest-facing Playwright smoke suite
  e2e/authentication.spec.ts  authenticated session + counter
  e2e/auth.setup.ts       signs in once, saves the storage state
semgrep/rules/          local Semgrep ruleset
docs/                   ARCHITECTURE.md, TESTING_GUIDE.md
graphify-out/           knowledge graph (tracked; refreshed with the code)
```

`@/` resolves to `src/`. There is no `src/types/`, `src/hooks/` or `src/utils/`
— types live beside what owns them and shared helpers live in `src/lib/`. See
`docs/ARCHITECTURE.md`.

## Component library

`src/components/ui/` holds hand-written stand-ins for `Button` and the `Card`
family, shaped like shadcn so that swapping them for the in-house package is a
delete plus an import rewrite. `components.json` is already configured for the
shadcn CLI, so `npx shadcn@latest add <component>` works if a primitive is
needed before that package lands. `AGENTS.md` has the swap procedure and the
constraints that keep the placeholders cheap to delete.

## Styling

Tailwind CSS v4, CSS-first — there is **no `tailwind.config.js`**.
`@tailwindcss/vite` is the build-side setup and `src/index.css` is the
configuration: a `:root` / `.dark` palette in `oklch()`, mapped onto Tailwind
colour utilities by an `@theme inline` block. That makes `src/index.css` the only
file allowed to hold a raw colour value; everything else names a token, and
`npm run test:arch` fails on a literal colour anywhere else.

Dark mode is a `dark` class on an ancestor, not a media query, so it can be
toggled in-app. Nothing toggles it yet.

## Testing

Three levels — **baseline** (typecheck, unit, arch), **full** (baseline + E2E +
Semgrep + fallow), **extensive** (full + mutation, CI only). Which one to run
where is in `AGENTS.md`; the details of each are in `docs/TESTING_GUIDE.md`.

Unit tests run on Vitest with happy-dom and Testing Library, colocated with the
code they cover. Coverage and mutation score are both at 100% on the code that
is mutated — a small surface, but the gates are real and the arch rules have
been verified to fail on planted violations.

E2E runs in three Playwright projects: `setup` signs in once and saves the
storage state, `guest` runs the smoke suite with an empty session (the bundle
boots, the Tailwind stylesheet is generated and applied, React state reaches the
DOM, no console errors), and `authenticated` reuses the saved session to drive
the protected page and the counter. The authenticated suite needs the backend
running — see the root `README.md` and `make integration-test`.

## Backend contract

The SPA is served by the Spring Boot backend and shares its session cookie.
`AGENTS.md`'s "Backend contract" section is authoritative. `apiFetch()` owns
CSRF recovery and returns typed semantic results instead of raw responses:
`unauthenticated` expires auth state and returns the user to login, while
`csrf-expired` preserves the session and lets the feature show retry copy.

In development, `vite.config.ts` proxies `/api` to the backend on `:8080`, so
`npm run dev` needs the backend up for anything past the login form.

## Technology stack

- **React 19** with TypeScript (strict, `noUnusedLocals` / `noUnusedParameters`)
- **react-router-dom 7** for routing (`/` login, `/showcase` protected)
- **Vite 7** for development and building, with Brotli/gzip precompression
- **Tailwind CSS v4** (CSS-first, no config file) with shadcn-shaped tokens
- **Vitest 4** + Testing Library + happy-dom for unit tests
- **Playwright** for E2E, with a session-reusing `authenticated` project
- **dependency-cruiser** for architecture rules
- **Semgrep** for static security analysis
- **fallow** for dead code, complexity and duplication
- **Stryker** for mutation testing
- **ESLint 9** (flat config) and **Prettier**

There is no global state library, no PWA support and no service worker.
