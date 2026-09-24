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
