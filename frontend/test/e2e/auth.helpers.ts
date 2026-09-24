import { expect, type Page } from "@playwright/test";

const TEST_CREDENTIALS = ["admin", "P@ssw0rd"] as const;

/** Sign in the dedicated E2E identity and wait for the protected page. */
export async function login(page: Page) {
  const [username, password] = TEST_CREDENTIALS;
  await page.goto("/");
  await page.getByLabel("Username").fill(username);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Sign in" }).click();

  await expect(page).toHaveURL(/\/showcase$/);
  await expect(page.getByText(`Signed in as ${username}`)).toBeVisible();
}
