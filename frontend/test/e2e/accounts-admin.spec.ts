import { expect, request, test, type APIRequestContext, type Page } from "@playwright/test";

import { postAdminAction, submitLoginViaApi } from "./auth.helpers";

/**
 * The accounts page driven as an administrator uses it.
 *
 * Serial, and the only place a spec changes a seeded account's standing — whether
 * it is disabled, and whether it is serving a lockout. Two reasons: the suite runs
 * `fullyParallel`, so tests in one file would otherwise race each other over the
 * same row; and the seeded `user` identity is shared with the `user` project, so
 * every round trip below has to put it back before anything else reads it.
 *
 * Disabling that account also ends the sessions it holds, the `user` project's
 * replayed one included — which is why `playwright.config.ts` makes this project
 * depend on `user` rather than run beside it.
 */
test.describe.serial("ADMIN accounts page", () => {
  const openAccounts = async (page: Page) => {
    await page.goto("/accounts");
    await expect(page.getByRole("heading", { name: "Accounts" })).toBeVisible();
    // The listing has arrived: every later assertion reads a rendered row
    // rather than the loading state.
    await expect(page.getByRole("rowheader", { exact: true, name: "admin" })).toBeVisible();
  };

  const accountRow = (page: Page, username: string) =>
    page
      .getByRole("row")
      .filter({ has: page.getByRole("rowheader", { exact: true, name: username }) });

  /** The seeded `USER` password, as `auth.setup.ts` signs in with it. */
  const USER_PASSWORD = "P@ssw0rd";

  /** Mirrors `app.auth.lockout.max-attempts` (`APP_LOCKOUT_MAX_ATTEMPTS`). */
  const REFUSALS_BEFORE_LOCKOUT = 5;

  /**
   * A cookie jar of its own for the login attempts below, so nothing here
   * touches the admin session this project replays: an accepted login rotates
   * the session id of the jar it arrives in.
   */
  const anonymousApi = async (): Promise<APIRequestContext> =>
    request.newContext({
      baseURL: test.info().project.use.baseURL,
      storageState: { cookies: [], origins: [] },
    });

  /** Drive the seeded account into a lockout the way a forgetful person does. */
  const lockAccount = async (api: APIRequestContext, username: string) => {
    for (let attempt = 0; attempt < REFUSALS_BEFORE_LOCKOUT; attempt += 1) {
      const refused = await submitLoginViaApi(api, username, "not-the-password");
      expect(refused.status()).toBe(401);
    }
  };

  test("lists every registered account with its role, status, and creation date", async ({
    page,
  }) => {
    await openAccounts(page);

    // The seeded identities, by role rather than by name where the assertion
    // allows it, so renaming a seed account in configuration does not fail this.
    await expect(page.getByRole("rowheader", { exact: true, name: "user" })).toBeVisible();

    const admin = accountRow(page, "admin");
    await expect(admin).toContainText("ADMIN");
    // A date in the rendered ISO form the page formats, whatever day the
    // environment first seeded the account.
    await expect(admin).toContainText(/\d{4}-\d{2}-\d{2}/);
    await expect(admin.getByText("Active")).toBeVisible();
  });

  test("offers no unlock for an account serving no lockout", async ({ page }) => {
    await openAccounts(page);

    await expect(
      accountRow(page, "user").getByRole("button", { name: "Unlock user" }),
    ).toBeDisabled();
  });

  /**
   * The self-disable refusal, shown before the click rather than after it. The
   * backend refuses the same request with a 409 — asserted in
   * `roles-admin.spec.ts`, where the response status is visible.
   */
  test("does not offer to disable the account making the request", async ({ page }) => {
    await openAccounts(page);

    const disableSelf = accountRow(page, "admin").getByRole("button", { name: "Disable admin" });
    await expect(disableSelf).toBeDisabled();
    await expect(disableSelf).toHaveAttribute("title", "You cannot disable your own account");
  });

  /**
   * The whole point of the page: an administrator closes an account to logins
   * and reopens it without leaving the browser. Restored in a `finally` even
   * when an expectation fails, because the `user` project depends on that
   * account existing and enabled.
   */
  test("closes an account to logins and reopens it", async ({ page }) => {
    await openAccounts(page);
    const row = accountRow(page, "user");

    try {
      await row.getByRole("button", { name: "Disable user" }).click();

      await expect(row.getByText("Disabled")).toBeVisible();
      await expect(row.getByRole("button", { name: "Enable user" })).toBeVisible();
      await expect(page.getByRole("alert")).toHaveCount(0);

      // The change is the backend's, not just the rendered row's: a reload shows
      // the same state.
      await page.reload();
      await expect(accountRow(page, "user").getByText("Disabled")).toBeVisible();

      await accountRow(page, "user").getByRole("button", { name: "Enable user" }).click();
      await expect(accountRow(page, "user").getByText("Active")).toBeVisible();
    } finally {
      // Belt and braces: if the UI enable above never ran, put the account back
      // through the API so no other spec inherits a disabled account.
      const restored = await postAdminAction(page, "user", "enable");
      expect(restored.status()).toBe(200);
    }
  });

  /**
   * The disable an administrator actually wants: the account stops acting now
   * rather than when its session happens to expire.
   *
   * Asserted over the API rather than through the page, because the subject is a
   * *second* caller's session. A cookie jar of its own is what makes "the holder
   * is signed out" observable at all — the accounts page has no view of it, and
   * the admin session this project replays must not be the one under test.
   */
  test("ends the session an account already holds", async ({ page }) => {
    const holder = await anonymousApi();

    try {
      const signedIn = await submitLoginViaApi(holder, "user", USER_PASSWORD);
      expect(signedIn.status()).toBe(200);

      // Live *before* the disable. Without this the 401 below would prove
      // nothing: an unauthenticated jar answers 401 too.
      const working = await holder.get("/api/auth/me");
      expect(working.status()).toBe(200);

      const disabled = await postAdminAction(page, "user", "disable");
      expect(disabled.status()).toBe(200);

      // Same jar, same cookie, and the session behind it no longer exists.
      const refused = await holder.get("/api/auth/me");
      expect(refused.status()).toBe(401);
    } finally {
      const restored = await postAdminAction(page, "user", "enable");
      expect(restored.status()).toBe(200);
      await holder.dispose();
    }
  });

  /**
   * Enabling is not the inverse of disabling. A revoked session is gone for good;
   * reopening the account only means it may sign in again, which is what the
   * fresh jar at the end proves.
   */
  test("does not hand a revoked session back when the account is reopened", async ({ page }) => {
    const holder = await anonymousApi();

    try {
      const signedIn = await submitLoginViaApi(holder, "user", USER_PASSWORD);
      expect(signedIn.status()).toBe(200);

      const disabled = await postAdminAction(page, "user", "disable");
      expect(disabled.status()).toBe(200);
      const reopened = await postAdminAction(page, "user", "enable");
      expect(reopened.status()).toBe(200);

      const refused = await holder.get("/api/auth/me");
      expect(refused.status()).toBe(401);

      const fresh = await anonymousApi();
      try {
        const accepted = await submitLoginViaApi(fresh, "user", USER_PASSWORD);
        expect(accepted.status()).toBe(200);
      } finally {
        await fresh.dispose();
      }
    } finally {
      // The account is already enabled unless an expectation above failed first.
      const restored = await postAdminAction(page, "user", "enable");
      expect(restored.status()).toBe(200);
      await holder.dispose();
    }
  });

  /**
   * The lockout as an administrator meets it: nobody imposes it, a run of failed
   * logins does, and the listing is where it becomes visible. Locking a *seeded*
   * account is only safe in this file, for the same reason disabling one is — it
   * is serial, and it puts the account back before anything else reads it.
   *
   * The failed logins go through an anonymous cookie jar rather than this page's,
   * so the admin session the parallel specs share is never in the blast radius.
   */
  test("reports an account that has locked itself out, and unlocks it", async ({ page }) => {
    const api = await anonymousApi();

    try {
      await lockAccount(api, "user");
      await openAccounts(page);
      const row = accountRow(page, "user");

      // The listing reports the lockout the login path imposed — this is the only
      // place an administrator can see it at all, and there is no expiry to show
      // because nothing but Unlock ends it.
      await expect(row.getByText("Locked")).toBeVisible();
      // Still enabled: a lockout is not a standing decision, and the page must
      // not conflate the two refusal mechanisms.
      await expect(row.getByRole("button", { name: "Disable user" })).toBeEnabled();

      const unlock = row.getByRole("button", { name: "Unlock user" });
      await expect(unlock).toBeEnabled();
      await unlock.click();

      await expect(row.getByText("Active")).toBeVisible();
      await expect(row.getByRole("button", { name: "Unlock user" })).toBeDisabled();

      // The unlock was the backend's, and it cleared the failure run with it: the
      // correct password is accepted again, and from a fresh jar so this proves
      // authentication rather than a surviving session.
      const fresh = await anonymousApi();
      try {
        const accepted = await submitLoginViaApi(fresh, "user", USER_PASSWORD);
        expect(accepted.status()).toBe(200);
      } finally {
        await fresh.dispose();
      }
    } finally {
      const restored = await postAdminAction(page, "user", "unlock");
      expect(restored.status()).toBe(200);
      await api.dispose();
    }
  });

  /**
   * Criterion 3, from the holder's side: imposing the lockout ends the sessions
   * the account is already holding, so it stops acting the moment the lock lands
   * rather than when its session happens to expire.
   *
   * Shaped like the disable test above, and for the same reason: the subject is a
   * *second* caller's session, which only a cookie jar of its own makes
   * observable. The refusals that impose the lock are driven from a third jar, so
   * the 401 below cannot be an artefact of the failed logins landing in the jar
   * under test.
   */
  test("ends the session an account held before it locked itself out", async ({ page }) => {
    const holder = await anonymousApi();
    const guesser = await anonymousApi();

    try {
      const signedIn = await submitLoginViaApi(holder, "user", USER_PASSWORD);
      expect(signedIn.status()).toBe(200);

      // Live *before* the lockout. Without this the 401 below would prove
      // nothing: an unauthenticated jar answers 401 too.
      const working = await holder.get("/api/auth/me");
      expect(working.status()).toBe(200);

      await lockAccount(guesser, "user");

      // Same jar, same cookie, and the session behind it no longer exists.
      const refused = await holder.get("/api/auth/me");
      expect(refused.status()).toBe(401);
    } finally {
      const restored = await postAdminAction(page, "user", "unlock");
      expect(restored.status()).toBe(200);
      await guesser.dispose();
      await holder.dispose();
    }
  });

  /**
   * What the lockout is for: while it holds, the account's own password stops
   * working. Asserted over the API because the SPA is told nothing that
   * distinguishes it from a wrong password — that is the point of the bare 401.
   */
  test("keeps refusing the correct password while the lockout holds", async ({ page }) => {
    const api = await anonymousApi();

    try {
      await lockAccount(api, "user");

      // Same jar as the refusals above: a refused login mints no session, so
      // there is nothing here for this attempt to sail in on.
      const refused = await submitLoginViaApi(api, "user", USER_PASSWORD);

      expect(refused.status()).toBe(401);
      expect(await refused.text()).toBe("");
    } finally {
      const restored = await postAdminAction(page, "user", "unlock");
      expect(restored.status()).toBe(200);
      await api.dispose();
    }
  });
});
