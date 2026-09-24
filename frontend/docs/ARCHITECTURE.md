# Architecture

What is here today, and where a new concern belongs. This baseline has one
page; the rules below exist so that the tenth page does not need a rewrite.

## The four layers

```
src/
  main.tsx            composition root — mounts React, imports index.css
  App.tsx             app root — where a router goes when one is needed
  index.css           Tailwind entry + the design tokens
  vite-env.d.ts       /// <reference types="vite/client" />
  pages/              one component per page
  components/ui/      shadcn primitives (placeholder — see "Component library")
  lib/                framework-agnostic helpers; a leaf
```

Imports run one way: `main` → `App` → `pages` → `components/ui` → `lib`. A
layer may skip a step (a page may call `cn` directly) but may never point back
up. `test/.dependency-cruiser.cjs` encodes exactly that, and
`npm run test:arch` runs it.

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
anything it drags in is effectively in every bundle chunk.

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

## What is deliberately absent

Nothing below exists yet. Each entry names where it goes, so the first person
to need it does not have to invent a convention.

| Concern              | Where it goes                                                                         |
| -------------------- | ------------------------------------------------------------------------------------- |
| Routing              | `src/App.tsx`, routing to `src/pages/`                                                |
| HTTP / data fetching | a new `src/lib/api/` (leaf-safe) or a per-feature `api.ts` beside its feature         |
| Global state         | beside the feature that owns it; hoist to `src/lib/` only when a second one needs it  |
| Environment config   | `VITE_`-prefixed variables, read through `import.meta.env`, documented in README.md   |
| Auth                 | its own folder under `src/`, plus new Playwright projects (see docs/TESTING_GUIDE.md) |
| PWA / service worker | `vite-plugin-pwa` in `vite.config.ts` + a `.fallowrc.jsonc` `entry` line              |
