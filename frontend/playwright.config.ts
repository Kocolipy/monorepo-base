import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./test/e2e",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  // Capped at 2 locally: the dev box runs tight on memory, and one Chromium
  // per worker is the biggest cost in this suite. CI stays serial.
  workers: process.env.CI ? 1 : 2,
  reporter: "html",
  use: {
    baseURL: "http://localhost:5173",
    trace: "on-first-retry",
    screenshot: "only-on-failure",
  },

  projects: [
    {
      name: "setup",
      testMatch: /auth\.setup\.ts/,
      use: { ...devices["Desktop Chrome"] },
    },

    // `testMatch` names its specs explicitly rather than globbing `*.spec.ts`,
    // which is what gives test/arch/e2eSpecRouting.test.ts something to
    // enforce: a new spec that nobody routed here fails that arch test instead
    // of being silently skipped at runtime.
    {
      name: "guest",
      testMatch: /(?:smoke|login)\.spec\.ts/,
      use: {
        ...devices["Desktop Chrome"],
        storageState: { cookies: [], origins: [] },
      },
    },
    {
      name: "user",
      testMatch: /roles-user\.spec\.ts/,
      dependencies: ["setup"],
      use: {
        ...devices["Desktop Chrome"],
        storageState: "test/e2e/.auth/user.json",
      },
    },
    {
      name: "admin",
      testMatch: /(?:authentication|session|roles-admin|accounts-admin)\.spec\.ts/,
      // After the `user` project, not beside it. Disabling an account now revokes
      // the sessions it holds, and `accounts-admin.spec.ts` disables the seeded
      // `user` — which is the very session the `user` project replays. Running
      // them in parallel would have this project pull that one's session out from
      // under it, so the destructive project goes last.
      //
      // The `guest` project needs no such ordering: it signs in as `admin`, and
      // disabling the last enabled administrator is refused.
      dependencies: ["setup", "user"],
      use: {
        ...devices["Desktop Chrome"],
        storageState: "test/e2e/.auth/admin.json",
      },
    },
  ],

  webServer: {
    command: "npm run dev",
    url: "http://localhost:5173",
    reuseExistingServer: !process.env.CI,
    timeout: 120 * 1000,
  },
});
