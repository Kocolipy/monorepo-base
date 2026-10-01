import { act, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { AuthContext, type AuthContextState } from "@/auth/auth-context-value";
import { apiFetch } from "@/lib/http";

import { Showcase } from "./showcase";

vi.mock("@/lib/http", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/lib/http")>()),
  apiFetch: vi.fn(),
}));

const apiFetchMock = vi.mocked(apiFetch);
const count = () => screen.getByTestId("count");
const increment = () => screen.getByRole("button", { name: "Increment" });
const reset = () => screen.getByRole("button", { name: "Reset" });

const auth: AuthContextState = {
  changePassword: vi.fn(),
  expireSession: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
  passwordChanged: false,
  sessionExpired: false,
  status: "authenticated",
  user: { passwordChangeRequired: false, role: "USER", username: "ada" },
};

function resolveWith(result: object) {
  apiFetchMock.mockResolvedValue(result as never);
}

function resolveOnceWith(result: object) {
  apiFetchMock.mockResolvedValueOnce(result as never);
}

function renderShowcase(value: AuthContextState = auth) {
  return render(
    <AuthContext.Provider value={value}>
      <MemoryRouter>
        <Showcase />
      </MemoryRouter>
    </AuthContext.Provider>,
  );
}

describe("Showcase", () => {
  beforeEach(() => {
    apiFetchMock.mockReset();
    resolveWith({ kind: "ok", data: 0 });
    vi.mocked(auth.expireSession).mockReset();
    vi.mocked(auth.logout).mockReset();
  });

  it("offers every User the password change, and only an ADMIN the accounts page", async () => {
    const { unmount } = renderShowcase();
    expect(await screen.findByRole("link", { name: "Change password" })).toHaveAttribute(
      "href",
      "/change-password",
    );
    expect(screen.queryByRole("link", { name: "Manage accounts" })).not.toBeInTheDocument();
    unmount();

    renderShowcase({
      ...auth,
      user: { passwordChangeRequired: false, role: "ADMIN", username: "grace" },
    });
    expect(await screen.findByRole("link", { name: "Manage accounts" })).toHaveAttribute(
      "href",
      "/accounts",
    );
    expect(screen.getByRole("link", { name: "Change password" })).toHaveAttribute(
      "href",
      "/change-password",
    );
  });

  it("renders the original home page and signed-in user", async () => {
    renderShowcase();
    expect(screen.getByRole("heading", { name: "Front End" })).toBeInTheDocument();
    expect(screen.getByText("Signed in as ada")).toBeInTheDocument();
    expect(await screen.findByRole("button", { name: "Increment" })).toBeEnabled();
  });

  it("renders safely while authenticated user details are unavailable", async () => {
    renderShowcase({ ...auth, user: null });
    expect(screen.getByText("Signed in as")).toBeInTheDocument();
    expect(await screen.findByRole("button", { name: "Increment" })).toBeEnabled();
  });

  it("loads the current count when the showcase opens", async () => {
    resolveWith({ kind: "ok", data: 3 });
    renderShowcase();

    expect(await screen.findByText("Clicked 3 times")).toBeInTheDocument();
    expect(apiFetchMock).toHaveBeenCalledWith("/api/count", {}, expect.any(Function));
  });

  it("decodes the count from the backend's response body", async () => {
    resolveWith({ kind: "ok", data: 0 });
    renderShowcase();
    await screen.findByText("Clicked 0 times");

    // apiFetch is stubbed above, so the decoder it was handed is exercised here.
    const decode = apiFetchMock.mock.calls[0]?.[2];
    await expect(decode?.(Response.json({ count: 7 }))).resolves.toBe(7);
  });

  it.each([
    ["a missing count", {}, "CountResponse.count is not an integer"],
    ["a string count", { count: "7" }, "CountResponse.count is not an integer"],
    ["a fractional count", { count: 1.5 }, "CountResponse.count is not an integer"],
    ["a non-object body", [7], "CountResponse is not an object"],
  ])("refuses a counter body with %s", async (_, body, message) => {
    renderShowcase();
    await screen.findByText("Clicked 0 times");

    const decode = apiFetchMock.mock.calls[0]?.[2];
    await expect(decode?.(Response.json(body))).rejects.toThrow(
      expect.objectContaining({ name: "DecodeError", message }),
    );
  });

  it("disables counter actions while the initial count is loading", async () => {
    let finishLoading: ((result: object) => void) | undefined;
    apiFetchMock.mockReturnValueOnce(
      new Promise((resolve) => {
        finishLoading = resolve;
      }) as never,
    );
    renderShowcase();

    expect(increment()).toBeDisabled();
    expect(reset()).toBeDisabled();

    finishLoading?.({ kind: "ok", data: 2 });
    expect(await screen.findByText("Clicked 2 times")).toBeInTheDocument();
    expect(increment()).toBeEnabled();
    expect(reset()).toBeEnabled();
  });

  it("reports a failed initial counter read", async () => {
    resolveWith({ kind: "failed", status: 503 });
    renderShowcase();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Unable to load the counter. Please try again.",
    );
    expect(increment()).toBeEnabled();
  });

  it("reports an expired CSRF token with security-specific copy", async () => {
    resolveWith({ kind: "csrf-expired" });
    renderShowcase();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Your security token expired. Please try again.",
    );
  });

  it("reports a refused counter update as permission denied and keeps the session", async () => {
    resolveOnceWith({ kind: "ok", data: 2 });
    resolveOnceWith({ kind: "forbidden" });
    const user = userEvent.setup();
    renderShowcase();

    await user.click(await screen.findByRole("button", { name: "Increment" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      /^You don't have permission to do this\.$/,
    );
    // The count from the last successful read stands, and nothing ends the session.
    expect(count()).toHaveTextContent("Clicked 2 times");
    expect(auth.expireSession).not.toHaveBeenCalled();
    expect(auth.logout).not.toHaveBeenCalled();
  });

  it("expires the auth state when the initial request is unauthenticated", async () => {
    let finishLoading: ((result: object) => void) | undefined;
    apiFetchMock.mockReturnValueOnce(
      new Promise((resolve) => {
        finishLoading = resolve;
      }) as never,
    );
    renderShowcase();

    await act(async () => {
      finishLoading?.({ kind: "unauthenticated" });
    });

    // The page never sees this case: the session seam ends the session, and the
    // route guard replaces this page with the login route on the same update.
    expect(auth.expireSession).toHaveBeenCalledOnce();
    expect(count()).toHaveTextContent(/^Clicked 0 times$/);
  });

  it("counts each click", async () => {
    resolveOnceWith({ kind: "ok", data: 0 });
    resolveOnceWith({ kind: "ok", data: 1 });
    resolveOnceWith({ kind: "ok", data: 2 });
    const user = userEvent.setup();
    renderShowcase();

    await user.click(increment());
    await user.click(increment());

    expect(count()).toHaveTextContent(/^Clicked 2 times$/);
    expect(apiFetchMock).toHaveBeenLastCalledWith(
      "/api/count/increment",
      { method: "POST" },
      expect.any(Function),
    );
  });

  it("uses the singular label at exactly one", async () => {
    resolveOnceWith({ kind: "ok", data: 0 });
    resolveOnceWith({ kind: "ok", data: 1 });
    const user = userEvent.setup();
    renderShowcase();

    await user.click(increment());
    expect(count()).toHaveTextContent(/^Clicked 1 time$/);
  });

  it("disables Reset until there is something to reset", async () => {
    resolveOnceWith({ kind: "ok", data: 0 });
    resolveOnceWith({ kind: "ok", data: 1 });
    const user = userEvent.setup();
    renderShowcase();

    expect(reset()).toBeDisabled();
    await user.click(increment());
    expect(reset()).toBeEnabled();
  });

  it("disables counter actions while an update is pending", async () => {
    let finishIncrement: ((result: object) => void) | undefined;
    resolveOnceWith({ kind: "ok", data: 0 });
    apiFetchMock.mockReturnValueOnce(
      new Promise((resolve) => {
        finishIncrement = resolve;
      }) as never,
    );
    const user = userEvent.setup();
    renderShowcase();

    await user.click(increment());
    expect(increment()).toBeDisabled();
    expect(reset()).toBeDisabled();

    finishIncrement?.({ kind: "ok", data: 1 });
    expect(await screen.findByText("Clicked 1 time")).toBeInTheDocument();
    expect(increment()).toBeEnabled();
    expect(reset()).toBeEnabled();
  });

  it("returns the count to zero on reset", async () => {
    resolveOnceWith({ kind: "ok", data: 0 });
    resolveOnceWith({ kind: "ok", data: 1 });
    resolveOnceWith({ kind: "ok", data: 0 });
    const user = userEvent.setup();
    renderShowcase();

    await user.click(increment());
    await user.click(reset());

    expect(count()).toHaveTextContent(/^Clicked 0 times$/);
    expect(reset()).toBeDisabled();
    expect(apiFetchMock).toHaveBeenLastCalledWith(
      "/api/count/reset",
      { method: "POST" },
      expect.any(Function),
    );
  });

  it("keeps the count and reports backend failures", async () => {
    resolveOnceWith({ kind: "ok", data: 0 });
    resolveOnceWith({ kind: "failed", status: 503 });
    const user = userEvent.setup();
    renderShowcase();

    await user.click(increment());

    expect(count()).toHaveTextContent(/^Clicked 0 times$/);
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Unable to update the counter. Please try again.",
    );
  });

  it("expires the auth state when an update is unauthenticated", async () => {
    resolveOnceWith({ kind: "ok", data: 0 });
    resolveOnceWith({ kind: "unauthenticated" });
    const user = userEvent.setup();
    renderShowcase();

    await user.click(increment());

    expect(auth.expireSession).toHaveBeenCalledOnce();
    expect(count()).toHaveTextContent(/^Clicked 0 times$/);
  });

  it("signs out", async () => {
    const user = userEvent.setup();
    renderShowcase();
    await user.click(screen.getByRole("button", { name: "Sign out" }));
    expect(auth.logout).toHaveBeenCalledOnce();
  });
});
