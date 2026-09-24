import { test as setup } from "@playwright/test";

import { loginAs } from "./auth.helpers";

setup("authenticate USER", async ({ page }) => {
  await loginAs(page, "user", "P@ssw0rd");
  await page.context().storageState({ path: "test/e2e/.auth/user.json" });
});

setup("authenticate ADMIN", async ({ page }) => {
  await loginAs(page, "admin", "P@ssw0rd");
  await page.context().storageState({ path: "test/e2e/.auth/admin.json" });
});
