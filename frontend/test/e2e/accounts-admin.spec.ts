import {
  expect,
  request,
  test,
  type APIRequestContext,
  type Locator,
  type Page,
} from "@playwright/test";

import { adminRequest, csrfHeaderFor, submitLoginViaApi } from "./auth.helpers";

/**
 * The Accounts page driven as an Admin uses it: both read-only projections,
 * Unlock, the forced password change, and a connector's token lifecycle, all
 * from the one interface.
 *
 * Every User this spec changes is one it PROVISIONS for itself, over SCIM,
 * with a token it issued through the page. The seeded `user` is never locked or
 * flagged here: since Unlock and a forced change both require a password change,
 * and password history refuses the old password back, a seeded identity put in
 * either state could not be restored for the specs that sign in as it. The
 * throwaway Users and the connector are deleted in a `finally`.
 *
 * Serial because the steps depend on each other: the token issued in the first
 * step is what provisions the Users every later step acts on.
 */

/** The backend itself: SCIM is not behind the Vite `/api` proxy. */
const BACKEND_URL = process.env.E2E_BACKEND_URL ?? "http://localhost:8080";

const USER_SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:User";

/** Mirrors `app.auth.lockout.max-attempts` (`APP_LOCKOUT_MAX_ATTEMPTS`). */
const REFUSALS_BEFORE_LOCKOUT = 5;

const RUN = Date.now().toString(36);
const CONNECTOR_NAME = `e2e-connector-${RUN}`;
const LOCKED_USER = `e2e-locked-${RUN}`;
const FORCED_USER = `e2e-forced-${RUN}`;
/** What the connector provisions; the connector write itself flags a change. */
const PROVISIONED_PASSWORD = "Provisioned-Secret-9x";
/** What each User sets for itself, clearing that flag, before the spec acts on it. */
const OWN_PASSWORD = "Self-Chosen-Secret-7q";

/** A cookie jar of its own, so nothing here touches the admin session this project replays. */
const anonymousApi = async (): Promise<APIRequestContext> =>
  request.newContext({
    baseURL: test.info().project.use.baseURL,
    storageState: { cookies: [], origins: [] },
  });

/** A bearer-authenticated SCIM client, straight to the backend. */
const scimApi = async (token: string): Promise<APIRequestContext> =>
  request.newContext({
    baseURL: BACKEND_URL,
    extraHTTPHeaders: {
      Accept: "application/scim+json",
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/scim+json",
    },
    storageState: { cookies: [], origins: [] },
  });

async function provision(scim: APIRequestContext, userName: string): Promise<string> {
  const created = await scim.post("/scim/v2/Users", {
    data: { active: true, password: PROVISIONED_PASSWORD, schemas: [USER_SCHEMA], userName },
  });
  expect(created.status(), `provisioning ${userName}`).toBe(201);
  return ((await created.json()) as { id: string }).id;
}

/** Deletes a SCIM User under its current ETag, as the conditional-write rules require. */
async function deprovision(scim: APIRequestContext, id: string) {
  const current = await scim.get(`/scim/v2/Users/${id}`);
  if (current.status() === 404) return;
  const deleted = await scim.delete(`/scim/v2/Users/${id}`, {
    headers: { "If-Match": current.headers()["etag"] ?? "" },
  });
  expect(deleted.status()).toBe(204);
}

/** Sign in as the provisioned User and replace the connector's password with its own. */
async function settle(userName: string) {
  const api = await anonymousApi();
  try {
    expect((await submitLoginViaApi(api, userName, PROVISIONED_PASSWORD)).status()).toBe(200);
    // Fetched after the login: it rotated the session and discarded the pre-login token.
    const changed = await api.post("/api/auth/change-password", {
      data: { currentPassword: PROVISIONED_PASSWORD, newPassword: OWN_PASSWORD },
      headers: await csrfHeaderFor(api),
    });
    expect(changed.status(), `${userName} sets its own password`).toBe(204);
  } finally {
    await api.dispose();
  }
}

/** Whether a fresh login as this User is accepted, and whether it is confined to the change. */
async function signInState(userName: string) {
  const api = await anonymousApi();
  try {
    const login = await submitLoginViaApi(api, userName, OWN_PASSWORD);
    if (login.status() !== 200) return { accepted: false, confined: false };
    const me = (await (await api.get("/api/auth/me")).json()) as {
      passwordChangeRequired?: boolean;
    };
    return { accepted: true, confined: me.passwordChangeRequired === true };
  } finally {
    await api.dispose();
  }
}

const usersTable = (page: Page) => page.getByRole("table", { name: "Users" });
const groupsTable = (page: Page) => page.getByRole("table", { name: "Groups" });

