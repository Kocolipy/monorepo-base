# Architecture

What is here today, and where a new concern belongs. This baseline has two
pages; the rules below exist so that the tenth page does not need a rewrite.

## The layers

```
src/
  main.tsx            composition root — mounts React, imports index.css
  App.tsx             app root — BrowserRouter, AuthProvider, the route table
  index.css           Tailwind entry + the design tokens
  vite-env.d.ts       /// <reference types="vite/client" />
  auth/               session state and the route guard
  pages/              one component per page, plus its own api.ts when it needs one
  components/ui/      shadcn primitives (placeholder — see "Component library")
  lib/                framework-agnostic helpers; a leaf
```

Imports run one way: `main` → `App` → `pages` → `auth` → `components/ui` →
`lib`. A layer may skip a step (a page may call `cn` directly, `auth/` reaches
straight into `lib/http`) but may never point back up.
`test/.dependency-cruiser.cjs` encodes exactly that, and `npm run test:arch`
runs it.

### Why `auth/` is its own folder and not a page

`src/auth/` is a _concern_, not a screen. It holds six files:

- `api.ts` — the three `/api/auth/*` calls, each mapping a status code to a
  domain outcome (`401` on `/me` is a guest, not an error).
- `auth-context.tsx` — the `AuthProvider`, which checks the session once on
  mount and owns the session status plus the expiry provenance.
- `auth-context-value.ts` — the context object and the `useAuth` hook, split out
  so a consumer importing the hook does not pull in the provider component.
  `useAuth` deliberately exposes no way to _end_ a session.
- `session-route.ts` — `resolveSessionRoute`, the whole routing contract as one
  pure transition table: who waits, who renders, who is redirected where, and
  what the redirect carries.
- `route-guards.tsx` — `ProtectedRoute` and `GuestRoute`, two thin adapters over
  that table sharing one pending view.
- `use-session-request.ts` — the seam features request through. It handles an
  `unauthenticated` result itself and returns a `SessionResult`, which has no
  `unauthenticated` member, so no page can forget to relay a session ending.

`pages/login.tsx`, `pages/showcase.tsx`, and `pages/accounts.tsx` are screens
that _consume_ this; they hold no session or role logic themselves, and none
decides where a visitor goes next. A new protected area adds a route declaration,
not a second copy of the guard.

`pages/accounts.tsx` is the widest of the three: it reads the account listing from
`GET /api/admin/accounts` and posts the three administration actions, replacing the
one affected row from each response rather than reloading the listing — the
response _is_ that account's new state, so a refetch would only add a request that
could disagree with it. Every request goes through `useSessionRequest`, so a
`401` ends the session in one place and a `403` stays a CSRF problem. What each
status means to an administrator (`409` a refused change, `404` an account that
has since gone) is mapped in that page, because only the page knows what was being
attempted.

`mb-transport-is-behind-the-session-seam` in `test/.dependency-cruiser.cjs`
enforces the direction: only `src/auth/` may import `lib/http.ts`, so a page
cannot opt out of the seam by calling `apiFetch` itself.

### Why every request goes through `lib/http.ts`

The backend enforces CSRF double-submit (`/frontend/AGENTS.md`, "Backend contract"), so every
unsafe request needs the `XSRF-TOKEN` cookie echoed in an `X-XSRF-TOKEN` header
or it comes back `403`. `apiFetch()` is the single place that knows this: it
reads the cookie **at call time**, adds the header on unsafe methods only, and
on a `403` re-seeds the cookie with a safe `GET` and retries exactly once.

Its interface returns an `ApiResult`: `ok` carries data from an explicit decoder,
`unauthenticated` means the session ended, `csrf-expired` keeps a persistent
`403` distinct, and `failed` covers every other transport, HTTP, or decoding
failure. Features retain their own human-facing copy while sharing status
meaning. A no-content request omits the decoder, so its `ok` data is typed as
`void` rather than pretending every success is JSON.

