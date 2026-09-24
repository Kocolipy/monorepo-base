import { expect, type BrowserContext, type Page } from "@playwright/test";

const TEST_CREDENTIALS = ["admin", "P@ssw0rd"] as const;

/**
 * The backend's session cookie (`server.servlet.session.cookie.name`).
 *
 * The CSRF cookie is deliberately not this one: `CookieCsrfTokenRepository`
 * holds the token outside the session, which is what lets `expireSession()`
 * below produce a `401` rather than a `403`.
 */
const SESSION_COOKIE = "JSESSIONID";

/** Fill and submit the login form, without asserting where it lands. */
export async function submitLogin(page: Page, username: string, password: string) {
  await page.goto("/");
  await page.getByLabel("Username").fill(username);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Sign in" }).click();
}

/** Sign in the dedicated E2E identity and wait for the protected page. */
export async function login(page: Page) {
  const [username, password] = TEST_CREDENTIALS;
  await submitLogin(page, username, password);

  await expect(page).toHaveURL(/\/showcase$/);
  await expect(page.getByText(`Signed in as ${username}`)).toBeVisible();
}

/**
 * Expire the session the way a server-side timeout looks to the SPA: the next
 * request carries no session cookie, so the backend answers `401`.
 *
 * Only this browser context is touched — the session stays valid on the
 * backend, so a spec using this cannot break one running beside it. Every other
 * cookie is put back, the CSRF token included, because a request missing *that*
 * is a `403` and would exercise the wrong branch of `apiFetch`.
 */
export async function expireSession(context: BrowserContext) {
  const surviving = (await context.cookies()).filter((cookie) => cookie.name !== SESSION_COOKIE);

  await context.clearCookies();
  await context.addCookies(surviving);
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
