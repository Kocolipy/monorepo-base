import { expect, test } from "@playwright/test";

import { login, submitLogin } from "./auth.helpers";

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
});