It lives in `lib/` because every layer may call it. That places it under
`mb-lib-is-a-leaf`, so it stays dependency-free — no auth types, no React. A
page may call it directly when a separate request module would only forward an
endpoint and result; a feature-specific module remains worthwhile when it owns
actual feature mapping.

### Why `components/ui/` is fenced off

`src/components/ui/` is not "the shared components folder" — it is a
placeholder for a package. The in-house shadcn library is what will live at
that import path, and the day it is published this folder gets deleted. That
only stays cheap while its contents depend on nothing but `cn()`, so the
`mb-ui-primitives-are-leaves` rule holds the boundary rather than trusting
everyone to remember.

Shared components that are _not_ library primitives — a page header, a layout
shell — do not belong here. Put them in `src/components/` (one level up), which
no rule constrains, or beside the page that owns them.

### Why `lib/` is a leaf

`mb-lib-is-a-leaf` keeps `src/lib/` importing nothing from `src/`. It is the
one folder every other layer may call, so an edge pointing out of it is a cycle
waiting to happen — and `cn()` in particular is imported by every primitive, so
anything it drags in is effectively in every bundle chunk. `http.ts` is held to
the same line: it takes a path, request options, and an optional decoder, and
knows nothing about auth or React. Both `auth/api.ts` and a page can consume its
semantic results without creating a cycle.

### Why there is no `src/types/`, `src/hooks/` or `src/utils/`

Those folders collect by _kind_ rather than by _concern_, so a feature ends up
smeared across four directories and nothing can be moved or deleted as a unit.
Types live in the file or folder that owns them. Shared helpers and shared
hooks go in `src/lib/` — that is also where `components.json` points the shadcn
CLI (`"hooks": "@/lib/hooks"`), so a generated hook lands in the right place
without anyone intervening. `mb-no-top-level-catchall-dirs` enforces the ban.

## The `@/` alias

`@/x` resolves to `src/x`, declared in four places that must agree:

| File                           | Mechanism                              |
| ------------------------------ | -------------------------------------- |
| `tsconfig.json`                | `compilerOptions.paths`                |
| `vite.config.ts`               | `resolve.alias`                        |
| `vitest.config.ts`             | `resolve.alias`                        |
| `test/.dependency-cruiser.cjs` | `options.tsConfig` (reads the `paths`) |

Vitest does not inherit `vite.config.ts` here — there are two separate config
files, because the unit suite deliberately does not load `@tailwindcss/vite`
(see below). So an alias change made in one has to be made in the other, and
missing dependency-cruiser is the quiet failure: aliased imports read as
unresolvable and every rule silently stops matching on them.

## Styling and the token pipeline

Tailwind v4, CSS-first. There is no `tailwind.config.js` and there should not
be one — `@tailwindcss/vite` in `vite.config.ts` is the whole build-side setup,
and `src/index.css` is the whole configuration:

1. `@import "tailwindcss"` pulls in the framework.
2. `@custom-variant dark (&:is(.dark *))` makes dark mode a class on an
   ancestor rather than a media query, so it can be toggled in-app later
   without touching a component.
3. `:root` and `.dark` hold the palette as raw `oklch()` values. **This is the
   only place a raw colour value belongs in the repo.**
4. `@theme inline` maps each palette variable onto a Tailwind colour utility,
   so `--color-card` becomes `bg-card`, `text-card`, `border-card`, and so on.
5. `@layer base` applies the default border colour and the body's
   background/foreground.

Components name the token, never the value. `test/arch/designTokens.test.ts`
fails the build on a hex, `rgb()`, `hsl()` or `oklch()` literal in any `src`
file other than `index.css` — Tailwind compiles arbitrary values like
`bg-[#0f172a]` happily, so without that test nothing would catch it.

Adding a shade means adding it in three spots: the `:root` block, the `.dark`
block, and `@theme inline`. That triple is deliberate — a token with no dark
value is a bug that only shows up for users in dark mode.

