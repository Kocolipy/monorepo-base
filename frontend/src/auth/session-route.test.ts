import { describe, expect, it } from "vitest";

import {
  DEFAULT_DESTINATION,
  LOGIN_PATH,
  resolveSessionRoute,
  type SessionRoute,
  type SessionRouteInput,
} from "./session-route";

const input = (overrides: Partial<SessionRouteInput> = {}): SessionRouteInput => ({
  pathname: "/showcase",
  requires: "authenticated",
  sessionExpired: false,
  status: "authenticated",
  ...overrides,
});

describe("resolveSessionRoute", () => {
  const cases: [string, SessionRouteInput, SessionRoute][] = [
    [
      "waits on a protected route while the session status is unknown",
      input({ status: "checking" }),
      { kind: "pending" },
    ],
    [
      "waits on a guest route while the session status is unknown",
      input({ pathname: LOGIN_PATH, requires: "guest", status: "checking" }),
      { kind: "pending" },
    ],
    [
      "renders a protected route for an authenticated visitor",
      input({ status: "authenticated" }),
      { kind: "render" },
    ],
    [
      "sends a guest to login, recording the return destination",
      input({ pathname: "/showcase", status: "guest" }),
      { kind: "redirect", state: { expired: false, from: "/showcase" }, to: LOGIN_PATH },
    ],
    [
      "marks the redirect as an expiry when the session ended",
      input({ pathname: "/showcase", sessionExpired: true, status: "guest" }),
      { kind: "redirect", state: { expired: true, from: "/showcase" }, to: LOGIN_PATH },
    ],
    [
      "renders a guest route for a guest",
      input({ pathname: LOGIN_PATH, requires: "guest", status: "guest" }),
      { kind: "render" },
    ],
    [
      "sends an authenticated visitor off a guest route to the default destination",
      input({ pathname: LOGIN_PATH, requires: "guest", status: "authenticated" }),
      { kind: "redirect", to: DEFAULT_DESTINATION },
    ],
    [
      "replays the recorded return destination instead of the default",
      input({
        pathname: LOGIN_PATH,
        requires: "guest",
        returnTo: "/showcase/settings",
        status: "authenticated",
      }),
      { kind: "redirect", to: "/showcase/settings" },
    ],
    [
      "ignores an expiry flag once the visitor is authenticated again",
      input({ pathname: LOGIN_PATH, requires: "guest", sessionExpired: true, status: "guest" }),
      { kind: "render" },
    ],
  ];

  it.each(cases)("%s", (_name, given, expected) => {
    expect(resolveSessionRoute(given)).toEqual(expected);
  });

  it("records the visited path, not a fixed one, as the return destination", () => {
    const route = resolveSessionRoute(input({ pathname: "/reports/42", status: "guest" }));
    expect(route).toEqual({
      kind: "redirect",
      state: { expired: false, from: "/reports/42" },
      to: LOGIN_PATH,
    });
  });

  it("renders an ADMIN-only route for an administrator", () => {
    expect(
      resolveSessionRoute(input({ pathname: "/accounts", requires: "ADMIN", role: "ADMIN" })),
    ).toEqual({ kind: "render" });
  });

  it("redirects a USER away from an ADMIN-only route", () => {
    expect(
      resolveSessionRoute(input({ pathname: "/accounts", requires: "ADMIN", role: "USER" })),
    ).toEqual({ kind: "redirect", to: DEFAULT_DESTINATION });
  });

  it("sends a guest on an ADMIN-only route to login with its return destination", () => {
    expect(
      resolveSessionRoute(input({ pathname: "/accounts", requires: "ADMIN", status: "guest" })),
    ).toEqual({
      kind: "redirect",
      state: { expired: false, from: "/accounts" },
      to: LOGIN_PATH,
    });
  });
});
