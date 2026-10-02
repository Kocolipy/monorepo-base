import { act, render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { AuthContext, type AuthContextState } from "@/auth/auth-context-value";
import { apiFetch } from "@/lib/http";

import type { Connector, ConnectorToken, IssuedToken } from "./accounts-api";
import { Connectors } from "./connectors";

vi.mock("@/lib/http", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/lib/http")>()),
  apiFetch: vi.fn(),
}));

const apiFetchMock = vi.mocked(apiFetch);

const auth: AuthContextState = {
  changePassword: vi.fn(),
  expireSession: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
  passwordChanged: false,
  sessionExpired: false,
  signOutForInactivity: vi.fn(),
  signedOutForInactivity: false,
  status: "authenticated",
  user: { idleTimeoutSeconds: 900, passwordChangeRequired: false, role: "ADMIN", username: "ada" },
};

const CONNECTOR_ID = "c0000000-0000-4000-8000-000000000001";
const TOKEN_ID = "a1b2c3d4-0000-4000-8000-000000000001";

const token = (overrides: Partial<ConnectorToken> = {}): ConnectorToken => ({
  active: true,
  expiresAt: "2027-01-02T00:00:00Z",
  id: TOKEN_ID,
  issuedAt: "2026-01-02T00:00:00Z",
  originalExpiresAt: "2027-01-02T00:00:00Z",
  revokedAt: null,
  scope: "READ_WRITE",
  ...overrides,
});

const connector = (overrides: Partial<Connector> = {}): Connector => ({
  createdAt: "2026-01-01T00:00:00Z",
  displayName: "Okta",
  id: CONNECTOR_ID,
  tokens: [],
  ...overrides,
});

const issued = (overrides: Partial<IssuedToken> = {}): IssuedToken => ({
  connectorId: CONNECTOR_ID,
  expiresAt: "2027-01-02T00:00:00Z",
  issuedAt: "2026-01-02T00:00:00Z",
  presentedValue: "scim_plaintext_value_shown_once",
  scope: "READ_WRITE",
  tokenId: TOKEN_ID,
  ...overrides,
});

/**
 * Answers every listing read from `listings` in turn (the last one repeats) and
 * every other request from `actions`, in order.
 */
function routeApi({ actions = [], listings }: { actions?: object[]; listings: object[] }) {
  const reads = [...listings];
  const queue = [...actions];
  apiFetchMock.mockImplementation(((path: string, init?: RequestInit) => {
    if (path === "/api/admin/connectors" && (init?.method ?? "GET") === "GET") {
      return Promise.resolve(reads.length > 1 ? reads.shift() : reads[0]);
    }
    const next = queue.shift();
    if (next === undefined) throw new Error(`unexpected ${init?.method} ${path}`);
    return Promise.resolve(next);
  }) as never);
}

function renderConnectors() {
  return render(
    <AuthContext.Provider value={auth}>
      <Connectors />
    </AuthContext.Provider>,
  );
}

const section = (name: string) => within(screen.getByRole("region", { name: `Connector ${name}` }));

