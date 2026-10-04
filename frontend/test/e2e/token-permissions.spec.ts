import { expect, test, type APIRequestContext } from "@playwright/test";

import { adminRequest, loginAsRole } from "./auth.helpers";
import { E2E_PREFIX, freshBrowser, runId, scimApi, USER_SCHEMA } from "./scim.helpers";

// Connector tokens carry Permissions (#117, ADR 0010), end to end: a Connector
// admin mints a `group:write`-only token from the SPA, the token writes Groups
// and is refused on Users writes, and an Account admin — who holds `group:write`
// through no Role and `connector:token` neither — is refused minting one.
//
// In its own project, after `guest`: the backend keeps one session per User and
// a sign-in ends every other one (#64), and `dev-roles.spec.ts` signs in as both
// of these fixture Users. Serial because the second test refuses a mint on the
// connector the first one created.

const GROUP_SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:Group";
const PATCH_OP = "urn:ietf:params:scim:api:messages:2.0:PatchOp";

const RUN = runId();
const CONNECTOR_NAME = `${E2E_PREFIX}group-writer-${RUN}`;

let connectorId: string | undefined;

test.describe.serial("connector token Permissions", () => {
  test("a Connector admin mints a group:write-only token that writes Groups and not Users", async ({
    page,
  }) => {
    let scim: APIRequestContext | undefined;
    let groupId: string | undefined;
    try {
      await loginAsRole(page, "connectorAdmin");
      await page.goto("/accounts");
      await expect(page.getByRole("heading", { name: "Connectors", exact: true })).toBeVisible();

      await page.getByLabel("New connector name").fill(CONNECTOR_NAME);
      await page.getByRole("button", { name: "Create connector" }).click();
      const section = page.getByRole("region", { name: `Connector ${CONNECTOR_NAME}` });
      await expect(section).toBeVisible();

      // Every directory Permission is the Connector admin's own, so each is offered;
      // only group:write is chosen.
      const choices = section.getByRole("group", { name: `Permissions for ${CONNECTOR_NAME}` });
      await expect(choices.getByRole("checkbox")).toHaveCount(4);
      for (const checkbox of await choices.getByRole("checkbox").all()) {
        await expect(checkbox).toBeEnabled();
      }
      const issue = section.getByRole("button", { name: `Issue token for ${CONNECTOR_NAME}` });
      await expect(issue).toBeDisabled();
      await choices.getByRole("checkbox", { name: /^group:write / }).check();
      await issue.click();

      const value = await page.getByLabel("New token value").textContent();
      expect(value).toBeTruthy();
      await expect(page.getByText(/^group:write · expires /)).toBeVisible();
      await expect(section.getByRole("cell", { name: "group:write", exact: true })).toHaveCount(1);
      const listed = (await (await page.request.get("/api/admin/connectors")).json()) as Array<{
        displayName: string;
        id: string;
        tokens: Array<{ permissions: string[] }>;
      }>;
      const mine = listed.find((connector) => connector.displayName === CONNECTOR_NAME);
      expect(mine?.tokens.map((token) => token.permissions)).toEqual([["group:write"]]);
      connectorId = mine?.id;

      scim = await scimApi(value!);

      // Groups writes: create, rename, delete.
      const created = await scim.post("/scim/v2/Groups", {
        data: { schemas: [GROUP_SCHEMA], displayName: `${E2E_PREFIX}group-${RUN}` },
      });
      expect(created.status()).toBe(201);
      groupId = ((await created.json()) as { id: string }).id;
      const renamed = await scim.patch(`/scim/v2/Groups/${groupId}`, {
        data: {
          schemas: [PATCH_OP],
          Operations: [
            { op: "replace", path: "displayName", value: `${E2E_PREFIX}group-renamed-${RUN}` },
          ],
        },
      });
      expect(renamed.status()).toBeLessThan(300);

      // A Users write is refused: the token authenticated, and lacks user:write.
      const userWrite = await scim.post("/scim/v2/Users", {
        data: { schemas: [USER_SCHEMA], userName: `${E2E_PREFIX}refused-${RUN}` },
      });
      expect(userWrite.status()).toBe(403);
      expect(userWrite.headers()["www-authenticate"]).toContain('error="insufficient_scope"');
      // And so is a read: write does not imply read.
      expect((await scim.get(`/scim/v2/Groups/${groupId}`)).status()).toBe(403);

      expect((await scim.delete(`/scim/v2/Groups/${groupId}`)).status()).toBe(204);
      groupId = undefined;
    } finally {
      if (groupId !== undefined) await scim?.delete(`/scim/v2/Groups/${groupId}`);
      await scim?.dispose();
    }
  });

  test("an Account admin is refused minting a token with group:write", async ({
    browser,
    page,
  }) => {
    expect(connectorId, "the first test created a connector").toBeTruthy();
    await loginAsRole(page, "accountAdmin");

    const refused = await adminRequest(
      page,
      "POST",
      `/api/admin/connectors/${connectorId}/tokens`,
      {
        permissions: ["group:write"],
      },
    );
    expect(refused.status()).toBe(403);

    // Nothing was minted: the connector still holds only the first test's token. Read in a
    // browser of its own — this page is already signed in, so the login form would not show.
    const context = await freshBrowser(browser);
    try {
      const reader = await context.newPage();
      await loginAsRole(reader, "connectorAdmin");
      const listed = (await (await reader.request.get("/api/admin/connectors")).json()) as Array<{
        id: string;
        tokens: unknown[];
      }>;
      expect(listed.find((connector) => connector.id === connectorId)?.tokens).toHaveLength(1);
    } finally {
      await context.close();
    }
  });

  test.afterAll(async ({ browser }) => {
    if (connectorId === undefined) return;
    const context = await browser.newContext({
      baseURL: test.info().project.use.baseURL,
      storageState: { cookies: [], origins: [] },
    });
    try {
      const page = await context.newPage();
      await loginAsRole(page, "connectorAdmin");
      const deleted = await adminRequest(page, "DELETE", `/api/admin/connectors/${connectorId}`);
      expect([204, 404]).toContain(deleted.status());
    } finally {
      await context.close();
    }
  });
});
