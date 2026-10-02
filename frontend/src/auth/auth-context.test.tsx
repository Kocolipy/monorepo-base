import { act, renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import * as http from "@/lib/http";

import * as authApi from "./api";
import { AuthProvider } from "./auth-context";
import { useAuthState } from "./auth-context-value";

vi.mock("./api");
vi.mock("@/lib/http");

const api = vi.mocked(authApi);

const CONFINED: authApi.AuthUser = {
  idleTimeoutSeconds: 900,
  passwordChangeRequired: true,
  role: null,
  username: "ada",
};
const USER: authApi.AuthUser = {
  idleTimeoutSeconds: 900,
  passwordChangeRequired: false,
  role: "USER",
  username: "ada",
};
const CREDENTIALS = ["ada", "chosen-1"] as const;

const wrapper = ({ children }: { children: ReactNode }) => <AuthProvider>{children}</AuthProvider>;

/** Mounts the provider and waits for its session check to settle. */
async function mounted() {
  const hook = renderHook(() => useAuthState(), { wrapper });
  await waitFor(() => expect(hook.result.current.status).not.toBe("checking"));
  return hook;
}

/** The provenance the route guards read, plus the status. */
const provenance = (state: ReturnType<typeof useAuthState>) => ({
  passwordChanged: state.passwordChanged,
  sessionExpired: state.sessionExpired,
  status: state.status,
});

describe("AuthProvider", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it("starts as a cold guest when there is no session", async () => {
    api.getCurrentUser.mockResolvedValue(null);
    const { result } = await mounted();

    expect(provenance(result.current)).toEqual({
      passwordChanged: false,
      sessionExpired: false,
      status: "guest",
    });
    expect(result.current.user).toBeNull();
  });

  it("treats a failed session check as a cold guest", async () => {
    api.getCurrentUser.mockRejectedValue(new Error("offline"));
    const { result } = await mounted();

    expect(provenance(result.current)).toEqual({
      passwordChanged: false,
      sessionExpired: false,
      status: "guest",
    });
  });

  it("holds a flagged session's user as reported", async () => {
    api.getCurrentUser.mockResolvedValue(CONFINED);
    const { result } = await mounted();

    expect(result.current.status).toBe("authenticated");
    expect(result.current.user).toEqual(CONFINED);
  });

  it("ends the session on a successful change and records why", async () => {
    api.getCurrentUser.mockResolvedValue(CONFINED);
    api.changePassword.mockResolvedValue({ kind: "changed" });
    const { result } = await mounted();

    let outcome: authApi.PasswordChangeOutcome | undefined;
    await act(async () => {
      outcome = await result.current.changePassword("old-value", "new-value");
    });

    expect(outcome).toEqual({ kind: "changed" });
    expect(api.changePassword).toHaveBeenCalledWith("old-value", "new-value");
    expect(provenance(result.current)).toEqual({
      passwordChanged: true,
      sessionExpired: false,
      status: "guest",
    });
    expect(result.current.user).toBeNull();
  });

  it.each([
    { kind: "current-password-rejected" },
    { kind: "locked" },
    { kind: "policy-violation", message: "rule" },
    { kind: "forbidden" },
    { kind: "csrf-expired" },
    { kind: "failed" },
  ] as authApi.PasswordChangeOutcome[])("keeps the session on a $kind refusal", async (refusal) => {
    api.getCurrentUser.mockResolvedValue(CONFINED);
    api.changePassword.mockResolvedValue(refusal);
    const { result } = await mounted();

    let outcome: authApi.PasswordChangeOutcome | undefined;
    await act(async () => {
      outcome = await result.current.changePassword("old-value", "new-value");
    });

    expect(outcome).toEqual(refusal);
    expect(result.current.status).toBe("authenticated");
    expect(result.current.user).toEqual(CONFINED);
    expect(result.current.passwordChanged).toBe(false);
  });

  it("clears the change provenance on the next login", async () => {
    api.getCurrentUser.mockResolvedValue(CONFINED);
    api.changePassword.mockResolvedValue({ kind: "changed" });
    api.login.mockResolvedValue(USER);
    const { result } = await mounted();

    await act(async () => {
      await result.current.changePassword("old-value", "new-value");
    });
    await act(async () => {
      await result.current.login(...CREDENTIALS);
    });

    expect(api.login).toHaveBeenCalledWith(...CREDENTIALS);
    expect(provenance(result.current)).toEqual({
      passwordChanged: false,
      sessionExpired: false,
      status: "authenticated",
    });
    expect(result.current.user).toEqual(USER);
  });

  it("forgets the CSRF token of a session the backend has ended", async () => {
    api.getCurrentUser.mockResolvedValue(USER);
    const { result } = await mounted();
    expect(http.discardCsrfToken).not.toHaveBeenCalled();

    act(() => result.current.expireSession());

    expect(http.discardCsrfToken).toHaveBeenCalledOnce();
  });

  it("clears the expiry provenance on the next login", async () => {
    api.getCurrentUser.mockResolvedValue(USER);
    api.login.mockResolvedValue(USER);
    const { result } = await mounted();

    act(() => result.current.expireSession());
    expect(provenance(result.current)).toEqual({
      passwordChanged: false,
      sessionExpired: true,
      status: "guest",
    });

    await act(async () => {
      await result.current.login(...CREDENTIALS);
    });
    expect(result.current.sessionExpired).toBe(false);
  });

  it("an expiry after a change says the session expired, not that it changed", async () => {
    api.getCurrentUser.mockResolvedValue(CONFINED);
    api.changePassword.mockResolvedValue({ kind: "changed" });
    const { result } = await mounted();

    await act(async () => {
      await result.current.changePassword("old-value", "new-value");
    });
    act(() => result.current.expireSession());

    expect(provenance(result.current)).toEqual({
      passwordChanged: false,
      sessionExpired: true,
      status: "guest",
    });
  });

  it("logs out to a cold guest, clearing both provenances", async () => {
    api.getCurrentUser.mockResolvedValue(CONFINED);
    api.changePassword.mockResolvedValue({ kind: "changed" });
    api.login.mockResolvedValue(USER);
    api.logout.mockResolvedValue(undefined);
    const { result } = await mounted();

    await act(async () => {
      await result.current.changePassword("old-value", "new-value");
    });
    act(() => result.current.expireSession());
    await act(async () => {
      await result.current.login(...CREDENTIALS);
    });
    await act(async () => {
      await result.current.changePassword("old-value", "new-value");
    });
    await act(async () => {
      await result.current.logout();
    });

    expect(api.logout).toHaveBeenCalledTimes(1);
    expect(provenance(result.current)).toEqual({
      passwordChanged: false,
      sessionExpired: false,
      status: "guest",
    });
    expect(result.current.user).toBeNull();
  });

  it("signs an idle session out: logs out, forgets the token and records why", async () => {
    api.getCurrentUser.mockResolvedValue(USER);
    api.logout.mockResolvedValue(undefined);
    const { result } = await mounted();

    await act(async () => {
      await result.current.signOutForInactivity();
    });

    expect(api.logout).toHaveBeenCalledOnce();
    expect(http.discardCsrfToken).toHaveBeenCalledOnce();
    expect(idleProvenance(result.current)).toEqual({
      passwordChanged: false,
      sessionExpired: false,
      signedOutForInactivity: true,
      status: "guest",
    });
    expect(result.current.user).toBeNull();
  });

  it("clears an idle session even when its logout fails", async () => {
    api.getCurrentUser.mockResolvedValue(USER);
    api.logout.mockRejectedValue(new Error("Unable to sign out. Please try again."));
    const { result } = await mounted();

    await act(async () => {
      await result.current.signOutForInactivity();
    });

    expect(http.discardCsrfToken).toHaveBeenCalledOnce();
    expect(idleProvenance(result.current)).toMatchObject({
      signedOutForInactivity: true,
      status: "guest",
    });
    expect(result.current.user).toBeNull();
  });

  it("clears the inactivity provenance on the next login, and on a later expiry", async () => {
    api.getCurrentUser.mockResolvedValue(USER);
    api.logout.mockResolvedValue(undefined);
    api.login.mockResolvedValue(USER);
    const { result } = await mounted();

    await act(async () => {
      await result.current.signOutForInactivity();
    });
    await act(async () => {
      await result.current.login(...CREDENTIALS);
    });
    expect(result.current.signedOutForInactivity).toBe(false);

    await act(async () => {
      await result.current.signOutForInactivity();
    });
    act(() => result.current.expireSession());
    expect(idleProvenance(result.current)).toEqual({
      passwordChanged: false,
      sessionExpired: true,
      signedOutForInactivity: false,
      status: "guest",
    });
  });
});

const idleProvenance = (state: ReturnType<typeof useAuthState>) => ({
  ...provenance(state),
  signedOutForInactivity: state.signedOutForInactivity,
});
