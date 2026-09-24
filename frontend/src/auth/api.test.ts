import { beforeEach, describe, expect, it, vi } from "vitest";

import { apiFetch } from "@/lib/http";

import { getCurrentUser, login, logout } from "./api";

vi.mock("@/lib/http");

const apiFetchMock = vi.mocked(apiFetch);
const TEST_LOGIN = ["ada", "secret"] as const;

function resolveWith(result: object) {
  apiFetchMock.mockResolvedValue(result as never);
}

describe("auth API", () => {
  beforeEach(() => {
    apiFetchMock.mockReset();
  });

  it("treats an unauthenticated session check as a guest", async () => {
    resolveWith({ kind: "unauthenticated" });

    await expect(getCurrentUser()).resolves.toBeNull();
    expect(apiFetchMock).toHaveBeenCalledWith("/api/auth/me", {}, expect.any(Function));
  });

  it("returns the authenticated user", async () => {
    resolveWith({ kind: "ok", data: { role: "USER", username: "ada" } });

    await expect(getCurrentUser()).resolves.toEqual({ role: "USER", username: "ada" });
  });

  it.each([{ kind: "csrf-expired" }, { kind: "failed", status: 503 }])(
    "rejects a failed session check for $kind",
    async (result) => {
      resolveWith(result);
      await expect(getCurrentUser()).rejects.toThrow("Unable to check the current session.");
    },
  );

  it("requests typed user data when signing in", async () => {
    const [username, password] = TEST_LOGIN;
    resolveWith({ kind: "ok", data: { role: "USER", username } });

    await expect(login(username, password)).resolves.toEqual({ role: "USER", username });
    expect(apiFetchMock).toHaveBeenCalledWith(
      "/api/auth/login",
      {
        body: JSON.stringify({ username, password }),
        headers: { "Content-Type": "application/json" },
        method: "POST",
      },
      expect.any(Function),
    );
  });

  it("reports invalid credentials without exposing backend details", async () => {
    resolveWith({ kind: "unauthenticated" });
    await expect(login("ada", "wrong")).rejects.toThrow("The username or password is incorrect.");
  });

  it("reports a persistent CSRF rejection as a token problem", async () => {
    resolveWith({ kind: "csrf-expired" });
    await expect(login("ada", "secret")).rejects.toThrow(
      "Your security token expired. Please try again.",
    );
  });

  it("reports other login failures", async () => {
    resolveWith({ kind: "failed", status: 500 });
    await expect(login("ada", "secret")).rejects.toThrow("Unable to sign in. Please try again.");
  });

  it("logs out without decoding a response body", async () => {
    resolveWith({ kind: "ok", data: undefined });

    await expect(logout()).resolves.toBeUndefined();
    expect(apiFetchMock).toHaveBeenCalledWith("/api/auth/logout", { method: "DELETE" });
  });

  it("treats an already-expired session as logged out", async () => {
    resolveWith({ kind: "unauthenticated" });
    await expect(logout()).resolves.toBeUndefined();
  });

  it("reports a persistent CSRF rejection on logout", async () => {
    resolveWith({ kind: "csrf-expired" });
    await expect(logout()).rejects.toThrow("Your security token expired. Please try again.");
  });

  it("reports other logout failures", async () => {
    resolveWith({ kind: "failed", status: 500 });
    await expect(logout()).rejects.toThrow("Unable to sign out. Please try again.");
  });
});
