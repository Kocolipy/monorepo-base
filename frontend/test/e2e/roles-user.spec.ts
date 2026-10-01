import { expect, test } from "@playwright/test";

import { postAdminAction } from "./auth.helpers";

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
    const response = await page.request.get("/api/admin/accounts");

    expect(response.status()).toBe(403);
    expect(await response.text()).not.toContain("@");
  });

  /**
   * The control endpoints matter more than the listing here: a `USER` who could
   * reach them could unlock or flag an administrator. The CSRF token is sent
   * deliberately — without it the chain answers 403 from the CSRF filter first,
   * and the test would pass without ever exercising the role check. Any id will
   * do: the role is refused before the target is looked up.
   */
  test("is forbidden from the Groups listing and from unlocking or flagging a User", async ({
    page,
  }) => {
    // Precondition, so the test cannot pass vacuously on an admin session.
    expect(await (await page.request.get("/api/auth/me")).json()).toMatchObject({ role: "USER" });

    expect((await page.request.get("/api/admin/groups")).status()).toBe(403);
    for (const action of ["unlock", "force-password-change"]) {
      const response = await postAdminAction(page, crypto.randomUUID(), action);

      expect(response.status(), `a USER must not reach ${action}`).toBe(403);
    }
  });
});
