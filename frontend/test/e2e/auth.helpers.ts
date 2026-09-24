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

/**
 * Reset the counter through the API, obeying the backend's CSRF contract.
 *
 * `page.request` shares the browser context's cookie jar but adds no header of
 * its own, so the token has to be read out and echoed exactly as the SPA does —
 * otherwise this returns `403` rather than resetting anything.
 */
export async function resetCounterViaApi(page: Page) {
  // A safe request first: it guarantees a token exists even on a cold context.
  await page.request.get("/api/auth/me");

  const cookies = await page.context().cookies();
  const token = cookies.find((cookie) => cookie.name === "XSRF-TOKEN")?.value;
  expect(token, "the backend should have seeded an XSRF-TOKEN cookie").toBeTruthy();

  const response = await page.request.post("/api/count/reset", {
    headers: { "X-XSRF-TOKEN": String(token) },
  });
  expect(response.ok()).toBe(true);
}
