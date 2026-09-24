# front-end

React + TypeScript + Vite + Tailwind CSS v4, with the full tooling gate wired
up around a single vanilla page.

This is a **baseline repo**. The application content is deliberately one page
with a counter on it; what is actually built out is the toolchain — type
checking, linting, unit tests, architecture tests, E2E, static security
analysis, dead-code/complexity analysis, and mutation testing.

## Setup

```bash
npm install
npx playwright install chromium   # first time only, for npm run test:e2e
npm run dev                       # http://localhost:5173
```

**No `.env` is required.** Nothing reads `import.meta.env` yet. When that
changes, the variable must be `VITE_`-prefixed (Vite only exposes that prefix
to the client) and documented here.

`npm run test:security` needs Semgrep on your `PATH`
(`pipx install semgrep`, or `pip install semgrep`).

## Scripts

| Command                  | Description                                             |
| ------------------------ | ------------------------------------------------------- |
| `npm run dev`            | Vite dev server on `:5173`                              |
| `npm run build`          | Type-check all three TS projects (`tsc -b`), then build |
| `npm run preview`        | Serve the production build locally                      |
| `npm run lint`           | ESLint                                                  |
| `npm run format`         | Prettier, write                                         |
| `npm run format:check`   | Prettier, check only                                    |
| `npm run typecheck`      | `tsc -b` over app, node, and test projects              |
| `npm test`               | Vitest, single pass                                     |
| `npm run test:watch`     | Vitest in watch mode                                    |
| `npm run test:ui`        | Vitest with the browser UI                              |
| `npm run test:coverage`  | Vitest with v8 coverage                                 |
| `npm run test:arch`      | dependency-cruiser + the `test/arch` vitest suite       |
| `npm run test:e2e`       | Playwright                                              |
| `npm run test:e2e:ui`    | Playwright interactive UI mode                          |
| `npm run test:e2e:debug` | Playwright debug mode                                   |
| `npm run test:security`  | Semgrep, local ruleset in `semgrep/rules/`              |
| `npm run test:mutation`  | Stryker mutation testing                                |
| `npm run analyze`        | Bundle visualizer → `dist/stats.html`                   |

Not npm scripts, but part of the gate:

```bash
npx fallow audit                                  # dead code / complexity / duplication
npx fallow dead-code --trace <file>:<export>      # a symbol's real consumers
```

Run a single test file or a single test by name:

```bash
npm test src/pages/home.test.tsx
npm test -- -t "counts each click"
```

## Project structure

```
src/
  main.tsx              mounts React, imports index.css
  App.tsx               app root — where a router goes
  index.css             Tailwind entry + the design tokens
  vite-env.d.ts
  pages/home.tsx        the one page
  components/ui/        shadcn primitives (placeholder — see below)
  lib/utils.ts          cn()
test/
  setup.ts              jest-dom
  .dependency-cruiser.cjs
  arch/                 architecture rules the module graph can't express
  e2e/smoke.spec.ts     Playwright smoke suite
semgrep/rules/          local Semgrep ruleset
docs/                   ARCHITECTURE.md, TESTING_GUIDE.md
graphify-out/           knowledge graph (tracked; refreshed with the code)
```

`@/` resolves to `src/`. There is no `src/types/`, `src/hooks/` or `src/utils/`
— types live beside what owns them and shared helpers live in `src/lib/`. See
`docs/ARCHITECTURE.md`.

## Component library

`src/components/ui/` holds hand-written stand-ins for `Button` and the `Card`
family — enough for the page to render, and no more. They follow the shadcn
shape (a `cva` variant table, `cn()` merging a `className` override) so that
swapping them for the in-house shadcn package is a delete plus an import
rewrite.

`components.json` is already configured for the shadcn CLI (`src/index.css` as
the token source, `@/lib/utils` as `cn`), so `npx shadcn@latest add <component>`
works if a primitive is needed before the package lands.

## Styling

Tailwind CSS v4, CSS-first — there is **no `tailwind.config.js`**.
`@tailwindcss/vite` is the build-side setup and `src/index.css` is the
configuration: a `:root` / `.dark` palette in `oklch()`, mapped onto Tailwind
colour utilities by an `@theme inline` block.

`src/index.css` is the only file in the repo allowed to hold a raw colour
value. Everything else names a token — `bg-card`, `text-muted-foreground`,
`var(--color-border)`. `npm run test:arch` fails on a literal hex, `rgb()`,
`hsl()` or `oklch()` anywhere else, because Tailwind would otherwise compile
`bg-[#0f172a]` without complaint.

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

E2E is a four-test Playwright smoke suite: the bundle boots, the Tailwind
stylesheet is generated and applied, React state reaches the DOM, and the page
loads with no console errors.

## Technology stack

- **React 19** with TypeScript (strict, `noUnusedLocals` / `noUnusedParameters`)
- **Vite 7** for development and building, with Brotli/gzip precompression
- **Tailwind CSS v4** (CSS-first, no config file) with shadcn-shaped tokens
- **Vitest 4** + Testing Library + happy-dom for unit tests
- **Playwright** for E2E
- **dependency-cruiser** for architecture rules
- **Semgrep** for static security analysis
- **fallow** for dead code, complexity and duplication
- **Stryker** for mutation testing
- **ESLint 9** (flat config) and **Prettier**

There is no PWA support and no service worker.

## Agent tooling

`AGENTS.md` is the entry point for coding agents; `CLAUDE.md` imports it and
adds one Claude-specific override. `graphify-out/` holds a knowledge graph of
the repo and is tracked in git — refresh it in the same commit as the change
that caused it:

```bash
$(cat graphify-out/.graphify_python) -m graphify update .
```
