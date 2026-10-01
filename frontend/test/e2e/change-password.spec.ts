import {
  expect,
  request,
  test,
  type APIRequestContext,
  type Browser,
  type Page,
} from "@playwright/test";

import { adminRequest, csrfHeaderFor, submitLogin, submitLoginViaApi } from "./auth.helpers";

/**
 * The `/change-password` route driven end to end: a forced change confined to
 * the route, a voluntary one from the showcase, and a lockout reached through
 * the route itself.
 *
 * Runs in the `admin` project because the forced change is made from the
 * Accounts page. Every User here is PROVISIONED by the test over SCIM with a
 * connector it creates — never a seeded identity, which a forced change or a
 * lockout would leave unusable for the other specs — and is deleted, with the
 * connector, in a `finally`. Each User signs in through a browser context of
 * its own, so nothing touches the admin session this project replays.
 */

/** The backend itself: SCIM is not behind the Vite `/api` proxy. */
const BACKEND_URL = process.env.E2E_BACKEND_URL ?? "http://localhost:8080";

const USER_SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:User";

/** Mirrors `app.auth.lockout.max-attempts` (`APP_LOCKOUT_MAX_ATTEMPTS`). */
const REFUSALS_BEFORE_LOCKOUT = 5;

const RUN = Date.now().toString(36);
/** What the connector provisions; the connector write itself flags a change. */
const PROVISIONED = "Provisioned-Secret-9x";
/** What each User sets for itself through the API before the test begins. */
const OWN = "Self-Chosen-Secret-7q";
/** What each User changes to through the page. */
const REPLACEMENT = "Replacement-Secret-3k";

interface Provisioned {
  connectorId: string;
  scim: APIRequestContext;
  userId: string;
  userName: string;
}

const scimApi = (token: string) =>
  request.newContext({
    baseURL: BACKEND_URL,
    extraHTTPHeaders: {
      Accept: "application/scim+json",
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/scim+json",
    },
    storageState: { cookies: [], origins: [] },
  });

const anonymousApi = () =>
  request.newContext({
    baseURL: test.info().project.use.baseURL,
    storageState: { cookies: [], origins: [] },
  });

/**
 * A connector of the test's own, a User it provisions, and that User settled
 * on a password of its own choosing — so its change-required flag starts clear.
 */
async function provision(adminPage: Page, label: string): Promise<Provisioned> {
  const userName = `e2e-cp-${label}-${RUN}`;
  const connector = await adminRequest(adminPage, "POST", "/api/admin/connectors", {
    // Not `e2e-connector-…`: accounts-admin.spec.ts's clean-up deletes every
    // connector with that prefix, and it can run beside this spec.
    displayName: `e2e-cpw-${label}-${RUN}`,
  });
  expect(connector.ok(), "creating the connector").toBe(true);
  const connectorId = ((await connector.json()) as { id: string }).id;
  const issued = await adminRequest(
    adminPage,
    "POST",
    `/api/admin/connectors/${connectorId}/tokens`,
    {
      scope: "READ_WRITE",
    },
  );
  expect(issued.ok(), "issuing its token").toBe(true);
  const scim = await scimApi(((await issued.json()) as { presentedValue: string }).presentedValue);

  const created = await scim.post("/scim/v2/Users", {
    data: { active: true, password: PROVISIONED, schemas: [USER_SCHEMA], userName },
  });
  expect(created.status(), `provisioning ${userName}`).toBe(201);
  const userId = ((await created.json()) as { id: string }).id;

  const api = await anonymousApi();
  try {
    expect((await submitLoginViaApi(api, userName, PROVISIONED)).status()).toBe(200);
    // Fetched after the login: it rotated the session and discarded the pre-login token.
    const settled = await api.post("/api/auth/change-password", {
      data: { currentPassword: PROVISIONED, newPassword: OWN },
      headers: await csrfHeaderFor(api),
    });
    expect(settled.status(), `${userName} settles on its own password`).toBe(204);
  } finally {
    await api.dispose();
  }
  return { connectorId, scim, userId, userName };
}

async function deprovision(adminPage: Page, provisioned: Provisioned | undefined) {
  if (provisioned === undefined) return;
  const { connectorId, scim, userId } = provisioned;
  try {
    const current = await scim.get(`/scim/v2/Users/${userId}`);
    if (current.status() !== 404) {
      const deleted = await scim.delete(`/scim/v2/Users/${userId}`, {
        headers: { "If-Match": current.headers()["etag"] ?? "" },
      });
      expect(deleted.status()).toBe(204);
    }
  } finally {
    await scim.dispose();
    await adminRequest(adminPage, "DELETE", `/api/admin/connectors/${connectorId}`);
  }
}

