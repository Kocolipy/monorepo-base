import { expect, test } from "@playwright/test";

import { expireSession } from "./auth.helpers";

const CSRF_HEADER = "x-xsrf-token";

// What the SPA does when a request it expected to succeed comes back refused.
// `src/lib/http.ts` classifies 401 and a *persistent* 403 into different results
// and `src/pages/showcase.tsx` answers them differently — a real backend on the
// other end is the only thing that proves those two statuses are what it
// actually sends. The unit suites cover the same branches against a stubbed
// `fetch`; these cover that the contract behind them is real.
//
// Neither test asserts a counter value: this spec shares the backend's single
// counter with authentication.spec.ts under `fullyParallel`. Both requests here
// are refused, so the count never moves.
test.describe("sessions, signed in", () => {
  test("returns to the login page when the session has expired", async ({ context, page }) => {
    await page.goto("/showcase");

    const increment = page.getByRole("button", { name: "Increment" });
    // The initial load disables the button; waiting for it also guarantees the
    // provider has finished checking the session before we break it.
    await expect(increment).toBeEnabled();

    await expireSession(context);

    await increment.click();

    // The 401 has to move the auth state to guest, not just show an error:
    // ProtectedRoute is what carries the user back to sign in.
    await expect(page.getByRole("heading", { name: "Welcome back" })).toBeVisible();
    await expect(page).toHaveURL(/\/$/);
  });

  test("reports an expired security token without ending the session", async ({ page }) => {
    await page.goto("/showcase");

    const increment = page.getByRole("button", { name: "Increment" });
    await expect(increment).toBeEnabled();

    // Corrupt the echoed token rather than deleting the cookie: `apiFetch`
    // answers a 403 by re-seeding the cookie and retrying once, so a deleted
    // cookie would simply be replaced and the request would succeed. Rewriting
    // the header on every attempt makes the backend reject both, which is the
    // only way to reach the `csrf-expired` result.
    await page.route("**/api/count/increment", async (route) => {
      await route.continue({
        headers: { ...route.request().headers(), [CSRF_HEADER]: "not-the-current-token" },
      });
    });

    await increment.click();

    await expect(page.getByRole("alert")).toHaveText(
      "Your security token expired. Please try again.",
    );

    // A 403 is not a 401: the session survives, so the user stays where they are
    // instead of being sent back to sign in.
    await expect(page).toHaveURL(/\/showcase$/);
    await expect(page.getByText("Signed in as admin")).toBeVisible();
  });
});