Vitest does **not** load `@tailwindcss/vite`: unit tests stub CSS imports, so
generating the utility stylesheet for every test file would be pure cost. The
one thing that needs real CSS is the E2E suite, which runs against the dev
server and therefore against `vite.config.ts` — and `smoke.spec.ts` asserts the
body's computed `background-color` resolved to something, which is what catches
a broken CSS pipeline.

## Build

`vite build` emits a single `vendor` chunk for everything under `node_modules`
and the app in `index`. `vite-plugin-compression2` writes `.gz` and `.br`
alongside each asset. There is **no PWA plugin and no service worker** — this
app is not installable and does not cache offline. Adding one means bringing
back `vite-plugin-pwa`, an `entry` line in `.fallowrc.jsonc` for the worker
source (nothing imports it, so fallow reads it as an unused file), and a
`registerType` decision.

## Dev-server reloads

`server.watch.ignored` lists `playwright-report/`, `coverage/` and `.claude/`.
Vite full-reloads _every connected page_ when a watched `.html` under the
project root changes, and it watches the whole root — so a finishing Playwright
run writing `playwright-report/index.html`, or `npm run test:coverage` writing
`coverage/index.html`, would reload whatever pages are open, including pages
another test is driving. Vite already ignores `**/test-results/**`; a new
directory that a test run writes into belongs on this list.

## Routing

`App.tsx` owns the whole route table — four routes, deliberately flat:

| Path        | Element                                                              | Notes                                                   |
| ----------- | -------------------------------------------------------------------- | ------------------------------------------------------- |
| `/`         | `<Login />`                                                          | Visitor login; authenticated accounts go to `/showcase` |
| `/showcase` | `<ProtectedRoute><Showcase /></ProtectedRoute>`                      | available to `USER` and `ADMIN`                         |
| `/accounts` | `<ProtectedRoute requiredRole="ADMIN"><Accounts /></ProtectedRoute>` | account administration, restricted to `ADMIN`           |
| `*`         | `<Navigate replace to="/" />`                                        | unknown paths fall back to login                        |

`resolveSessionRoute` is the pure transition table behind both guard adapters.
It sends a Visitor to login with a return destination, renders authenticated
routes for either role, and redirects a role mismatch to `/showcase`. Spring
Security remains authoritative for server operations: `/api/admin/**` requires
`ADMIN` even if client-side routing is bypassed, so the guard decides what is
_rendered_ and never what is _permitted_.

`BrowserRouter` means real paths, not hashes, so the backend has to serve
`index.html` for any unmatched path — that fallback is the backend's side of the
SPA contract, and a deep link like `/showcase` 404s without it.

A new protected area is a new `<Route>` wrapped in the existing
`ProtectedRoute`. Nested layouts and lazy route chunks are both unused; add them
in `App.tsx` when there is a second protected area, not before.

## What is deliberately absent

Nothing below exists yet. Each entry names where it goes, so the first person
to need it does not have to invent a convention.

| Concern                         | Where it goes                                                                        |
| ------------------------------- | ------------------------------------------------------------------------------------ |
| Global state                    | beside the feature that owns it; hoist to `src/lib/` only when a second one needs it |
| Server-state caching            | a query library wrapping `apiFetch`, wired in `App.tsx` beside `AuthProvider`        |
| Shared non-primitive components | `src/components/` (one level up from `ui/`), or beside the page that owns them       |
| Environment config              | `VITE_`-prefixed variables, read through `import.meta.env`, documented in README.md  |
| Session-expiry warning          | `src/auth/`, reading the 15-minute window from `/frontend/AGENTS.md`                 |
| Nested layouts, lazy routes     | `src/App.tsx`, when there is a second protected area                                 |
| PWA / service worker            | `vite-plugin-pwa` in `vite.config.ts` + a `.fallowrc.jsonc` `entry` line             |

Already present, and where it lives: routing in `src/App.tsx`, authentication in
`src/auth/`, and typed HTTP results in `src/lib/http.ts`. A feature module maps
results only when it adds feature behavior; a page consumes pass-through
results directly.
