import { fileURLToPath } from "node:url";
import { configDefaults, coverageConfigDefaults, defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react-swc";

// Pinned so a date-formatting assertion means the same thing on a developer's
// machine as it does in CI.
process.env.TZ = "UTC";

export default defineConfig({
  // Deliberately not @tailwindcss/vite: unit tests stub CSS imports, so
  // generating the utility stylesheet for every test file would be pure cost.
  // The one thing that needs real CSS is the E2E suite, which runs against the
  // dev server and therefore against vite.config.ts.
  plugins: [react()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  test: {
    exclude: [...configDefaults.exclude, "**/.claude/**", "**/.stryker-tmp/**", "**/e2e/**"],
    coverage: {
      exclude: [
        ...coverageConfigDefaults.exclude,
        // Files with zero executable statements. v8 reports 0/0 as 0%, so
        // leaving them in drags the report down with rows no test can cover.
        "**/*.css",
        "**/*.d.ts",
        "**/e2e/**",
        "**/scripts/**",
        "playwright.config.ts",
        // The composition root: it calls createRoot against the real document
        // and has no branch to assert. The smoke E2E covers that it mounts.
        "src/main.tsx",
      ],
    },
    environment: "happy-dom",
    globals: true,
    setupFiles: ["./test/setup.ts"],
  },
});
