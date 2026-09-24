import { afterEach, describe, expect, it, vi } from "vitest";

import { getCurrentUser, login, logout } from "./api";

describe("auth API", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("treats an unauthorized session check as a guest", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 401 })));

    await expect(getCurrentUser()).resolves.toBeNull();
    expect(fetch).toHaveBeenCalledWith("/api/auth/me", { credentials: "include" });
  });

  it("returns the authenticated user", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ username: "ada" }), {
          headers: { "Content-Type": "application/json" },
        }),
      ),
    );

    await expect(getCurrentUser()).resolves.toEqual({ username: "ada" });
  });

  it("rejects a failed session check", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 503 })));

    await expect(getCurrentUser()).rejects.toThrow("Unable to check the current session.");
  });

  it("reports invalid credentials without exposing backend details", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 401 })));

    await expect(login("ada", "wrong")).rejects.toThrow("The username or password is incorrect.");
  });

  it("rejects other login failures", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 500 })));

    await expect(login("ada", "secret")).rejects.toThrow("Unable to sign in. Please try again.");
  });

  it("logs out with the session cookie", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 204 })));

    await logout();
    expect(fetch).toHaveBeenCalledWith("/api/auth/logout", {
      credentials: "include",
      method: "DELETE",
    });
  });

  it("treats an already-expired session as logged out", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 401 })));

    await expect(logout()).resolves.toBeUndefined();
  });

  it("rejects logout failures other than an expired session", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 500 })));
    await expect(logout()).rejects.toThrow("Unable to sign out. Please try again.");
  });
});