describe("Connectors", () => {
  beforeEach(() => {
    apiFetchMock.mockReset();
  });

  it("lists every connector with its tokens' scope, dates and status", async () => {
    routeApi({
      listings: [
        {
          kind: "ok",
          data: [
            connector({
              tokens: [
                token(),
                token({ active: false, id: "e0000000-x", scope: "READ_ONLY" }),
                token({ active: false, id: "r0000000-x", revokedAt: "2026-02-03T00:00:00Z" }),
              ],
            }),
          ],
        },
      ],
    });
    renderConnectors();

    expect(await screen.findByRole("heading", { name: "Okta" })).toBeInTheDocument();
    const okta = section("Okta");
    expect(okta.getAllByRole("columnheader").map((header) => header.textContent)).toEqual([
      "Token",
      "Scope",
      "Issued",
      "Expires",
      "Status",
      "Actions",
    ]);
    expect(okta.getByText("Created 2026-01-01")).toBeInTheDocument();
    const active = within(okta.getByRole("rowheader", { name: "a1b2c3d4" }).closest("tr")!);
    expect(active.getByText("READ_WRITE")).toBeInTheDocument();
    expect(active.getByText("2026-01-02")).toBeInTheDocument();
    expect(active.getByText("2027-01-02")).toBeInTheDocument();
    expect(active.getByText("Active")).toBeInTheDocument();
    expect(okta.getByText("Expired")).toBeInTheDocument();
    expect(okta.getByText("Revoked 2026-02-03")).toBeInTheDocument();
    // Rotate and revoke only for a token that would still be accepted.
    expect(okta.getAllByRole("button", { name: /^Rotate token/ })).toHaveLength(1);
    expect(okta.getAllByRole("button", { name: /^Revoke token/ })).toHaveLength(1);
    // No token value anywhere in the listing: the type has no field for one.
    expect(screen.queryByLabelText("New token value")).not.toBeInTheDocument();
  });

  it("creates a connector and re-reads the listing", async () => {
    routeApi({
      actions: [{ kind: "ok", data: connector() }],
      listings: [
        { kind: "ok", data: [] },
        { kind: "ok", data: [connector()] },
      ],
    });
    const user = userEvent.setup();
    renderConnectors();

    expect(await screen.findByText("No connectors exist.")).toBeInTheDocument();
    await user.type(screen.getByLabelText("New connector name"), "  Okta  ");
    await user.click(screen.getByRole("button", { name: "Create connector" }));

    expect(apiFetchMock).toHaveBeenCalledWith(
      "/api/admin/connectors",
      {
        body: JSON.stringify({ displayName: "Okta" }),
        headers: { "Content-Type": "application/json" },
        method: "POST",
      },
      expect.any(Function),
    );
    expect(await screen.findByRole("heading", { name: "Okta" })).toBeInTheDocument();
    expect(section("Okta").getByText("No tokens issued.")).toBeInTheDocument();
    expect(section("Okta").queryByRole("table")).not.toBeInTheDocument();
    expect(screen.getByLabelText("New connector name")).toHaveValue("");
  });

  it("says it is loading until the listing arrives", async () => {
    let finish: ((result: object) => void) | undefined;
    apiFetchMock.mockReturnValueOnce(
      new Promise((resolve) => {
        finish = resolve;
      }) as never,
    );
    renderConnectors();

    expect(screen.getByText("Loading connectors…")).toBeInTheDocument();
    await act(async () => {
      finish?.({ kind: "ok", data: [] });
    });
    expect(await screen.findByText("No connectors exist.")).toBeInTheDocument();
    expect(screen.queryByText("Loading connectors…")).not.toBeInTheDocument();
  });

  it("reports a failed create naming the connector", async () => {
    routeApi({
      actions: [{ kind: "failed", status: 503 }],
      listings: [{ kind: "ok", data: [] }],
    });
    const user = userEvent.setup();
    renderConnectors();

    await user.type(await screen.findByLabelText("New connector name"), "Okta");
    await user.click(screen.getByRole("button", { name: "Create connector" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Creating Okta failed. Please try again.",
    );
    // The name is kept for a retry: only a successful create clears it.
    expect(screen.getByLabelText("New connector name")).toHaveValue("Okta");
  });

  it("does not submit a blank connector name", async () => {
    routeApi({ listings: [{ kind: "ok", data: [] }] });
    const user = userEvent.setup();
    renderConnectors();

    await user.type(await screen.findByLabelText("New connector name"), "   ");
    await user.click(screen.getByRole("button", { name: "Create connector" }));

    expect(apiFetchMock).toHaveBeenCalledTimes(1);
  });

  it("deletes a connector", async () => {
    routeApi({
      actions: [{ kind: "ok", data: undefined }],
      listings: [
        { kind: "ok", data: [connector()] },
        { kind: "ok", data: [] },
      ],
    });
    const user = userEvent.setup();
    renderConnectors();

    await user.click(await screen.findByRole("button", { name: "Delete Okta" }));

    expect(apiFetchMock).toHaveBeenCalledWith(`/api/admin/connectors/${CONNECTOR_ID}`, {
      method: "DELETE",
    });
    expect(await screen.findByText("No connectors exist.")).toBeInTheDocument();
  });

  /**
   * The one-time disclosure: the plaintext from the issue response is shown, with
   * the warning that it cannot be retrieved again, and nowhere but component
   * state — dismissing it is final, and the re-read listing never carries it.
   */
  it("issues a token and discloses its value once", async () => {
    routeApi({
      actions: [{ kind: "ok", data: issued() }],
      listings: [
        { kind: "ok", data: [connector()] },
        { kind: "ok", data: [connector({ tokens: [token()] })] },
      ],
    });
    const user = userEvent.setup();
    renderConnectors();

    await screen.findByRole("heading", { name: "Okta" });
    await user.selectOptions(screen.getByLabelText("Scope for Okta"), "READ_WRITE");
    await user.type(screen.getByLabelText("Lifetime in days for Okta (default 365)"), "30");
    await user.click(screen.getByRole("button", { name: "Issue token for Okta" }));

    expect(apiFetchMock).toHaveBeenCalledWith(
      `/api/admin/connectors/${CONNECTOR_ID}/tokens`,
      {
        body: JSON.stringify({ scope: "READ_WRITE", lifetimeDays: 30 }),
        headers: { "Content-Type": "application/json" },
        method: "POST",
      },
      expect.any(Function),
    );
    const disclosure = await screen.findByRole("region", { name: "New token for Okta" });
    expect(within(disclosure).getByLabelText("New token value")).toHaveTextContent(
      "scim_plaintext_value_shown_once",
    );
    expect(disclosure).toHaveTextContent("cannot be retrieved again");
    expect(within(disclosure).getByText("READ_WRITE · expires 2027-01-02")).toBeInTheDocument();
    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(0);

    await user.click(screen.getByRole("button", { name: "Dismiss token" }));
    expect(screen.queryByText("scim_plaintext_value_shown_once")).not.toBeInTheDocument();
  });

  it("issues a read-only token with the default lifetime when none is given", async () => {
    routeApi({
      actions: [{ kind: "ok", data: issued({ scope: "READ_ONLY" }) }],
      listings: [{ kind: "ok", data: [connector()] }],
    });
    const user = userEvent.setup();
    renderConnectors();

    await user.click(await screen.findByRole("button", { name: "Issue token for Okta" }));

    expect(apiFetchMock).toHaveBeenCalledWith(
      `/api/admin/connectors/${CONNECTOR_ID}/tokens`,
      expect.objectContaining({
        body: JSON.stringify({ scope: "READ_ONLY", lifetimeDays: null }),
      }),
      expect.any(Function),
    );
  });

  it("rotates a token and discloses the replacement once", async () => {
    routeApi({
      actions: [{ kind: "ok", data: issued({ presentedValue: "scim_rotated_value" }) }],
      listings: [{ kind: "ok", data: [connector({ tokens: [token()] })] }],
    });
    const user = userEvent.setup();
    renderConnectors();

    await user.click(await screen.findByRole("button", { name: "Rotate token a1b2c3d4" }));

    expect(apiFetchMock).toHaveBeenCalledWith(
      `/api/admin/connectors/${CONNECTOR_ID}/tokens/${TOKEN_ID}/rotate`,
      { body: "{}", headers: { "Content-Type": "application/json" }, method: "POST" },
      expect.any(Function),
    );
    expect(await screen.findByLabelText("New token value")).toHaveTextContent("scim_rotated_value");
  });

  it("revokes a token and shows the re-read state", async () => {
    routeApi({
      actions: [{ kind: "ok", data: undefined }],
      listings: [
        { kind: "ok", data: [connector({ tokens: [token()] })] },
        {
          kind: "ok",
          data: [
            connector({ tokens: [token({ active: false, revokedAt: "2026-05-06T00:00:00Z" })] }),
          ],
        },
      ],
    });
    const user = userEvent.setup();
    renderConnectors();

    await user.click(await screen.findByRole("button", { name: "Revoke token a1b2c3d4" }));

    expect(apiFetchMock).toHaveBeenCalledWith(
      `/api/admin/connectors/${CONNECTOR_ID}/tokens/${TOKEN_ID}/revoke`,
      { method: "POST" },
    );
    expect(await screen.findByText("Revoked 2026-05-06")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /^Revoke token/ })).not.toBeInTheDocument();
    expect(screen.queryByLabelText("New token value")).not.toBeInTheDocument();
  });

  it("disables every control while a change is in flight", async () => {
    let finish: ((result: object) => void) | undefined;
    apiFetchMock.mockImplementation(((_path: string, init?: RequestInit) => {
      if ((init?.method ?? "GET") === "GET") {
        return Promise.resolve({ kind: "ok", data: [connector({ tokens: [token()] })] });
      }
      return new Promise((resolve) => {
        finish = resolve;
      });
    }) as never);
    const user = userEvent.setup();
    renderConnectors();

    await user.click(await screen.findByRole("button", { name: "Revoke token a1b2c3d4" }));

    for (const name of [
      "Delete Okta",
      "Issue token for Okta",
      "Rotate token a1b2c3d4",
      "Create connector",
    ]) {
      expect(screen.getByRole("button", { name })).toBeDisabled();
    }
    finish?.({ kind: "ok", data: undefined });
    expect(await screen.findByRole("button", { name: "Delete Okta" })).toBeEnabled();
  });

  it("reports an out-of-range request as a refusal", async () => {
    routeApi({
      actions: [{ kind: "failed", status: 400 }],
      listings: [{ kind: "ok", data: [connector()] }],
    });
    const user = userEvent.setup();
    renderConnectors();

    await user.click(await screen.findByRole("button", { name: "Issue token for Okta" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Refused: Issuing a token for Okta — check the values and try again.",
    );
    expect(screen.queryByLabelText("New token value")).not.toBeInTheDocument();
  });

  it("reports a connector that has since gone", async () => {
    routeApi({
      actions: [{ kind: "failed", status: 404 }],
      listings: [{ kind: "ok", data: [connector()] }],
    });
    const user = userEvent.setup();
    renderConnectors();

    await user.click(await screen.findByRole("button", { name: "Delete Okta" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Deleting Okta failed: it no longer exists. Reload the page.",
    );
  });

  it("reports any other failure and an expired security token", async () => {
    routeApi({
      actions: [{ kind: "failed", status: 503 }, { kind: "failed" }, { kind: "csrf-expired" }],
      listings: [{ kind: "ok", data: [connector({ tokens: [token()] })] }],
    });
    const user = userEvent.setup();
    renderConnectors();

    await user.click(await screen.findByRole("button", { name: "Rotate token a1b2c3d4" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Rotating the token failed. Please try again.",
    );

    await user.click(screen.getByRole("button", { name: "Revoke token a1b2c3d4" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Revoking the token failed. Please try again.",
    );

    await user.click(screen.getByRole("button", { name: "Delete Okta" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Your security token expired. Please try again.",
    );
  });

  it("reports a refused mutation as permission denied and keeps the session", async () => {
    routeApi({
      actions: [{ kind: "forbidden" }],
      listings: [{ kind: "ok", data: [connector({ tokens: [token()] })] }],
    });
    const user = userEvent.setup();
    renderConnectors();

    await user.click(await screen.findByRole("button", { name: "Rotate token a1b2c3d4" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      /^You don't have permission to do this\.$/,
    );
    expect(auth.expireSession).not.toHaveBeenCalled();
    expect(auth.logout).not.toHaveBeenCalled();
  });

  it("reports a refused listing read as permission denied and keeps the session", async () => {
    routeApi({ listings: [{ kind: "forbidden" }] });
    renderConnectors();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      /^You don't have permission to do this\.$/,
    );
    expect(auth.expireSession).not.toHaveBeenCalled();
    expect(auth.logout).not.toHaveBeenCalled();
  });

  it("reports a failed listing read without claiming there are no connectors", async () => {
    routeApi({ listings: [{ kind: "failed", status: 503 }] });
    renderConnectors();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Unable to load the connectors. Please try again.",
    );
    expect(screen.queryByText("No connectors exist.")).not.toBeInTheDocument();
    expect(screen.queryByText("Loading connectors…")).not.toBeInTheDocument();
  });
});
