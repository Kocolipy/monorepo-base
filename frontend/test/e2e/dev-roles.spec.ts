import { expect, test } from "@playwright/test";

import { DEV_ROLES, loginAsRole, type DevRole } from "./auth.helpers";

// The development role mapping's fixture Users: each signs in through the login
// page and holds exactly its Role's Permissions on `GET /api/auth/me`. This is
// what lets a later spec sign in as a Role rather than as the Admin.
//
// In the `guest` project — empty `storageState` — because signing in is the
// subject. The Superuser is left out on purpose: its User is the Bootstrap
// Admin, whose sign-in would end the session `admin.json` replays and that
// `login.spec.ts` uses beside this file; `roles-admin.spec.ts` checks it on the
// replayed session instead. Each fixture User is signed in by this file alone.
const FIXTURE_ROLES: DevRole[] = ["accountAdmin", "auditor", "connectorAdmin", "monitoring"];

test.describe("development Roles", () => {
  for (const role of FIXTURE_ROLES) {
    test(`${DEV_ROLES[role].username} signs in holding the ${role} Permissions`, async ({
      page,
    }) => {
      await loginAsRole(page, role);

      const me = await page.request.get("/api/auth/me");
      expect(me.status()).toBe(200);
      const body = (await me.json()) as { permissions: string[]; role: string };
      expect(body.permissions).toEqual(DEV_ROLES[role].permissions);
      // Not an Admin: the Roles are not the Admin group.
      expect(body.role).toBe("USER");
    });
  }
});
