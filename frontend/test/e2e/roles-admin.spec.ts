import { expect, test } from "@playwright/test";

import { postAdminAction } from "./auth.helpers";

test.describe("ADMIN route guards", () => {
  test("may view the accounts page", async ({ page }) => {
    await page.goto("/accounts");

    await expect(page.getByRole("heading", { name: "Accounts" })).toBeVisible();
    await expect(page.getByRole("rowheader", { exact: true, name: "admin" })).toBeVisible();
    await expect(page).toHaveURL(/\/accounts$/);
  });
});

test.describe("ADMIN user listing", () => {
  // `page.request` shares the context's cookie jar, so the replayed ADMIN
  // session authenticates this call. No CSRF header is needed: the token is
  // only demanded of unsafe methods.
  test("lists every registered account", async ({ page }) => {
    const response = await page.request.get("/api/admin/users");

    expect(response.status()).toBe(200);

    const users = (await response.json()) as Array<Record<string, unknown>>;
    expect(users.length).toBeGreaterThanOrEqual(2);

    // The seeded identities, by role rather than by name, so renaming either
    // seed account in configuration does not fail this.
    expect(users.map((user) => user.role)).toContain("USER");
    expect(users.map((user) => user.role)).toContain("ADMIN");
  });

  test("reports each account's username, email, role, status, and creation date", async ({
    page,
  }) => {
    const response = await page.request.get("/api/admin/users");
    const users = (await response.json()) as Array<Record<string, unknown>>;

    const admin = users.find((user) => user.role === "ADMIN");
    expect(admin, "the seeded ADMIN account should be listed").toBeTruthy();

    expect(Object.keys(admin!).sort()).toEqual([
      "createdAt",
      "email",
      "enabled",
      "locked",
      "lockedUntil",
      "role",
      "username",
    ]);
    expect(admin!.username).toEqual(expect.any(String));
    expect(admin!.email).toContain("@");
    expect(admin!.enabled).toBe(true);
    expect(admin!.locked).toBe(false);
    // Parseable as a date rather than a fixed value: the timestamp is whenever
    // this environment first seeded the account.
    expect(Number.isNaN(Date.parse(String(admin!.createdAt)))).toBe(false);
  });

  /**
   * The acceptance criterion the whole endpoint exists to respect. Asserted on
   * the raw body, not on parsed fields: a hash nested inside an unexpected
   * object would still be caught.
   */
  test("never returns password hashes", async ({ page }) => {
    const response = await page.request.get("/api/admin/users");
    const body = await response.text();

    expect(body).not.toContain("password");
    expect(body).not.toContain("$2a$");
  });
});

test.describe("ADMIN account control", () => {
  /**
   * The disable/enable round trip is asserted through the page in
   * `accounts-admin.spec.ts`, which reaches the same endpoints through the SPA.
   * It lives in exactly one spec on purpose: the seeded `user` identity is
   * shared with the `user` project running beside this one, and two specs
   * toggling it in parallel would race. The endpoint's own contract — status
   * codes, the refusals, the response shape — is covered in
   * `AdminUserEndpointTests`.
   */

  /**
   * Unlocking an account that is serving no lockout is the safe case to assert
   * live: it is idempotent, writes nothing, and cannot disturb a parallel spec.
   * That enabling does not lift a lockout, and unlocking does not enable, is
   * asserted in `AccountAdministrationServiceTests`, where the clock can be moved
   * and a locked account can be set up without three real failed logins racing
   * every other spec's session.
   */
  test("reports an account as unlocked when it is serving no lockout", async ({ page }) => {
    const response = await postAdminAction(page, "user", "unlock");

    expect(response.status()).toBe(200);
    expect(await response.json()).toMatchObject({ locked: false, lockedUntil: null });
  });

  /**
   * Asserted as self-disable rather than as the last-administrator guard: this
   * project's session IS the `admin` account, so the self check answers first and
   * the result does not depend on how many administrators the environment
   * happens to have seeded. The last-administrator refusal is covered in
   * `AdminUserEndpointTests`, where exactly two accounts exist and disabling the
   * only admin can be provoked deterministically.
   */
  test("refuses to disable the account making the request", async ({ page }) => {
    const response = await postAdminAction(page, "admin", "disable");

    // 409, not 403: the caller is an administrator, the action is what is
    // refused. A 403 here would mean the role check turned it away instead.
    expect(response.status()).toBe(409);

    const listing = (await (await page.request.get("/api/admin/users")).json()) as Array<
      Record<string, unknown>
    >;
    expect(listing.find((account) => account.username === "admin")).toMatchObject({
      enabled: true,
    });
  });

  test("answers a request for an account that does not exist with 404", async ({ page }) => {
    const response = await postAdminAction(page, "nobody", "disable");

    expect(response.status()).toBe(404);
  });
});
