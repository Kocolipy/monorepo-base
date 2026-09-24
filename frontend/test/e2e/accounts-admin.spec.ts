import { expect, test, type Page } from "@playwright/test";

import { postAdminAction } from "./auth.helpers";

/**
 * The accounts page driven as an administrator uses it.
 *
 * Serial, and the only place a spec changes a seeded account's standing through
 * the UI. Two reasons: the suite runs `fullyParallel`, so tests in one file
 * would otherwise race each other over the same row; and the seeded `user`
 * identity is shared with the `user` project running beside this one, so the
 * round trip below has to put it back before anything else reads it.
 *
 * Parallel specs are unaffected *while* it is disabled because they replay a
 * saved session — the backend decides account status when authenticating, not on
 * every request.
 */
test.describe.serial("ADMIN accounts page", () => {
  const openAccounts = async (page: Page) => {
    await page.goto("/accounts");
    await expect(page.getByRole("heading", { name: "Accounts" })).toBeVisible();
    // The listing has arrived: every later assertion reads a rendered row
    // rather than the loading state.
    await expect(page.getByRole("rowheader", { exact: true, name: "admin" })).toBeVisible();
  };

  const accountRow = (page: Page, username: string) =>
    page
      .getByRole("row")
      .filter({ has: page.getByRole("rowheader", { exact: true, name: username }) });

  test("lists every registered account with its role, status, and creation date", async ({
    page,
  }) => {
    await openAccounts(page);

    // The seeded identities, by role rather than by name where the assertion
    // allows it, so renaming a seed account in configuration does not fail this.
    await expect(page.getByRole("rowheader", { exact: true, name: "user" })).toBeVisible();

    const admin = accountRow(page, "admin");
    await expect(admin).toContainText("ADMIN");
    await expect(admin).toContainText("@");
    // A date in the rendered ISO form the page formats, whatever day the
    // environment first seeded the account.
    await expect(admin).toContainText(/\d{4}-\d{2}-\d{2}/);
    await expect(admin.getByText("Active")).toBeVisible();
  });

  test("offers no unlock for an account serving no lockout", async ({ page }) => {
    await openAccounts(page);

    await expect(
      accountRow(page, "user").getByRole("button", { name: "Unlock user" }),
    ).toBeDisabled();
  });

  /**
   * The self-disable refusal, shown before the click rather than after it. The
   * backend refuses the same request with a 409 — asserted in
   * `roles-admin.spec.ts`, where the response status is visible.
   */
  test("does not offer to disable the account making the request", async ({ page }) => {
    await openAccounts(page);

    const disableSelf = accountRow(page, "admin").getByRole("button", { name: "Disable admin" });
    await expect(disableSelf).toBeDisabled();
    await expect(disableSelf).toHaveAttribute("title", "You cannot disable your own account");
  });

  /**
   * The whole point of the page: an administrator closes an account to logins
   * and reopens it without leaving the browser. Restored in a `finally` even
   * when an expectation fails, because the `user` project depends on that
   * account existing and enabled.
   */
  test("closes an account to logins and reopens it", async ({ page }) => {
    await openAccounts(page);
    const row = accountRow(page, "user");

    try {
      await row.getByRole("button", { name: "Disable user" }).click();

      await expect(row.getByText("Disabled")).toBeVisible();
      await expect(row.getByRole("button", { name: "Enable user" })).toBeVisible();
      await expect(page.getByRole("alert")).toHaveCount(0);

      // The change is the backend's, not just the rendered row's: a reload shows
      // the same state.
      await page.reload();
      await expect(accountRow(page, "user").getByText("Disabled")).toBeVisible();

      await accountRow(page, "user").getByRole("button", { name: "Enable user" }).click();
      await expect(accountRow(page, "user").getByText("Active")).toBeVisible();
    } finally {
      // Belt and braces: if the UI enable above never ran, put the account back
      // through the API so no other spec inherits a disabled account.
      const restored = await postAdminAction(page, "user", "enable");
      expect(restored.status()).toBe(200);
    }
  });
});
