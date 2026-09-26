import { act, render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { AuthContext, type AuthContextState } from "@/auth/auth-context-value";
import { apiFetch } from "@/lib/http";

import { Accounts, type AdminAccount } from "./accounts";

vi.mock("@/lib/http", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/lib/http")>()),
  apiFetch: vi.fn(),
}));

const apiFetchMock = vi.mocked(apiFetch);

const auth: AuthContextState = {
  expireSession: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
  sessionExpired: false,
  status: "authenticated",
  user: { role: "ADMIN", username: "ada" },
};

const account = (overrides: Partial<AdminAccount> = {}): AdminAccount => ({
  createdAt: "2026-01-02T03:04:05Z",
  enabled: true,
  locked: false,
  role: "USER",
  username: "grace",
  ...overrides,
});

function resolveOnceWith(result: object) {
  apiFetchMock.mockResolvedValueOnce(result as never);
}

function renderAccounts(value: AuthContextState = auth) {
  return render(
    <AuthContext.Provider value={value}>
      <MemoryRouter>
        <Accounts />
      </MemoryRouter>
    </AuthContext.Provider>,
  );
}

/** The row for a named account, addressed by its row header. */
const row = (username: string) =>
  within(screen.getByRole("rowheader", { name: username }).closest("tr")!);