/** A browser context with no session, as a User's own browser. */
async function freshBrowser(browser: Browser) {
  return browser.newContext({
    baseURL: test.info().project.use.baseURL,
    storageState: { cookies: [], origins: [] },
  });
}

const heading = (page: Page) => page.getByRole("heading", { name: "Change your password" });

async function submitChange(page: Page, current: string, next: string, confirm: string = next) {
  await page.getByLabel("Current password").fill(current);
  await page.getByLabel("New password", { exact: true }).fill(next);
  await page.getByLabel("Confirm new password").fill(confirm);
  await page.getByRole("button", { name: "Change password" }).click();
}

async function expectFieldsCleared(page: Page) {
  for (const label of ["Current password", "Confirm new password"]) {
    await expect(page.getByLabel(label)).toHaveValue("");
  }
  await expect(page.getByLabel("New password", { exact: true })).toHaveValue("");
}

/** After a successful change: on login, told why, and the new password works. */
async function expectSignedOutThenSignIn(page: Page, userName: string, password: string) {
  await expect(page.getByRole("heading", { name: "Welcome back" })).toBeVisible();
  await expect(page).toHaveURL(/\/$/);
  await expect(page.getByRole("status")).toHaveText(
    "Your password was changed. Sign in with your new password.",
  );

  await submitLogin(page, userName, password);
  await expect(page).toHaveURL(/\/showcase$/);
  await expect(page.getByText(`Signed in as ${userName}`)).toBeVisible();
  const me = (await (await page.request.get("/api/auth/me")).json()) as {
    passwordChangeRequired: boolean;
  };
  expect(me.passwordChangeRequired).toBe(false);
}

test("an Admin's forced change confines the User to /change-password until it is made", async ({
  browser,
  page,
}) => {
  test.setTimeout(120_000);
  let user: Provisioned | undefined;
  const context = await freshBrowser(browser);
  try {
    user = await provision(page, "forced");

    // The Admin forces the change from the Accounts page.
    await page.goto("/accounts");
    const row = page
      .getByRole("table", { name: "Users" })
      .getByRole("row")
      .filter({
        has: page.getByRole("rowheader", { name: new RegExp(`^${user.userName}(?![\\w-])`) }),
      });
    await row.getByRole("button", { name: `Force password change for ${user.userName}` }).click();
    await expect(row.getByText("Required")).toBeVisible();

    // The User signs in and lands on the change, not the showcase.
    const own = await context.newPage();
    await submitLogin(own, user.userName, OWN);
    await expect(own).toHaveURL(/\/change-password$/);
    await expect(heading(own)).toBeVisible();
    await expect(
      own.getByText("Your password must be replaced before you can continue.", { exact: false }),
    ).toBeVisible();
    await expect(own.getByRole("button", { name: "Sign out" })).toBeVisible();

    // Confined: a direct navigation elsewhere is sent back.
    for (const elsewhere of ["/showcase", "/accounts", "/", "/no-such-page"]) {
      await own.goto(elsewhere);
      await expect(own).toHaveURL(/\/change-password$/);
      await expect(heading(own)).toBeVisible();
    }

    // A wrong current password is refused, and the fields are cleared.
    await submitChange(own, "Not-The-Current-One-1", REPLACEMENT);
    await expect(own.getByRole("alert")).toHaveText("The current password is incorrect.");
    await expectFieldsCleared(own);
    await expect(own).toHaveURL(/\/change-password$/);

    // A policy-violating password is refused by the rule the backend names.
    // Reuse, not length: the browser's `minLength` now holds back a short one
    // before it is sent (see the policy test below).
    await submitChange(own, OWN, OWN);
    await expect(own.getByRole("alert")).toHaveText(
      /^The new password must differ from the current password and the \d+ most recent ones$/,
    );
    await expectFieldsCleared(own);
    expect(await own.content()).not.toContain(OWN);

    // The change itself: back to login, then in with the new password, unflagged.
    await submitChange(own, OWN, REPLACEMENT);
    await expectSignedOutThenSignIn(own, user.userName, REPLACEMENT);
  } finally {
    await context.close();
    await deprovision(page, user);
  }
});

