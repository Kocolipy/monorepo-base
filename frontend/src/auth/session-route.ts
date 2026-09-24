/**
 * The session's routing contract, as one pure transition table.
 *
 * Both route guards are adapters over `resolveSessionRoute`, so the whole
 * contract — who waits, who renders, who is redirected where, and what the
 * redirect carries — is decided in one function that needs no router to test.
 */

import type { AuthRole } from "./api";
import type { AuthStatus } from "./auth-context-value";

/** The login route, which is also where an unauthenticated visitor is sent. */
export const LOGIN_PATH = "/";

/** Where an authenticated visitor lands with no return destination recorded. */
export const DEFAULT_DESTINATION = "/showcase";

/** What a route requires of the current visitor. */
export type SessionRequirement = "authenticated" | "guest" | AuthRole;

/**
 * State a redirect carries forward.
 *
 * `from` is the **return destination**: the protected path the visitor asked
 * for, replayed once they sign in. `expired` distinguishes an expired session
 * from a cold visit, so the login route can say which happened.
 */
export interface SessionRouteState {
  from?: string;
  expired?: boolean;
}

export type SessionRoute =
  | { kind: "pending" }
  | { kind: "render" }
  | { kind: "redirect"; to: string; state?: SessionRouteState };

export interface SessionRouteInput {
  /** The path being visited, recorded as the return destination on a redirect. */
  pathname: string;
  requires: SessionRequirement;
  /** A return destination carried by an earlier redirect, if there was one. */
  returnTo?: string;
  /** The authenticated account's role, when one is available. */
  role?: AuthRole;
  sessionExpired: boolean;
  status: AuthStatus;
}

export function resolveSessionRoute({
  pathname,
  requires,
  returnTo,
  role,
  sessionExpired,
  status,
}: SessionRouteInput): SessionRoute {
  if (status === "checking") return { kind: "pending" };

  if (requires !== "guest") {
    if (status === "guest") {
      return {
        kind: "redirect",
        state: { expired: sessionExpired, from: pathname },
        to: LOGIN_PATH,
      };
    }
    if (requires === "authenticated" || role === requires) return { kind: "render" };
    return { kind: "redirect", to: DEFAULT_DESTINATION };
  }

  if (status === "guest") return { kind: "render" };
  return { kind: "redirect", to: returnTo ?? DEFAULT_DESTINATION };
}
