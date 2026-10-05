import { act, fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";

import { AuthContext, type AuthContextState } from "@/auth/auth-context-value";
import type { SessionRouteState } from "@/auth/session-route";

import { Login } from "./login";

const INACTIVE = "You were signed out because you were inactive. Please sign in again.";
const EXPIRED = "Your session ended. Please sign in again.";
const CHANGED = "Your password was changed. Sign in with your new password.";

function renderLogin(login: AuthContextState["login"], state?: SessionRouteState) {
  const value: AuthContextState = {
    changePassword: vi.fn(),
    expireSession: vi.fn(),
    login,
    logout: vi.fn(),
    passwordChanged: false,
    sessionExpired: false,
    signOutForInactivity: vi.fn(),
    signedOutForInactivity: false,
    status: "guest",
    user: null,
  };
  render(
    <MemoryRouter initialEntries={[{ pathname: "/", state }]}>
      <AuthContext.Provider value={value}>
        <Login />
      </AuthContext.Provider>
    </MemoryRouter>,
  );
}

const submit = () =>
  act(async () => {
    fireEvent.change(screen.getByLabelText("Username"), { target: { value: "ada" } });
    fireEvent.change(screen.getByLabelText("Password"), { target: { value: "secret-value" } });
    fireEvent.submit(screen.getByRole("button", { name: /Sign/ }));
  });

describe("Login", () => {
  it.each([
    ["an inactivity sign-out", { from: "/accounts", inactive: true }, INACTIVE],
    ["an expiry", { expired: true, from: "/accounts" }, EXPIRED],
    ["a password change", { passwordChanged: true }, CHANGED],
    // Provenance is exclusive in practice; the precedence still has to be stated.
    ["a change over inactivity", { inactive: true, passwordChanged: true }, CHANGED],
    ["inactivity over an expiry", { expired: true, inactive: true }, INACTIVE],
  ] as [string, SessionRouteState, string][])("says why for %s", (_, state, message) => {
    renderLogin(vi.fn(), state);

    // The whole status, not a substring: exact text rather than an anchored
    // RegExp built from `message`.
    expect(screen.getByRole("status").textContent).toBe(message);
  });

  it("says nothing on a cold visit", () => {
    renderLogin(vi.fn());

    expect(screen.queryByRole("status")).not.toBeInTheDocument();
  });

  it("shows the submission in progress, then lets the form be used again", async () => {
    let finish: () => void = () => undefined;
    renderLogin(
      vi.fn(
        () =>
          new Promise<void>((resolve) => {
            finish = resolve;
          }),
      ),
    );

    await submit();
    expect(screen.getByRole("button", { name: "Signing in…" })).toBeDisabled();

    await act(async () => finish());
    expect(screen.getByRole("button", { name: "Sign in" })).toBeEnabled();
  });

  it("shows the refusal's own message, and clears it on the next attempt", async () => {
    const login = vi
      .fn<AuthContextState["login"]>()
      .mockRejectedValueOnce(new Error("The username or password is incorrect."))
      .mockReturnValueOnce(new Promise<void>(() => undefined));
    renderLogin(login, { expired: true });

    await submit();
    expect(screen.getByRole("alert")).toHaveTextContent(
      /^The username or password is incorrect\.$/,
    );
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Sign in" })).toBeEnabled();

    await submit();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(screen.getByRole("status")).toHaveTextContent(new RegExp(`^${EXPIRED}$`));
  });

  it("falls back to a generic message for a refusal that is not an Error", async () => {
    renderLogin(vi.fn().mockRejectedValue("offline"));

    await submit();

    expect(screen.getByRole("alert")).toHaveTextContent(/^Unable to sign in\. Please try again\.$/);
  });
});