test("an unflagged User changes its password voluntarily from the showcase", async ({
  browser,
  page,
}) => {
  test.setTimeout(120_000);
  let user: Provisioned | undefined;
  const context = await freshBrowser(browser);
  try {
    user = await provision(page, "voluntary");

    const own = await context.newPage();
    await submitLogin(own, user.userName, OWN);
    await expect(own).toHaveURL(/\/showcase$/);

    await own.getByRole("link", { name: "Change password" }).click();
    await expect(own).toHaveURL(/\/change-password$/);
    await expect(
      own.getByText("Choose a new password for your account.", { exact: false }),
    ).toBeVisible();
    await expect(own.getByRole("link", { name: "Back" })).toBeVisible();

    await submitChange(own, OWN, REPLACEMENT);
    await expectSignedOutThenSignIn(own, user.userName, REPLACEMENT);
  } finally {
    await context.close();
    await deprovision(page, user);
  }
});

test("the form states the policy, and the browser holds back a too-short password", async ({
  browser,
  page,
}) => {
  test.setTimeout(120_000);
  let user: Provisioned | undefined;
  const context = await freshBrowser(browser);
  try {
    user = await provision(page, "policy");

    const own = await context.newPage();
    await submitLogin(own, user.userName, OWN);
    await expect(own).toHaveURL(/\/showcase$/);
    await own.getByRole("link", { name: "Change password" }).click();
    await expect(heading(own)).toBeVisible();

    // The rules are stated before anything is typed, and are the new field's description.
    const newPassword = own.getByLabel("New password", { exact: true });
    await expect(newPassword).toHaveAccessibleDescription(
      [
        "12 to 256 characters long",
        "Must not contain your user name",
        "Must not reuse your current or recent passwords",
      ].join(" "),
    );

    // Too short: the browser's own constraint validation stops the submit, so
    // nothing reaches the backend and no refusal is rendered.
    const changes: string[] = [];
    own.on("request", (sent) => {
      if (sent.url().endsWith("/api/auth/change-password")) changes.push(sent.method());
    });
    await submitChange(own, OWN, "short-1");
    expect(await newPassword.evaluate((input: HTMLInputElement) => input.validity.tooShort)).toBe(
      true,
    );
    await expect(own.getByRole("alert")).toHaveCount(0);
    await expect(heading(own)).toBeVisible();
    expect(changes).toEqual([]);

    // Long enough but containing the user name: the browser lets it through,
    // and the backend's rule — one the page stated — is shown verbatim.
    await submitChange(own, OWN, `${user.userName}-Secret-5w`);
    await expect(own.getByRole("alert")).toHaveText(
      "The new password must not contain the user name",
    );
    expect(changes).toEqual(["POST"]);
    await expectFieldsCleared(own);
  } finally {
    await context.close();
    await deprovision(page, user);
  }
});

test("wrong current passwords lock the account, and the page says an Admin must Unlock it", async ({
  browser,
  page,
}) => {
  test.setTimeout(120_000);
  let user: Provisioned | undefined;
  const context = await freshBrowser(browser);
  try {
    user = await provision(page, "locked");

    const own = await context.newPage();
    await submitLogin(own, user.userName, OWN);
    await expect(own).toHaveURL(/\/showcase$/);
    await own.getByRole("link", { name: "Change password" }).click();
    await expect(heading(own)).toBeVisible();

    for (let attempt = 1; attempt < REFUSALS_BEFORE_LOCKOUT; attempt += 1) {
      await submitChange(own, `Wrong-Guess-${attempt}-xyz`, REPLACEMENT);
      await expect(own.getByRole("alert")).toHaveText("The current password is incorrect.");
    }
    await submitChange(own, "Wrong-Guess-last-xyz", REPLACEMENT);
    await expect(own.getByRole("alert")).toHaveText(
      "Too many incorrect passwords: the account is now locked and this session has ended. An Admin must Unlock the account before you can sign in again.",
    );
    await expect(own.getByLabel("Current password")).toBeDisabled();
    await expect(own.getByRole("button", { name: "Change password" })).toBeDisabled();

    // The backend's side of it: this session is gone and the password no longer signs in.
    expect((await own.request.get("/api/auth/me")).status()).toBe(401);
    const api = await anonymousApi();
    try {
      expect((await submitLoginViaApi(api, user.userName, OWN)).status()).toBe(401);
    } finally {
      await api.dispose();
    }

    // Signing out still works, and leads to login.
    await own.getByRole("button", { name: "Sign out" }).click();
    await expect(own.getByRole("heading", { name: "Welcome back" })).toBeVisible();
  } finally {
    await context.close();
    await deprovision(page, user);
  }
});
