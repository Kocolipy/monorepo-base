import { expect, test } from "@playwright/test";

test.describe("ADMIN route guards", () => {
  test("may view the accounts page", async ({ page }) => {
    await page.goto("/accounts");

    await expect(page.getByRole("heading", { name: "Accounts" })).toBeVisible();
    await expect(page.getByText("Account management is coming soon.")).toBeVisible();
    await expect(page).toHaveURL(/\/accounts$/);
  });
});
