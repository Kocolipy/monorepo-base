# Testing Guide

## Running the suites

| Command                 | What it runs                                          | Typical use                           |
| ----------------------- | ----------------------------------------------------- | ------------------------------------- |
| `npm test`              | vitest, single pass, everything but `test/e2e`        | after every change                    |
| `npm run test:watch`    | vitest in watch mode                                  | while writing a test                  |
| `npm test <path>`       | one file                                              | narrowing a failure                   |
| `npm test -- -t "name"` | one test by name                                      | narrowing further                     |
| `npm run typecheck`     | `tsc -b` over all three projects                      | after every change                    |
| `npm run test:arch`     | dependency-cruiser + `test/arch`                      | after every change                    |
| `npm run test:coverage` | vitest with v8 coverage                               | checking a gap, not a gate            |
| `npm run test:e2e`      | Playwright against `npm run dev`                      | once an implementation is complete    |
| `npm run test:security` | Semgrep, local ruleset                                | once an implementation is complete    |
| `npx fallow audit`      | dead code / complexity / duplication in the changeset | once an implementation is complete    |
| `npm run test:mutation` | Stryker over the whole repo                           | CI only                               |
| scoped Stryker          | Stryker over the source one test covers               | after writing or changing a unit test |

The three levels — baseline, full, extensive — and which one to run where are
in AGENTS.md.

## Unit tests

Vitest with happy-dom and Testing Library. Globals are on, so `describe` / `it`
/ `expect` need no import (they are imported explicitly anyway in the files
here, which costs nothing and survives a config change). `test/setup.ts` pulls
in `@testing-library/jest-dom`.

Tests are **colocated** with the code they cover — `showcase.test.tsx` beside
`showcase.tsx` — and type-checked through `tsconfig.test.json`, which is the only
project that includes them. `tsconfig.json` excludes `*.test.*` and
`*.testHelpers.*` so production builds never see them.

`*.testHelpers.ts(x)` is the name for shared test support. It is excluded from
the production project exactly as `*.test.*` is, and is deliberately **not**
`*.helpers.ts(x)` — that name is reserved for production helpers, which stay
inside the `cq-no-devdep-in-prod` dependency rule.

**A unit test — new or changed — is not finished until scoped Stryker confirms
it kills mutants in the code it covers.** A weakened assertion still passes, and
still counts as covered; Stryker is the only gate that notices. That is how the
trap below was found. The command is under Mutation testing.

### Assert exactly, not loosely

`toHaveTextContent` is a **substring** match. `toHaveTextContent("Clicked 1 time")`
passes on the buggy `"Clicked 1 times"` — so the assertion written to pin down
a pluralisation is exactly the one that does not. Anchor it:
`toHaveTextContent(/^Clicked 1 time$/)`. `src/pages/showcase.test.tsx` carries the
comment; Stryker is what found it (the `count === 1` mutant survived).

Playwright's `toHaveText` is exact by default, so E2E does not have this trap.

### Coverage excludes — why the list is explicit

`vitest.config.ts` lists each exclusion rather than globbing. v8 reports a file
with zero executable statements as 0%, so leaving those in drags the report
down with rows no test can ever cover: `*.css`, `*.d.ts`, the e2e and scripts
trees, and `src/main.tsx` (a `createRoot` call against the real document, with
no branch to assert — the smoke E2E covers that it mounts). Drop a file from
that list the moment it gains coverable code.

## Architecture tests

`npm run test:arch` is two halves, and a new rule belongs in whichever half can
actually see the thing it governs:

- **`test/.dependency-cruiser.cjs`** — anything expressible as _module A must
  not import module B_. It matches resolved module edges, and reads the `@/`
  alias out of `tsconfig.json`.
- **`test/arch/*.test.ts`** — anything the module graph cannot see: a string
  that never becomes an import, a config file's routing table, a value written
  in CSS. Each file names its rule in a comment at the top, in the same
  `snake_case` id AGENTS.md uses.

There are two of the latter today:

- `designTokens.test.ts` — no colour literal outside `src/index.css`.
- `e2eSpecRouting.test.ts` — every Playwright spec is matched by exactly one
  project's `testMatch`.

### Why the arch suite reads sources through `node:fs`

`test/arch/sources.ts` reads files with `node:fs`, not
`import.meta.glob(..., { query: "?raw" })`. The glob is the obvious choice and
it is wrong here: vitest replaces every `.css` import with an empty module
(`test.css` is off by default) **before** the `?raw` query is honoured, so a
stylesheet globbed that way arrives as an empty string and the rule reading it
passes vacuously. `designTokens.test.ts` was written that way first, and its
"keeps `src/index.css` as the palette" case is the guard that caught it — a
rule with an exemption should assert that the exempted file still contains what
the exemption is for.

`sources.ts` resolves the repo root from `process.cwd()`, not
`import.meta.url`: under vitest the latter is a served URL, not a `file:` one,
and `fileURLToPath` throws on it.

