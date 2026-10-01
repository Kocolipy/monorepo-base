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

/**
 * The self-service password change: open to every authenticated visitor, and
 * the only route a session with the change-required flag is offered.
 */
export const CREDENTIAL_CHANGE_PATH = "/change-password";

/** What a route requires of the current visitor. */
export type SessionRequirement = "authenticated" | "guest" | AuthRole;

/**
 * State a redirect carries forward.
 *
 * `from` is the **return destination**: the protected path the visitor asked
 * for, replayed once they sign in. `expired` distinguishes an expired session
 * from a cold visit, and `passwordChanged` a session ended by the User's own
 * successful password change, so the login route can say which happened.
 */
export interface SessionRouteState {
  from?: string;
  expired?: boolean;
  passwordChanged?: boolean;
}

export type SessionRoute =
  | { kind: "pending" }
  | { kind: "render" }
  | { kind: "redirect"; to: string; state?: SessionRouteState };

export interface SessionRouteInput {
  /** The session carries the change-required flag and is confined to the change. */
  passwordChangeRequired: boolean;
  /** The current `guest` status came from a successful password change. */
  passwordChanged: boolean;
  /** The path being visited, recorded as the return destination on a redirect. */
  pathname: string;
  requires: SessionRequirement;
  /** A return destination carried by an earlier redirect, if there was one. */
  returnTo?: string;
  /** The authenticated account's role, when one is available. */
  role?: AuthRole | null;
  sessionExpired: boolean;
  status: AuthStatus;
}

export function resolveSessionRoute({
  passwordChangeRequired,
  passwordChanged,
  pathname,
  requires,
  returnTo,
  role,
  sessionExpired,
  status,
}: SessionRouteInput): SessionRoute {
  if (status === "checking") return { kind: "pending" };

  if (status === "guest") {
    if (requires === "guest") return { kind: "render" };
    // A change ends the session on purpose, so the page it was made from is no
    // destination to replay: the next sign-in lands on the default instead.
    if (passwordChanged) {
      return { kind: "redirect", state: { passwordChanged: true }, to: LOGIN_PATH };
    }
    return {
      kind: "redirect",
      state: { expired: sessionExpired, from: pathname },
      to: LOGIN_PATH,
    };
  }

  // Confinement outranks every other rule, a recorded return destination and an
  // Admin's role included: the session may do nothing else until it changes.
  if (passwordChangeRequired) {
    return pathname === CREDENTIAL_CHANGE_PATH
      ? { kind: "render" }
      : { kind: "redirect", to: CREDENTIAL_CHANGE_PATH };
  }

  if (requires === "guest") return { kind: "redirect", to: returnTo ?? DEFAULT_DESTINATION };
  if (requires === "authenticated" || role === requires) return { kind: "render" };
  return { kind: "redirect", to: DEFAULT_DESTINATION };
}
