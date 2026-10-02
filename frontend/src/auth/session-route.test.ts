import { describe, expect, it } from "vitest";

import {
  CREDENTIAL_CHANGE_PATH,
  DEFAULT_DESTINATION,
  LOGIN_PATH,
  resolveSessionRoute,
  type SessionRoute,
  type SessionRouteInput,
} from "./session-route";

const input = (overrides: Partial<SessionRouteInput> = {}): SessionRouteInput => ({
  passwordChangeRequired: false,
  passwordChanged: false,
  pathname: "/showcase",
  requires: "authenticated",
  sessionExpired: false,
  signedOutForInactivity: false,
  status: "authenticated",
  ...overrides,
});

/** A confined session exactly as the backend reports one: flagged, and holding no role. */
const flagged = (overrides: Partial<SessionRouteInput> = {}): SessionRouteInput =>
  input({ passwordChangeRequired: true, role: null, ...overrides });

const toChangePassword: SessionRoute = { kind: "redirect", to: CREDENTIAL_CHANGE_PATH };

describe("resolveSessionRoute for the change-required flag", () => {
  const cases: [string, SessionRouteInput, SessionRoute][] = [
    [
      "waits for a flagged session like any other",
      flagged({ status: "checking" }),
      { kind: "pending" },
    ],
    [
      "renders the change-password route for a flagged session",
      flagged({ pathname: CREDENTIAL_CHANGE_PATH }),
      { kind: "render" },
    ],
    [
      "confines a flagged session on the showcase",
      flagged({ pathname: "/showcase" }),
      toChangePassword,
    ],
    [
      "confines a flagged session on the ADMIN-only route",
      flagged({ pathname: "/accounts", requires: "ADMIN" }),
      toChangePassword,
    ],
    [
      "confines a flagged Admin even if a role were reported",
      flagged({ pathname: "/accounts", requires: "ADMIN", role: "ADMIN" }),
      toChangePassword,
    ],
    [
      "confines a flagged USER even if a role were reported",
      flagged({ pathname: "/showcase", role: "USER" }),
      toChangePassword,
    ],
    [
      "sends a flagged session off the login route to the change",
      flagged({ pathname: LOGIN_PATH, requires: "guest" }),
      toChangePassword,
    ],
    [
      "ignores a recorded return destination while flagged",
      flagged({ pathname: LOGIN_PATH, requires: "guest", returnTo: "/accounts" }),
      toChangePassword,
    ],
    [
      "renders the change-password route for an unflagged USER",
      input({ pathname: CREDENTIAL_CHANGE_PATH, role: "USER" }),
      { kind: "render" },
    ],
    [
      "renders the change-password route for an unflagged ADMIN",
      input({ pathname: CREDENTIAL_CHANGE_PATH, role: "ADMIN" }),
      { kind: "render" },
    ],
    [
      "lets an unflagged ADMIN reach the ADMIN-only route",
      input({ pathname: "/accounts", requires: "ADMIN", role: "ADMIN" }),
      { kind: "render" },
    ],
    [
      "sends a Visitor on the change-password route to login",
      input({ pathname: CREDENTIAL_CHANGE_PATH, status: "guest" }),
      { kind: "redirect", state: { expired: false, from: CREDENTIAL_CHANGE_PATH }, to: LOGIN_PATH },
    ],
    [
      "returns a User whose change succeeded to login, recording no return destination",
      input({ passwordChanged: true, pathname: CREDENTIAL_CHANGE_PATH, status: "guest" }),
      { kind: "redirect", state: { passwordChanged: true }, to: LOGIN_PATH },
    ],
    [
      "renders login after a successful change",
      input({ passwordChanged: true, pathname: LOGIN_PATH, requires: "guest", status: "guest" }),
      { kind: "render" },
    ],
  ];

  it.each(cases)("%s", (_name, given, expected) => {
    expect(resolveSessionRoute(given)).toEqual(expected);
  });
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
      "marks the redirect as an inactivity sign-out, replaying the page it left",
      input({ pathname: "/accounts", signedOutForInactivity: true, status: "guest" }),
      { kind: "redirect", state: { from: "/accounts", inactive: true }, to: LOGIN_PATH },
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
