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

`src/auth/` is a _concern_, not a screen. It holds four files:

- `api.ts` — the three `/api/auth/*` calls, each mapping a status code to a
  domain outcome (`401` on `/me` is a guest, not an error).
- `auth-context.tsx` — the `AuthProvider`, which checks the session once on
  mount and owns the `checking | authenticated | guest` status.
- `auth-context-value.ts` — the context object and the `useAuth` hook, split out
  so a consumer importing the hook does not pull in the provider component.
- `protected-route.tsx` — renders a waiting state while `checking`, redirects to
  `/` with the attempted path in router state while `guest`, otherwise renders
  its children.

`pages/login.tsx` and `pages/showcase.tsx` are screens that _consume_ this; they
hold no session logic themselves. A second protected area adds a route, not a
second copy of the guard.

### Why every request goes through `lib/http.ts`

The backend enforces CSRF double-submit (`/frontend/AGENTS.md`, "Backend contract"), so every
unsafe request needs the `XSRF-TOKEN` cookie echoed in an `X-XSRF-TOKEN` header
or it comes back `403`. `apiFetch()` is the single place that knows this: it
reads the cookie **at call time** (login and logout both rotate the token, so a
value captured at start-up or held in state is stale), adds the header on unsafe
methods only, and on a `403` re-seeds the cookie with a safe `GET` and retries
exactly once — which also covers the cold start where `POST /api/auth/login` is
the tab's first HTTP call.

It lives in `lib/` because both `auth/api.ts` and `pages/showcase-api.ts` need
it and `lib/` is the one folder every layer may call. That places it under
`mb-lib-is-a-leaf`, so it must stay dependency-free — no auth types, no React.
A second API module goes beside its feature and calls `apiFetch`; a module that
calls `fetch` directly is a bug, because the CSRF and retry behaviour then
exists in two places that will drift.

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
the same line: it takes a path and a `RequestInit` and knows nothing about auth
or React, which is what lets both `auth/api.ts` and a page's `*-api.ts` sit on
top of it without a cycle.

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

`App.tsx` owns the whole route table — three routes, deliberately flat:

| Path        | Element                                         | Notes                                                       |
| ----------- | ----------------------------------------------- | ----------------------------------------------------------- |
| `/`         | `<Login />`                                     | public; redirects to `/showcase` when already authenticated |
| `/showcase` | `<ProtectedRoute><Showcase /></ProtectedRoute>` | guarded on `useAuth().status`                               |
| `*`         | `<Navigate replace to="/" />`                   | unknown paths fall back to login                            |

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
| Role / permission checks        | `src/auth/`, alongside `ProtectedRoute` — the backend is the authority               |
| Session-expiry warning          | `src/auth/`, reading the 15-minute window from `/frontend/AGENTS.md`                 |
| Nested layouts, lazy routes     | `src/App.tsx`, when there is a second protected area                                 |
| PWA / service worker            | `vite-plugin-pwa` in `vite.config.ts` + a `.fallowrc.jsonc` `entry` line             |

Already present, and where it lives: routing in `src/App.tsx`, authentication in
`src/auth/`, HTTP in `src/lib/http.ts` with a per-feature `*-api.ts` beside each
page.