### Prove a new rule fails

An architecture test that cannot fail is worse than none — it reads as
coverage. Plant the violation, watch the rule catch it, then remove it:

```bash
echo 'export const BAD = "#ff0000";' > src/lib/violation.ts
cp test/e2e/smoke.spec.ts test/e2e/unrouted.spec.ts
npx vitest --run test/arch          # both rules should fail
rm src/lib/violation.ts test/e2e/unrouted.spec.ts
```

## Mutation testing

`npm run test:mutation` runs Stryker over the whole repo — CI only, far too
slow for an implementation loop. The in-loop run is scoped, and `--mutate` names
the **source** the test covers, never the test file:

```bash
npx stryker run --mutate 'src/pages/**/*.tsx,!src/pages/**/*.test.tsx'
```

A survivor means the test asserts too weakly to catch the bug it claims to
cover. Strengthen the assertion and re-run until the mutant dies — a survivor
you cannot kill is either a mutant with no observable effect, which belongs in
the `mutate` exclusions with its reason, or a gap in what the test set out to
prove.

The `mutation-testing` skill drives the run and triages the survivors. Two of
its defaults disagree with the rule above, so pass them explicitly:

```bash
/mutation-testing --target=src/pages/showcase.tsx --threshold=100
```

`--scope=changed` covers the whole changeset rather than the source one test
covers, and `--threshold=70` reports a pass with survivors still standing.

**The `--mutate` trap:** the flag _replaces_ the `mutate` array in
`stryker.config.json`, it does not narrow it. A glob without the `!` negations
mutates the test files too, which produces nonsense survivors. Always carry the
exclusions in the flag.

`stryker.config.json` excludes two things from mutation, and each has its
reason recorded in a `_comment_mutate` key beside the array:

- `src/main.tsx` — the composition root; every mutant is uncoverable or a
  restatement of what the smoke E2E proves.
- `src/components/ui/**` — vendored placeholder code, due to be deleted when
  the in-house shadcn package is published. Its mutants are edits to Tailwind
  class strings, and killing them means pinning assertions to markup that is
  about to be replaced.

`thresholds.break` is `null`, so a low score reports but does not fail. The
score is at 100% today; treat a drop as a question about the test, not a number
to chase.

## E2E

Playwright specs in `test/e2e/`, run against `npm run dev` on `:5173`
(`webServer` starts it, `reuseExistingServer` outside CI reuses one you already
have up). First run on a machine needs `npx playwright install chromium`.

The `setup` project signs the dedicated E2E user in once and saves Playwright
`storageState` under the ignored `test/e2e/.auth/` directory. The
`authenticated` project depends on that setup and reuses the session; `guest`
starts with explicitly empty browser storage.

### Routing a new spec

Each project in `playwright.config.ts` names its specs explicitly with a
`testMatch` rather than globbing `*.spec.ts`. **A
spec matched by no project's `testMatch` runs in no project and is silently
skipped** — the suite still reports green. `test/arch/e2eSpecRouting.test.ts`
reads the routing table out of the config and fails on an unmatched or
doubly-matched spec, so adding a spec means adding it to a `testMatch`.

Keep specs that only need an authenticated session in `authenticated`; do not
call `login()` in each test. Specs that exercise sign-in itself should get a
separate signed-out project with explicitly empty `storageState`. Per-test
`login()` under `fullyParallel` fires N concurrent logins that can throttle and
time out; shared storage state collapses that to one.

### Calling the API from a spec

`page.request` shares the browser context's cookie jar but adds **no headers of
its own**, so it does not satisfy the backend's CSRF contract
(`/frontend/AGENTS.md`, "Backend contract") — an unsafe request made that way
returns `403` and the
spec fails somewhere unrelated to what it was testing. Read the token out of the
context and echo it, as `resetCounterViaApi()` in `test/e2e/auth.helpers.ts`
does; add new API fixtures beside it rather than inlining a raw
`page.request.post`.

### Forcing a refused request

Two branches of `apiFetch` only exist because the backend distinguishes them, so
the spec has to make a real request come back refused rather than stub the
response:

- **`401`** — drop the session cookie from the browser context and put every
  other cookie back (`expireSession()` in `test/e2e/auth.helpers.ts`). The CSRF
  token lives outside the session (`CookieCsrfTokenRepository`), so keeping it is
  what makes this a `401` and not a `403`. The backend session stays valid, so a
  spec doing this cannot break one running beside it.
- **`403`, persistently** — rewrite the `X-XSRF-TOKEN` header with
  `page.route`. Deleting the cookie does not work: `apiFetch` answers a `403` by
  re-seeding the cookie and retrying once, so the retry would succeed. Rewriting
  on every attempt makes the backend reject both.

A test that forces a failure has to be shown to **fire**: neuter the mechanism
(rename the cookie, drop the header rewrite), confirm the test fails, then put it
back. Both of these were verified that way.

