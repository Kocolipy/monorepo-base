import { expect, type BrowserContext, type Page } from "@playwright/test";

const ADMIN_CREDENTIALS = ["admin", "P@ssw0rd"] as const;

/**
 * The backend's session cookie (`server.servlet.session.cookie.name`).
 *
 * The CSRF cookie is deliberately not this one: `CookieCsrfTokenRepository`
 * holds the token outside the session, which is what lets `expireSession()`
 * below produce a `401` rather than a `403`.
 */
export const SESSION_COOKIE = "JSESSIONID";

/** Fill and submit the login form, without asserting where it lands. */
export async function submitLogin(page: Page, username: string, password: string) {
  await page.goto("/");
  await page.getByLabel("Username").fill(username);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Sign in" }).click();
}

/** Sign in as a named seeded identity and wait for the protected page. */
export async function loginAs(page: Page, username: string, password: string) {
  await submitLogin(page, username, password);

  await expect(page).toHaveURL(/\/showcase$/);
  await expect(page.getByText(`Signed in as ${username}`)).toBeVisible();
}

/** Sign in the seeded ADMIN identity used by the existing authenticated specs. */
export async function login(page: Page) {
  const [username, password] = ADMIN_CREDENTIALS;
  await loginAs(page, username, password);
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
 * Read the session cookie out of the jar so a spec can put it back later.
 *
 * The counterpart to `expireSession`: that one takes the cookie away to make the
 * *browser* forget a session the backend still honours, this one keeps a copy so
 * a spec can hand a *retired* id back to the backend and watch it be refused.
 */
export async function captureSessionCookie(context: BrowserContext) {
  const captured = (await context.cookies()).find((cookie) => cookie.name === SESSION_COOKIE);
  expect(captured, `the backend should have issued a ${SESSION_COOKIE} cookie`).toBeTruthy();

  return captured!;
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

/**
 * POST an administration action, obeying the CSRF contract.
 *
 * Returns the response rather than asserting on it, because both outcomes are
 * worth testing: an `ADMIN` gets the updated account, and a `USER` must get a
 * `403` for the *role* — which is only proven when the token is present, since a
 * missing token earns the same 403 from the CSRF filter first.
 */
export async function postAdminAction(page: Page, username: string, action: string) {
  await page.request.get("/api/auth/me");

  const cookies = await page.context().cookies();
  const token = cookies.find((cookie) => cookie.name === "XSRF-TOKEN")?.value;
  expect(token, "the backend should have seeded an XSRF-TOKEN cookie").toBeTruthy();

  return page.request.post(`/api/admin/accounts/${username}/${action}`, {
    headers: { "X-XSRF-TOKEN": String(token) },
  });
}
