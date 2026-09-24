# AGENTS.md

React + Vite + Tailwind baseline. One vanilla page, and the full tooling gate
around it. There is no router, no data layer, no authentication and no service
worker yet — this file describes what is actually here, not what is planned.

## Commands

```bash
npm install                       # node_modules is often stale — vitest may be missing until you run this
npm run dev                       # Vite dev server on :5173
npm run build                     # tsc -b (app, node, and test projects) + vite build
npm run lint                      # ESLint
npm run format                    # Prettier, write
npm run typecheck                 # tsc -b over all three projects, no bundle
npm test                          # vitest, single pass
npm run test:watch                # vitest in watch mode
npm test src/pages/home.test.tsx  # one file
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

**No `.env` is required.** Nothing in the app reads `import.meta.env`, and the
E2E suite authenticates against nothing. Add `VITE_`-prefixed variables (Vite
only exposes that prefix to the client) and document them in README.md when
that changes.

First E2E run on a machine also needs `npx playwright install chromium`.

## Architecture

Four folders under `src/`, and the dependency direction runs one way through
them:

- **`src/components/ui/`** — the shadcn primitives. This is a **placeholder**
  for the in-house component library (see below). It may import `cn` from
  `src/lib/` and its own siblings, nothing else.
- **`src/lib/`** — framework-agnostic helpers any layer may call, and a leaf:
  it imports nothing from `src/`. Holds `cn()` today. Shared hooks belong here
  too — `components.json` points the shadcn CLI at `@/lib/hooks`.
- **`src/pages/`** — one component per page. Free to import from `ui/` and
  `lib/`.
- **`src/App.tsx` / `src/main.tsx`** — the composition root. `main.tsx` mounts
  and owns the one `src/index.css` import; `App.tsx` is where a router goes
  when one is needed.

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
the path alias, touching the token pipeline, or wiring in a router or a data
layer. It has the folder map, the reasoning behind each dependency rule, and
where a new concern belongs.

## Component library

`src/components/ui/` holds hand-written stand-ins for `Button` and the `Card`
family — enough for the vanilla page to render, and deliberately no more. They
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

## Agent skills

### Issue tracker

Issues are tracked in GitHub on this repository. See
`docs/agents/issue-tracker.md`.

### Domain docs

This repository uses a single-context domain-documentation layout. See
`docs/agents/domain.md`.

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