/**
 * A row addressed by its row header, exactly: `admin` must not also match
 * `admin2`. The `has` locator is resolved inside each row, so it is built from
 * the page rather than from the table.
 */
const rowOf = (page: Page, table: Locator, name: string) =>
  table.getByRole("row").filter({
    has: page.getByRole("rowheader", { name: new RegExp(`^${name}(?![\\w-])`) }),
  });

async function openAccounts(page: Page) {
  await page.goto("/accounts");
  await expect(page.getByRole("heading", { name: "Accounts" })).toBeVisible();
  await expect(usersTable(page).getByRole("row").nth(1)).toBeVisible();
  await expect(groupsTable(page).getByRole("row").nth(1)).toBeVisible();
}

test.describe.serial("ADMIN accounts page", () => {
  test("shows both projections with no writable identity or membership control", async ({
    page,
  }) => {
    await openAccounts(page);

    for (const table of [usersTable(page), groupsTable(page)]) {
      await expect(table.getByRole("textbox")).toHaveCount(0);
      await expect(table.getByRole("checkbox")).toHaveCount(0);
      await expect(table.getByRole("combobox")).toHaveCount(0);
      await expect(table.getByRole("spinbutton")).toHaveCount(0);
    }
    await expect(groupsTable(page).getByRole("button")).toHaveCount(0);
    for (const label of await usersTable(page).getByRole("button").allTextContents()) {
      expect(label).toMatch(/^(Unlock|Force password change for) /);
    }
    await expect(page.getByRole("button", { name: /Disable|Enable|Deactivate/ })).toHaveCount(0);

    // The protected Admin group is marked, and Unlock is described as the only lift.
    await expect(groupsTable(page).getByText("Protected Admin group")).toHaveCount(1);
    await expect(
      page.getByText(/A lockout never expires: Unlock is the only way it ends/),
    ).toBeVisible();

    // The Bootstrap Admin — this project's own session — shows no lockout state and no Unlock.
    const bootstrap = rowOf(page, usersTable(page), "admin");
    await expect(bootstrap.getByText("Bootstrap Admin")).toBeVisible();
    await expect(bootstrap.getByTitle("The Bootstrap Admin cannot be locked")).toBeVisible();
    await expect(bootstrap.getByText(/^(Locked|Not locked)$/)).toHaveCount(0);
    await expect(bootstrap.getByRole("button", { name: /Unlock/ })).toHaveCount(0);
  });

  test("unlocks a User, forces another's change and manages a connector's tokens", async ({
    page,
  }) => {
    // Two provisions, two self-service changes, a lockout's worth of refused
    // logins and four confirming sign-ins, each a password hash on the backend.
    test.setTimeout(120_000);
    const provisioned: string[] = [];
    let scim: APIRequestContext | undefined;

    try {
      // A connector and a READ_WRITE token, from the page. The value is read off
      // the one-time disclosure — there is no other way to get it.
      await openAccounts(page);
      await page.getByLabel("New connector name").fill(CONNECTOR_NAME);
      await page.getByRole("button", { name: "Create connector" }).click();
      const section = page.getByRole("region", { name: `Connector ${CONNECTOR_NAME}` });
      await expect(section).toBeVisible();
      await section.getByLabel(`Scope for ${CONNECTOR_NAME}`).selectOption("READ_WRITE");
      await section.getByRole("button", { name: `Issue token for ${CONNECTOR_NAME}` }).click();
      const firstValue = await page.getByLabel("New token value").textContent();
      expect(firstValue).toBeTruthy();
      await expect(section.getByText("Active")).toHaveCount(1);

      // Two Users of this spec's own, each settled on a password it chose.
      scim = await scimApi(firstValue!);
      provisioned.push(await provision(scim, LOCKED_USER), await provision(scim, FORCED_USER));
      await settle(LOCKED_USER);
      await settle(FORCED_USER);

      // Lock the first the way a forgetful person does.
      const guesser = await anonymousApi();
      try {
        for (let attempt = 0; attempt < REFUSALS_BEFORE_LOCKOUT; attempt += 1) {
          expect((await submitLoginViaApi(guesser, LOCKED_USER, "not-the-password")).status()).toBe(
            401,
          );
        }
      } finally {
        await guesser.dispose();
      }
      expect(await signInState(LOCKED_USER)).toEqual({ accepted: false, confined: false });

      // Unlock it from the page.
      await page.reload();
      const locked = rowOf(page, usersTable(page), LOCKED_USER);
      await expect(locked.getByText("Locked", { exact: true })).toBeVisible();
      await locked.getByRole("button", { name: `Unlock ${LOCKED_USER}` }).click();
      await expect(locked.getByText("Not locked")).toBeVisible();
      await expect(locked.getByText("Required")).toBeVisible();
      await expect(locked.getByRole("button", { name: /^Unlock/ })).toHaveCount(0);
      // The backend's doing: the password works again, into the change flow only.
      expect(await signInState(LOCKED_USER)).toEqual({ accepted: true, confined: true });

      // Force the second's change from the page.
      const forced = rowOf(page, usersTable(page), FORCED_USER);
      await expect(forced.getByText("No", { exact: true })).toBeVisible();
      await forced
        .getByRole("button", { name: `Force password change for ${FORCED_USER}` })
        .click();
      await expect(forced.getByText("Required")).toBeVisible();
      await expect(forced.getByRole("button", { name: /^Force password change/ })).toHaveCount(0);
      expect(await signInState(FORCED_USER)).toEqual({ accepted: true, confined: true });

      // Deprovision with the token that provisioned them, before rotating it away.
      for (const id of provisioned.splice(0)) await deprovision(scim, id);
      await scim.dispose();
      scim = undefined;

      // The disclosure did not survive the reload before the Unlock: the value
      // exists nowhere on the page any more, only in the client that used it.
      await expect(page.getByLabel("New token value")).toHaveCount(0);
      expect(await page.content()).not.toContain(firstValue!);

      // Rotate from the page: a new value, disclosed once; the old one stops working.
      await section.getByRole("button", { name: /^Rotate token/ }).click();
      const rotatedValue = await page.getByLabel("New token value").textContent();
      expect(rotatedValue).toBeTruthy();
      expect(rotatedValue).not.toBe(firstValue);
      const oldClient = await scimApi(firstValue!);
      const newClient = await scimApi(rotatedValue!);
      try {
        expect((await oldClient.get("/scim/v2/Users?count=1")).status()).toBe(401);
        expect((await newClient.get("/scim/v2/Users?count=1")).status()).toBe(200);

        // The disclosure is gone after a reload, and nothing on the page carries the value.
        await page.reload();
        await expect(page.getByLabel("New token value")).toHaveCount(0);
        expect(await page.content()).not.toContain(rotatedValue!);

        // Revoke from the page.
        await section.getByRole("button", { name: /^Revoke token/ }).click();
        await expect(section.getByRole("button", { name: /^Revoke token/ })).toHaveCount(0);
        await expect(section.getByText(/^Revoked /)).toHaveCount(1);
        expect((await newClient.get("/scim/v2/Users?count=1")).status()).toBe(401);
      } finally {
        await oldClient.dispose();
        await newClient.dispose();
      }

      // Delete the connector from the page.
      await section.getByRole("button", { name: `Delete ${CONNECTOR_NAME}` }).click();
      await expect(section).toHaveCount(0);
    } finally {
      await cleanUp(page, scim);
    }
  });
});

