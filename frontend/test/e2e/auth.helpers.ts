import { expect, type APIRequestContext, type BrowserContext, type Page } from "@playwright/test";

const ADMIN_CREDENTIALS = ["admin", "P@ssw0rd"] as const;

/**
 * The backend's session cookie (`server.servlet.session.cookie.name`).
 *
 * There is no CSRF cookie at all: the token lives in the session and is fetched
 * from `GET /api/auth/csrf`, so removing this cookie (see `expireSession()`)
 * takes the token with it.
 */
export const SESSION_COOKIE = "JSESSIONID";

/**
 * The CSRF header for an unsafe request made through `api`'s cookie jar,
 * fetched exactly as the SPA fetches it: `GET /api/auth/csrf` returns the
 * session's token and the header to send it in, and opens a session on a cold
 * jar. `page.request` is such a context, sharing its page's cookies.
 *
 * Fetch it after anything that changes the session — a login rotates the id and
 * discards the pre-login token.
 */
export async function csrfHeaderFor(api: APIRequestContext): Promise<Record<string, string>> {
  const response = await api.get("/api/auth/csrf");
  expect(response.status(), "GET /api/auth/csrf").toBe(200);
  const { headerName, token } = (await response.json()) as { headerName: string; token: string };
  expect(token, "the backend should have returned a CSRF token").toBeTruthy();
  return { [headerName]: token };
}

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
 * request carries no session cookie. A safe request is answered `401`; an
 * unsafe one is first refused `403` (its token belonged to the session), and
 * `apiFetch`'s single re-fetch-and-retry then lands on the same `401`.
 *
 * Only this browser context is touched — the session stays valid on the
 * backend, so a spec using this cannot break one running beside it.
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
 * its own, so the token has to be fetched and echoed exactly as the SPA does —
 * otherwise this returns `403` rather than resetting anything.
 */
export async function resetCounterViaApi(page: Page) {
  const response = await page.request.post("/api/count/reset", {
    headers: await csrfHeaderFor(page.request),
  });
  expect(response.ok()).toBe(true);
}

/**
 * POST credentials to the login endpoint, obeying the CSRF contract, and return
 * the response.
 *
 * Takes its own {@link APIRequestContext} rather than a `Page` on purpose: a spec
 * that drives the login path must not do it through the cookie jar of a browser
 * context replaying a shared session, because an accepted login rotates that
 * session's id on the backend and every spec running beside it would lose it.
 * Build the context with an empty `storageState` — see `lockAccount` in
 * `accounts-admin.spec.ts`.
 */
export async function submitLoginViaApi(
  api: APIRequestContext,
  username: string,
  password: string,
) {
  // The fetch opens the anonymous session the token — and then the login — belongs to.
  const csrf = await csrfHeaderFor(api);

  return api.post("/api/auth/login", {
    data: { username, password },
    // A login refused for the credentials and a login refused for a missing
    // token are both 401/403 shaped, so the token has to be present for the
    // response to mean anything about the password.
    headers: csrf,
  });
}

/** The CSRF header for an unsafe request made through this page's cookie jar. */
async function csrfHeader(page: Page): Promise<Record<string, string>> {
  return csrfHeaderFor(page.request);
}

/**
 * POST an administration action on a User, obeying the CSRF contract. Addressed
 * by the User's stable id, which is what the operations take.
 *
 * Returns the response rather than asserting on it, because both outcomes are
 * worth testing: an `ADMIN` gets the updated row, and a `USER` must get a
 * `403` for the *role* — which is only proven when the token is present, since a
 * missing token earns the same 403 from the CSRF filter first.
 */
export async function postAdminAction(page: Page, userId: string, action: string) {
  return page.request.post(`/api/admin/accounts/${userId}/${action}`, {
    headers: await csrfHeader(page),
  });
}

/** Any unsafe administration request, obeying the CSRF contract. */
export async function adminRequest(
  page: Page,
  method: "POST" | "PUT" | "PATCH" | "DELETE",
  path: string,
  data?: unknown,
) {
  return page.request.fetch(path, { data, headers: await csrfHeader(page), method });
}

/** The stable id of a listed User, read from the Users projection by `userName`. */
export async function userIdOf(page: Page, userName: string): Promise<string> {
  const listing = (await (await page.request.get("/api/admin/accounts")).json()) as Array<{
    id: string;
    userName: string;
  }>;
  const found = listing.find((row) => row.userName === userName);
  expect(found, `${userName} should be listed`).toBeTruthy();
  return found!.id;
}
