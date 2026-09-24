import { expect, test } from "@playwright/test";

import { resetCounterViaApi } from "./auth.helpers";

test.describe("authentication and counter", () => {
  test.beforeEach(async ({ page }) => {
    await resetCounterViaApi(page);
  });

  test("opens the protected showcase with the saved session and manages the counter", async ({
    page,
  }) => {
    await page.goto("/showcase");

    await expect(page).toHaveURL(/\/showcase$/);
    await expect(page.getByText("Signed in as admin")).toBeVisible();

    const count = page.getByTestId("count");
    const increment = page.getByRole("button", { name: "Increment" });
    const reset = page.getByRole("button", { name: "Reset" });

    await expect(count).toHaveText("Clicked 0 times");
    await expect(reset).toBeDisabled();

    await increment.click();
    await expect(count).toHaveText("Clicked 1 time");

    await increment.click();
    await expect(count).toHaveText("Clicked 2 times");

    await reset.click();
    await expect(count).toHaveText("Clicked 0 times");
    await expect(reset).toBeDisabled();
  });
});
