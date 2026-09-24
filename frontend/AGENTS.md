# AGENTS.md — frontend

React + Vite + Tailwind baseline, and the full tooling gate around it. Two
pages behind a session-backed login: `react-router-dom` routes them, `src/auth/`
owns the session, and each page's own `*-api.ts` talks to the backend through
`src/lib/http.ts`. There is no global state library and no service worker — this
file describes what is actually here, not what is planned.

Monorepo-wide rules — layout, the path discipline, the SPA contract with the
backend, line endings, ignore rules, and the shared agent docs and skills — live
in the root `AGENTS.md`. This file covers only what is specific to this app.
Run every command below from `frontend/`.

**`npm ci`, never `npm install`** — `install` re-resolves semver ranges and
rewrites `package-lock.json`, so it is only for a deliberate dependency change.
Node is pinned in `/.nvmrc` and `/.tool-versions` and declared in
`package.json`'s `engines`; `.npmrc` sets `engine-strict`, so a wrong Node is a
hard install failure. Activate the pin from the repo root (`nvm use`, or
`mise install`) before the first install.

## Commands

```bash
npm ci                            # node_modules is often stale — vitest may be missing until you run this
npm run dev                       # Vite dev server on :5173
npm run build                     # tsc -b (app, node, and test projects) + vite build
npm run lint                      # ESLint
npm run format                    # Prettier, write
npm run typecheck                 # tsc -b over all three projects, no bundle
npm test                          # vitest, single pass
npm run test:watch                # vitest in watch mode
npm test src/pages/showcase.test.tsx  # one file
npm test -- -t "name of test"     # one test by name
npm run test:coverage             # v8 coverage
npm run test:arch                 # depcruise + arch vitest suite
npm run test:e2e                  # playwright E2E tests
npm run test:e2e:ui               # playwright interactive UI mode
npm run test:e2e:debug            # playwright debug mode
npm run test:security             # semgrep, local ruleset in semgrep/rules
npm run test:mutation             # full-repo Stryker (slow — CI only)
npx stryker run --mutate '<src-glob>,!<test-glob>'   # scoped Stryker (see docs/TESTING_GUIDE.md)
npx fallow audit                  # dead code / complexity / duplication in the changeset
npx fallow dead-code --trace <file>:<export>         # a symbol's real consumers, before deleting it
npm run analyze                   # bundle visualizer -> dist/stats.html
```

**No `.env` is required** — see the root `AGENTS.md` for the repo-wide
environment rules and the `VITE_` prefix requirement.

First E2E run on a machine also needs `npx playwright install chromium`.

## Architecture

Five folders under `src/`, and the dependency direction runs one way through
them:

- **`src/components/ui/`** — the shadcn primitives. This is a **placeholder**
  for the in-house component library (see below). It may import `cn` from
  `src/lib/` and its own siblings, nothing else.
- **`src/lib/`** — framework-agnostic helpers any layer may call, and a leaf:
  it imports nothing from `src/`. Holds `cn()` and `http.ts`, the single
  `fetch` wrapper every API module goes through (see "Backend contract").
  Shared hooks belong here too — `components.json` points the shadcn CLI at
  `@/lib/hooks`.
- **`src/auth/`** — the session. `api.ts` wraps the three `/api/auth/*`
  endpoints, `auth-context.tsx` holds the `checking | authenticated | guest`
  status, `auth-context-value.ts` is the context plus the `useAuth` hook, and
  `protected-route.tsx` gates a route on it.
- **`src/pages/`** — one component per page (`login.tsx`, `showcase.tsx`), each
  with its own API module beside it when it needs one (`showcase-api.ts`). Free
  to import from `auth/`, `ui/` and `lib/`.
- **`src/App.tsx` / `src/main.tsx`** — the composition root. `main.tsx` mounts
  and owns the one `src/index.css` import; `App.tsx` owns the `BrowserRouter`,
  wraps everything in `AuthProvider`, and routes `/` to login and `/showcase`
  through `ProtectedRoute`.