/**
 * Whatever the happy path did not get to remove — from this run or from an
 * earlier one that died mid-way: every `e2e-` User, through a token minted for
 * the purpose, then every `e2e-` connector, over the admin API.
 */
async function cleanUp(page: Page, scim: APIRequestContext | undefined) {
  await scim?.dispose();
  const connectors = (await (await page.request.get("/api/admin/connectors")).json()) as Array<{
    displayName: string;
    id: string;
  }>;
  const leftovers = connectors.filter((connector) =>
    connector.displayName.startsWith("e2e-connector-"),
  );
  const users = (await (await page.request.get("/api/admin/accounts")).json()) as Array<{
    id: string;
    userName: string;
  }>;
  const strays = users.filter((row) => /^e2e-(locked|forced)-/.test(row.userName));

  if (strays.length > 0) {
    let owner = leftovers[0];
    if (owner === undefined) {
      const created = await adminRequest(page, "POST", "/api/admin/connectors", {
        displayName: `e2e-connector-cleanup-${RUN}`,
      });
      owner = (await created.json()) as { displayName: string; id: string };
      leftovers.push(owner);
    }
    const issued = await adminRequest(page, "POST", `/api/admin/connectors/${owner.id}/tokens`, {
      scope: "READ_WRITE",
    });
    const client = await scimApi(
      ((await issued.json()) as { presentedValue: string }).presentedValue,
    );
    try {
      for (const stray of strays) await deprovision(client, stray.id);
    } finally {
      await client.dispose();
    }
  }
  for (const leftover of leftovers) {
    await adminRequest(page, "DELETE", `/api/admin/connectors/${leftover.id}`);
  }
}
