import { test as setup } from "@playwright/test";

import { login } from "./auth.helpers";

const STORAGE_STATE = "test/e2e/.auth/user.json";

setup("authenticate", async ({ page }) => {
  await login(page);
  await page.context().storageState({ path: STORAGE_STATE });
});