`@/` resolves to `src/`. That alias is declared in four places — `tsconfig.json`
`paths`, `vite.config.ts`, `vitest.config.ts`, and (via `tsConfig`)
`test/.dependency-cruiser.cjs` — and all four have to agree, or a change breaks
a different tool than the one being edited.

Four rules are review-blocking:

- **`src/components/ui/` imports only `src/lib/` and its siblings.** A
  primitive that reaches into a page cannot be swapped out for the published
  package. `npm run test:arch` enforces this.
- **`src/lib/` imports nothing from `src/`.** Same gate.
- **There is no `src/types/`, `src/hooks/` or `src/utils/`.** Types live in the
  folder that owns them; shared helpers and hooks live in `src/lib/`. Same gate.
- **Colors come from the tokens in `src/index.css`.** Tailwind utilities
  (`bg-card`, `text-muted-foreground`) in TSX, `var(--color-*)` in CSS. No
  literal hex, `rgb()`, `hsl()` or `oklch()` belongs anywhere else — a new shade
  goes in the `:root` / `.dark` pair and the `@theme inline` block.
  `test/arch/designTokens.test.ts` enforces this; Tailwind compiles
  `bg-[#0f172a]` without complaint, so nothing else would catch it.

**Read `docs/ARCHITECTURE.md`** before adding a folder under `src/`, changing
the path alias, or touching the token pipeline. It has the folder map, the
reasoning behind each dependency rule, and where a new concern belongs.

## Backend contract

`/backend/FRONTEND.md` is the authority on the runtime contract — read it before
changing anything that issues a request. The parts the SPA has to honour:

- **Every unsafe request carries `X-XSRF-TOKEN`.** The backend enforces CSRF
  double-submit, so a `POST` / `PUT` / `PATCH` / `DELETE` without the header
  comes back `403`. `src/lib/http.ts` is the only place that deals with this:
  `apiFetch()` reads the `XSRF-TOKEN` cookie **per request** (login and logout
  both rotate it, so a cached value goes stale), adds the header on unsafe
  methods only, and always sends `credentials: "include"`.
- **`403` is not `401`.** A `403` means the token was missing or stale;
  `apiFetch` re-seeds it with a safe `GET /api/auth/me` and retries once, then
  surfaces the failure. Only `401` means the session ended, and only `401` sends
  the user to the login screen — treating `403` as a logout looks like a random
  sign-out to the user.
- **Never call `fetch` directly from a component or an API module.** Go through
  `apiFetch`, or the CSRF handling exists in one more place that can drift.
  Playwright's `page.request` bypasses it too: use `resetCounterViaApi()` in
  `test/e2e/auth.helpers.ts` as the pattern for an API call from a spec.
- **Sessions expire after 15 minutes** of inactivity, the single default in
  every environment. Nothing in the SPA hardcodes that today; if a countdown or
  expiry warning is added, it reads 15 minutes from this contract.
- **The CSP forbids inline script, `eval`, and every third-party origin** for
  scripts, styles, fonts, images and `fetch`. Self-host instead of adding a CDN,
  and avoid Vite plugins that inject inline `<script>`. Inline _styles_ are
  allowed.

## Component library

`src/components/ui/` holds hand-written stand-ins for `Button` and the `Card`
family — enough for the two pages to render, and deliberately no more. They
follow the shadcn shape (a `cva` variant table, `cn()` merging a `className`
override) so that swapping them out is a delete plus an import rewrite.

**When the in-house shadcn package is published:** add it to `dependencies`,
delete `src/components/ui/`, and repoint `@/components/ui` at the package (or
change the imports). `components.json` is already configured for the shadcn CLI
— `src/index.css` as the token source, `@/lib/utils` as `cn`, `@/components/ui`
as the component target — so `npx shadcn@latest add <component>` also works if
a primitive is needed before the package lands.

Two things the placeholders deliberately do not do: no `asChild` / Radix `Slot`
(the real library owns that), and no icon dependency. Keep them cheap to delete.

## Testing

**Three levels, and where you are decides which one you run.**