### Sharing backend state under `fullyParallel`

The counter is one value on the backend and `authentication.spec.ts` resets it,
so a spec running beside it must not assert a count. `session.spec.ts` only
makes requests the backend refuses, which leaves the count untouched.

### Unit-testing a module that calls the API

Everything under `src/` reaches the backend through `apiFetch` in
`src/lib/http.ts`. That module alone stubs `fetch` and proves credentials, CSRF
recovery, status classification, and decoding. Feature tests mock `apiFetch`
with an `ApiResult` (`ok`, `unauthenticated`, `csrf-expired`, or `failed`) and
assert only their own response to that meaning. This keeps raw `Response`
construction and cookie setup out of feature suites; `src/auth/api.test.ts` and
`src/pages/showcase.test.tsx` are the patterns.

### Flakiness — the rules that keep these tests green

Playwright auto-waits and auto-retries every web-first assertion (`toBeVisible`,
`toHaveText`, `toBeEnabled`, `toHaveAttribute`), so lean on those instead of
timing hacks:

- **No `page.waitForTimeout()`.** A fixed sleep is a race — too short on a
  loaded CI box, wasted time otherwise. Assert the end state; the assertion
  waits exactly as long as needed.
- **No `waitForLoadState("networkidle")`.** Wait for a concrete landmark
  element instead.
- **Assert the step you are on before acting on it.** StrictMode
  double-invokes mount effects in development, so an unguarded
  `useEffect(..., [])` fires twice and the loser's response lands whenever it
  lands. A `toHaveText("Step 3 of 5")` names the step the app actually reached;
  a `toBeVisible` on its content would just time out.
- **A full page reload wipes in-memory state, and `npm run dev` causes them.**
  Vite full-reloads every connected page when a watched `.html` under the root
  changes — see `server.watch.ignored` in `vite.config.ts`. Symptom: the failure
  screenshot shows the app back on its first screen, fully loaded, nothing in
  flight.
- **Prefer role and label selectors over CSS.** `getByRole("button", { name: "Reset" })`,
  `getByLabel("Display Name")`. `getByTestId` is fine for a value with no
  accessible name of its own, as `smoke.spec.ts` uses for the counter.
- **Restore anything a spec mutates.** Not applicable yet — no spec writes to
  shared state — but it is the rule the moment one does.

## Semgrep

`npm run test:security` scans with the local ruleset in `semgrep/rules/`. Adding
a rule: give it an `fe-` prefixed id (registry ids can never collide with it),
put the reason this project cares in the `message`, and **verify it fires**
before committing — write the violating snippet in a scratch file outside the
repo and scan it:

```bash
npx semgrep scan --config semgrep/rules --metrics=off --no-git-ignore /tmp/scratch
```

A pattern starting with `{` has to be quoted in the YAML, or the parser reads it
as a flow mapping and the whole config is rejected.

## Fallow

`npx fallow audit` scopes to the files changed since the merge-base with the
branch's upstream and fails only on findings the changeset **introduced** —
inherited ones still print, attributed `introduced: false`. `--base <ref>` pins
a different base. `npx fallow review --brief` renders the same analysis as an
orientation brief that always exits 0 — for reading, not gating.

Dead code sits at zero here, so any unused export, file, or dependency in an
audit is one you just added. That is why `buttonVariants` in
`src/components/ui/button.tsx` is module-local rather than exported the way the
shadcn generator emits it — export it the moment something outside that file
composes on it.

**A file nothing imports needs an `entry` line in `.fallowrc.jsonc`.** Fallow
walks JS/TS imports, so anything reached only through a config file — a service
worker source, a script a CI job calls — reads as an unused file. The `entry`
array is empty today and each future line carries its reason in a comment
beside it. `ignoreDependencies` lists `tailwindcss` for the same reason: it is
resolved by `@tailwindcss/vite` and by the `@import "tailwindcss"` in
`src/index.css`, never by a JS import statement.

**A suppression carries its reason above it** — a comment naming the consumer
the static graph cannot see, then `// fallow-ignore-next-line unused-export` on
the line before the export. `npx fallow suppressions` inventories every marker
and flags stale ones.

**`--production` is the lens for test-only exports.** A default run counts a
test file as a consumer, so an export nothing but its own test imports reads as
used. `npx fallow dead-code --production` drops test files from the consumer
set — read the result as a review question, not a delete list.

Boundary zones are deliberately unconfigured, so `fallow guard` has nothing to
say here: architecture rules are dependency-cruiser's job, under
`npm run test:arch`.

The tool's own documentation ships inside the package, at
`node_modules/fallow/skills/fallow/` — `SKILL.md` plus a `references/` folder
with the full CLI reference and a gotchas file. Read flags there rather than
guessing, and use `npx fallow explain <issue-type>` to understand a finding
without re-running the analysis.