describe("Accounts", () => {
  beforeEach(() => {
    apiFetchMock.mockReset();
    vi.mocked(auth.expireSession).mockReset();
    vi.mocked(auth.logout).mockReset();
  });

  it("lists every account with its role, status, and creation date", async () => {
    resolveOnceWith({ kind: "ok", data: [account(), account({ role: "ADMIN", username: "ada" })] });
    renderAccounts();

    expect(await screen.findByRole("rowheader", { name: "grace" })).toBeInTheDocument();
    const grace = row("grace");
    expect(grace.getByText("USER")).toBeInTheDocument();
    expect(grace.getByText("Active")).toBeInTheDocument();
    expect(grace.getByText("2026-01-02")).toBeInTheDocument();

    expect(screen.getByRole("rowheader", { name: "ada" })).toBeInTheDocument();
    expect(apiFetchMock).toHaveBeenCalledWith("/api/admin/accounts", {}, expect.any(Function));
  });

  it("renders a row written before the creation column existed", async () => {
    resolveOnceWith({ kind: "ok", data: [account({ createdAt: null })] });
    renderAccounts();

    expect(await screen.findByRole("rowheader", { name: "grace" })).toBeInTheDocument();
    expect(row("grace").getAllByText("—")).toHaveLength(1);
  });

  /**
   * The acceptance criterion the status column exists for: an account can be
   * both closed to logins and serving a lockout, and reporting only one of them
   * would hide the other from the administrator who has to clear it.
   */
  it("reports being disabled and being locked as separate states", async () => {
    resolveOnceWith({
      kind: "ok",
      data: [
        account({ enabled: false, username: "closed" }),
        account({ locked: true, username: "penalised" }),
        account({ enabled: false, locked: true, username: "both" }),
      ],
    });
    renderAccounts();

    expect(await screen.findByRole("rowheader", { name: "closed" })).toBeInTheDocument();
    expect(row("closed").getByText("Disabled")).toBeInTheDocument();
    expect(row("closed").queryByText("Locked")).not.toBeInTheDocument();

    expect(row("penalised").getByText("Locked")).toBeInTheDocument();
    expect(row("penalised").queryByText("Disabled")).not.toBeInTheDocument();

    expect(row("both").getByText("Disabled")).toBeInTheDocument();
    expect(row("both").getByText("Locked")).toBeInTheDocument();
  });

  it("closes an account to logins and offers to reopen it", async () => {
    resolveOnceWith({ kind: "ok", data: [account()] });
    resolveOnceWith({ kind: "ok", data: account({ enabled: false }) });
    const user = userEvent.setup();
    renderAccounts();

    await user.click(await screen.findByRole("button", { name: "Disable grace" }));

    expect(apiFetchMock).toHaveBeenLastCalledWith(
      "/api/admin/accounts/grace/disable",
      { method: "POST" },
      expect.any(Function),
    );
    expect(row("grace").getByText("Disabled")).toBeInTheDocument();
    expect(row("grace").getByRole("button", { name: "Enable grace" })).toBeEnabled();
    expect(row("grace").queryByRole("button", { name: "Disable grace" })).not.toBeInTheDocument();
  });

  it("reopens a disabled account", async () => {
    resolveOnceWith({ kind: "ok", data: [account({ enabled: false })] });
    resolveOnceWith({ kind: "ok", data: account() });
    const user = userEvent.setup();
    renderAccounts();

    await user.click(await screen.findByRole("button", { name: "Enable grace" }));

    expect(apiFetchMock).toHaveBeenLastCalledWith(
      "/api/admin/accounts/grace/enable",
      { method: "POST" },
      expect.any(Function),
    );
    expect(row("grace").getByText("Active")).toBeInTheDocument();
  });

  /**
   * Enabling does not lift a lockout, so a reopened account that is still
   * serving one keeps its Unlock control — otherwise the administrator would
   * believe access was restored when it was not.
   */
  it("keeps a standing lockout after the account is reopened", async () => {
    resolveOnceWith({
      kind: "ok",
      data: [account({ enabled: false, locked: true })],
    });
    resolveOnceWith({
      kind: "ok",
      data: account({ locked: true }),
    });
    const user = userEvent.setup();
    renderAccounts();

    await user.click(await screen.findByRole("button", { name: "Enable grace" }));

    expect(row("grace").queryByText("Disabled")).not.toBeInTheDocument();
    expect(row("grace").getByText("Locked")).toBeInTheDocument();
    expect(row("grace").getByRole("button", { name: "Unlock grace" })).toBeEnabled();
  });

  it("ends a lockout early", async () => {
    resolveOnceWith({
      kind: "ok",
      data: [account({ locked: true })],
    });
    resolveOnceWith({ kind: "ok", data: account() });
    const user = userEvent.setup();
    renderAccounts();

    await user.click(await screen.findByRole("button", { name: "Unlock grace" }));

    expect(apiFetchMock).toHaveBeenLastCalledWith(
      "/api/admin/accounts/grace/unlock",
      { method: "POST" },
      expect.any(Function),
    );
    expect(row("grace").getByText("Active")).toBeInTheDocument();
    expect(row("grace").getByRole("button", { name: "Unlock grace" })).toBeDisabled();
  });

  it("offers no unlock for an account serving no lockout", async () => {
    resolveOnceWith({ kind: "ok", data: [account()] });
    renderAccounts();

    expect(await screen.findByRole("button", { name: "Unlock grace" })).toBeDisabled();
  });

  it("refuses to disable the signed-in administrator's own account", async () => {
    resolveOnceWith({ kind: "ok", data: [account({ role: "ADMIN", username: "ada" })] });
    renderAccounts();

    const button = await screen.findByRole("button", { name: "Disable ada" });
    expect(button).toBeDisabled();
    expect(button).toHaveAttribute("title", "You cannot disable your own account");
  });

  it("disables every control while an action is in flight", async () => {
    let finishAction: ((result: object) => void) | undefined;
    resolveOnceWith({ kind: "ok", data: [account(), account({ username: "hopper" })] });
    apiFetchMock.mockReturnValueOnce(
      new Promise((resolve) => {
        finishAction = resolve;
      }) as never,
    );
    const user = userEvent.setup();
    renderAccounts();

    await user.click(await screen.findByRole("button", { name: "Disable grace" }));

    expect(screen.getByRole("button", { name: "Disable grace" })).toBeDisabled();
    // The other row too: two actions in flight could each answer with a row
    // built from a listing the other one has already changed.
    expect(screen.getByRole("button", { name: "Disable hopper" })).toBeDisabled();

    await act(async () => {
      finishAction?.({ kind: "ok", data: account({ enabled: false }) });
    });
    expect(screen.getByRole("button", { name: "Disable hopper" })).toBeEnabled();
  });

  it("reports a refused change as a refusal about the action", async () => {
    resolveOnceWith({ kind: "ok", data: [account()] });
    resolveOnceWith({ kind: "failed", status: 409 });
    const user = userEvent.setup();
    renderAccounts();

    await user.click(await screen.findByRole("button", { name: "Disable grace" }));

    expect(screen.getByRole("alert")).toHaveTextContent(
      "Refused: disabling grace would leave nobody able to restore access.",
    );
    // The listing is untouched: the backend refused, so nothing changed.
    expect(row("grace").getByText("Active")).toBeInTheDocument();
  });

  it("reports an account that has since been removed", async () => {
    resolveOnceWith({ kind: "ok", data: [account()] });
    resolveOnceWith({ kind: "failed", status: 404 });
    const user = userEvent.setup();
    renderAccounts();

    await user.click(await screen.findByRole("button", { name: "Disable grace" }));

    expect(screen.getByRole("alert")).toHaveTextContent(
      "grace no longer exists. Reload the page for the current list.",
    );
  });

  it("reports a failed action with action-specific copy", async () => {
    resolveOnceWith({
      kind: "ok",
      data: [account({ locked: true })],
    });
    resolveOnceWith({ kind: "failed", status: 503 });
    const user = userEvent.setup();
    renderAccounts();

    await user.click(await screen.findByRole("button", { name: "Unlock grace" }));

    expect(screen.getByRole("alert")).toHaveTextContent(
      "Unable to unlock grace. Please try again.",
    );
  });

  it("reports an expired security token with transport-specific copy", async () => {
    resolveOnceWith({ kind: "ok", data: [account()] });
    resolveOnceWith({ kind: "csrf-expired" });
    const user = userEvent.setup();
    renderAccounts();

    await user.click(await screen.findByRole("button", { name: "Disable grace" }));

    expect(screen.getByRole("alert")).toHaveTextContent(
      "Your security token expired. Please try again.",
    );
  });

  it("reports a failed listing read", async () => {
    resolveOnceWith({ kind: "failed", status: 503 });
    renderAccounts();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Unable to load the accounts. Please try again.",
    );
    expect(screen.queryByText("No accounts are registered.")).not.toBeInTheDocument();
  });

  it("says so when no account is registered", async () => {
    resolveOnceWith({ kind: "ok", data: [] });
    renderAccounts();

    expect(await screen.findByText("No accounts are registered.")).toBeInTheDocument();
  });

  it("expires the auth state when the listing read is unauthenticated", async () => {
    let finishLoading: ((result: object) => void) | undefined;
    apiFetchMock.mockReturnValueOnce(
      new Promise((resolve) => {
        finishLoading = resolve;
      }) as never,
    );
    renderAccounts();

    expect(screen.getByText("Loading accounts…")).toBeInTheDocument();

    await act(async () => {
      finishLoading?.({ kind: "unauthenticated" });
    });

    // The page never renders this case: the session seam ends the session and
    // the route guard replaces this page on the same update.
    expect(auth.expireSession).toHaveBeenCalledOnce();
  });

  it("signs out", async () => {
    resolveOnceWith({ kind: "ok", data: [] });
    const user = userEvent.setup();
    renderAccounts();

    await user.click(screen.getByRole("button", { name: "Sign out" }));
    expect(auth.logout).toHaveBeenCalledOnce();
  });
});