| Level         | Command                                                                      | When                                                                                                                                       |
| ------------- | ---------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| **baseline**  | `npm run typecheck`, `npm test`, and `npm run test:arch`                     | Before touching code, to establish a green baseline, then after each change — it proves the change broke nothing that was already passing. |
| **full**      | baseline + `npm run test:e2e` + `npm run test:security` + `npx fallow audit` | Once an implementation is complete, to confirm the whole thing works.                                                                      |
| **extensive** | full + `npm run test:mutation`                                               | CI only — a whole-repo Stryker run is far too slow to sit in an implementation loop.                                                       |

Scoped Stryker sits outside these levels: it runs per unit test, not per
changeset.

The suite is small today, so the baseline runs in seconds; run it in the
foreground. The ten-minute `timeout: 600000` habit is worth keeping anyway, so
a growing suite never gets cut off mid-run.

**Read `docs/TESTING_GUIDE.md`** before writing or changing a unit test, adding
an architecture rule, suppressing a fallow finding, adding a file nothing
imports, or writing or debugging an E2E spec. It has the per-command run table,
the vitest setup and colocation rules, the completion criterion a unit test has
to meet, why the arch suite reads sources through `node:fs` rather than
`import.meta.glob`, the Stryker `--mutate` trap, how a new Playwright spec gets
routed, and the flakiness rules.

## Semgrep

`npm run test:security` runs the local ruleset in `semgrep/rules/` — six rules
covering the DOM injection sinks (`dangerouslySetInnerHTML`, `innerHTML`,
`document.write`), `eval` / `new Function`, `target="_blank"` without
`noopener`, and credential-shaped names assigned string literals.

Local rather than a registry pack so the run stays offline and deterministic
and each rule carries the reason this project cares about it. A wider sweep is
still worth doing occasionally:

```bash
npx semgrep scan --config p/typescript --config p/react
```

A new rule goes in `semgrep/rules/` with its reason in a comment and an `fe-`
prefixed id, so it can never collide with a registry id. Verify it fires: write
the violating snippet in a scratch file, scan it, and confirm the finding
before committing the rule.

## Fallow

**Trace before you delete.** `noUnusedLocals` catches unused _locals_ only — an
unused export, file, or dependency is invisible to `tsc`, and fallow's import
graph is what sees them. It cuts the other way too: that graph is syntactic, so
a symbol reached through a config file or a dynamic `import()` also reads as
unused. `npx fallow dead-code --trace <file>:<export>` (or
`--trace-dependency <name>`) prints the real consumer list in under a second —
delete on that evidence, never on a summary line.

`npx fallow audit` is the `full`-level gate because it fails only on findings
**this changeset introduced**, where a bare `npx fallow` also reports the
duplication and complexity the repo already carries. Dead code is the
exception: it sits at zero, so an unused export, file, or dependency in an
audit is one you just added. This is why `buttonVariants` in
`src/components/ui/button.tsx` is not exported.

## TypeScript

Strict, with `noUnusedLocals` / `noUnusedParameters` — an unused import is a
baseline failure, not a lint warning. Three TypeScript projects, and
`npm run typecheck` and `npm run build` each list all three explicitly, so a
project dropped from either invocation stops being checked at all:

- `tsconfig.json` — `src`, ES2020, DOM, excluding `*.test.*` and
  `*.testHelpers.*`. Sets `"types": []`, so no `@types/*` package is injected
  globally and browser source cannot accidentally reach `process` or `Buffer`.
  `src/vite-env.d.ts` pulls in `vite/client` by reference, which that array
  does not gate.
- `tsconfig.node.json` — `vite.config.ts`, `vitest.config.ts`,
  `playwright.config.ts`. ES2022, `"types": ["node"]` for their `process.env`
  reads.
- `tsconfig.test.json` — the colocated tests and `test/**`, excluding
  `test/e2e/**` (Playwright specs are checked by the Playwright run, not here).

Unused _exports_ are past what `tsc` can see — that is fallow's half, above.
