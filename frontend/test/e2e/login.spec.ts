import { expect, test } from "@playwright/test";

import { SESSION_COOKIE, captureSessionCookie, login, submitLogin } from "./auth.helpers";

// The signed-out half of the session contract: who is turned away, what they are
// told, and that signing out really ends the session rather than only clearing
// React state. This spec belongs to the `guest` project — empty `storageState` —
// because sign-in is its subject rather than its setup, so the per-test `login()`
// the E2E guide warns about is deliberate here.
//
// Flakiness rules for anything added here live in docs/TESTING_GUIDE.md.
test.describe("sessions, signed out", () => {
  test("keeps a rejected sign-in on the login page", async ({ page }) => {
    await submitLogin(page, "admin", "not-the-password");

    // The backend answers 401 and deliberately does not say which half was
    // wrong, so the SPA must not invent a more specific message either.
    await expect(page.getByRole("alert")).toHaveText("The username or password is incorrect.");
    await expect(page).toHaveURL(/\/$/);
    await expect(page.getByRole("button", { name: "Sign in" })).toBeEnabled();
  });

  test("sends a guest asking for the protected page to the login page", async ({ page }) => {
    await page.goto("/showcase");

    await expect(page.getByRole("heading", { name: "Welcome back" })).toBeVisible();
    await expect(page).toHaveURL(/\/$/);
  });

  test("sends a guest asking for the accounts page to the login page", async ({ page }) => {
    await page.goto("/accounts");

    await expect(page.getByRole("heading", { name: "Welcome back" })).toBeVisible();
    await expect(page).toHaveURL(/\/$/);
  });

  test("redirects an unknown path to the login page", async ({ page }) => {
    await page.goto("/no-such-page");

    await expect(page.getByRole("heading", { name: "Welcome back" })).toBeVisible();
    await expect(page).toHaveURL(/\/$/);
  });

  test("ends the session on sign out", async ({ page }) => {
    await login(page);

    await page.getByRole("button", { name: "Sign out" }).click();

    await expect(page.getByRole("heading", { name: "Welcome back" })).toBeVisible();
    await expect(page).toHaveURL(/\/$/);

    // Returning to the protected page is the assertion that matters: a sign-out
    // that only reset the provider's state would sail back in on a cookie the
    // backend still honours, and a reload is the one thing the SPA's own state
    // cannot survive.
    await page.goto("/showcase");

    await expect(page.getByRole("heading", { name: "Welcome back" })).toBeVisible();
    await expect(page).toHaveURL(/\/$/);
  });

  test("refuses a session cookie captured before sign out", async ({ page, context }) => {
    await login(page);
    const captured = await captureSessionCookie(context);

    await page.getByRole("button", { name: "Sign out" }).click();
    await expect(page.getByRole("heading", { name: "Welcome back" })).toBeVisible();

    // Sign-out clears the cookie, so the test above cannot tell a session the
    // backend destroyed from one the browser merely forgot. Putting the captured
    // id back is the replay: the request now carries an id the backend issued and
    // has since retired.
    await context.addCookies([captured]);

    // Without this the test could pass vacuously: a request carrying no session
    // cookie at all is also a 401, so the re-add has to be shown to have landed.
    const jar = await context.cookies();
    expect(jar.find((cookie) => cookie.name === SESSION_COOKIE)?.value).toBe(captured.value);

    // Assert on the API rather than the UI. A redirect to the login page would
    // also follow from the cleared cookie, where a 401 can only mean the backend
    // refused this id — so dropping `session.invalidate()` from logout fails here
    // and nowhere else. `page.request` shares the context's jar, and /me is safe,
    // so no CSRF header is owed.
    const replayed = await page.request.get("/api/auth/me");

    expect(replayed.status()).toBe(401);
  });
});
