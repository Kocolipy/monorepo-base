import { describe, expect, it } from "vitest";

import { readSources } from "./sources";

// Architecture rule `colors_come_from_tokens` (AGENTS.md, Architecture).
// src/index.css holds the light and dark palettes and maps them onto Tailwind
// colour utilities; every other file names a token (`bg-card`,
// `text-muted-foreground`, `var(--color-border)`) instead of a value. A
// literal that slips into a component is invisible to the dark-mode switch and
// to any later rebrand, and nothing else in the pipeline flags it — Tailwind
// compiles arbitrary values like `bg-[#0f172a]` without complaint.
const TOKEN_SOURCE = "src/index.css";

// Hex (3, 4, 6 or 8 digits) and the four colour functions. The leading `\b`
// keeps identifiers such as `--color-oklch-ish` out.
const COLOR_LITERAL = /#[0-9a-fA-F]{3,8}\b|\b(?:rgba?|hsla?|oklch|oklab)\s*\(/;

// Blanks out comments while preserving line count and column positions, so
// reported line numbers still point at the real source.
const blankComments = (source: string): string =>
  source
    .replace(/\/\*[\s\S]*?\*\//g, (match) => match.replace(/[^\n]/g, " "))
    .replace(/(^|[^:])\/\/.*$/gm, (match, prefix: string) => prefix.padEnd(match.length));

const sources = readSources("src", [".ts", ".tsx", ".css"]);
const files = sources
  .filter(({ path }) => path !== TOKEN_SOURCE)
  .map(({ path, code }) => ({ path, code: blankComments(code) }));

describe("design tokens", () => {
  it("finds the sources to check", () => {
    expect(files.length).toBeGreaterThan(0);
  });

  it("keeps src/index.css as the palette", () => {
    // Guards the rule itself: if the palette moves and TOKEN_SOURCE is not
    // moved with it, the exemption above would silently cover a file that no
    // longer defines any colour, and the real palette would start failing.
    const palette = sources.find(({ path }) => path === TOKEN_SOURCE);

    expect(palette, `${TOKEN_SOURCE} is where the light and dark palettes live.`).toBeDefined();
    expect(COLOR_LITERAL.test(palette?.code ?? "")).toBe(true);
  });

  it("has no colors_come_from_tokens violation", () => {
    const violations = files.flatMap(({ path, code }) =>
      code
        .split("\n")
        .map((line, index) => ({ line, number: index + 1 }))
        .filter(({ line }) => COLOR_LITERAL.test(line))
        .map(({ line, number }) => `${path}:${number}: ${line.trim()}`),
    );

    const why =
      `Colors come from the tokens in ${TOKEN_SOURCE} — use a Tailwind utility ` +
      "(`bg-card`, `text-muted-foreground`) in TSX and `var(--color-*)` in CSS.\n" +
      "A new shade belongs in the :root / .dark pair and the @theme inline block, " +
      "not at the call site.\n";

    expect(violations, why + violations.join("\n")).toEqual([]);
  });
});
