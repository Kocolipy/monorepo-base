import { expect, test } from "@playwright/test";

test.describe("USER route guards", () => {
  test("may view the counter page", async ({ page }) => {
    await page.goto("/showcase");

    await expect(page.getByRole("heading", { name: "Front End" })).toBeVisible();
    await expect(page).toHaveURL(/\/showcase$/);
  });

  test("is redirected away from the accounts page", async ({ page }) => {
    await page.goto("/accounts");

    await expect(page.getByRole("heading", { name: "Front End" })).toBeVisible();
    await expect(page).toHaveURL(/\/showcase$/);
  });
});

test.describe("USER user listing", () => {
  /**
   * The server-side half of the role policy. The SPA redirect above proves only
   * that the page is unreachable; this proves the data is, which is what matters
   * when the caller is not a browser at all.
   *
   * 403 and not 401: the session is valid and the backend accepted it, the role
   * is what was refused. A 401 here would mean the request arrived
   * unauthenticated, and the test would be passing for the wrong reason.
   */
  test("is forbidden from listing accounts", async ({ page }) => {
    const response = await page.request.get("/api/admin/users");

    expect(response.status()).toBe(403);
    expect(await response.text()).not.toContain("@");
  });
});
