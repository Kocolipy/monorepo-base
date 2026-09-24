import { expect, test } from "@playwright/test";

test.describe("ADMIN route guards", () => {
  test("may view the accounts page", async ({ page }) => {
    await page.goto("/accounts");

    await expect(page.getByRole("heading", { name: "Accounts" })).toBeVisible();
    await expect(page.getByText("Account management is coming soon.")).toBeVisible();
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
      "role",
      "username",
    ]);
    expect(admin!.username).toEqual(expect.any(String));
    expect(admin!.email).toContain("@");
    expect(admin!.enabled).toBe(true);
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
