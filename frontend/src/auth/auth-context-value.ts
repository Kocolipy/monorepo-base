import { createContext, useContext } from "react";

import type { AuthUser } from "./api";

/** The session status: what the SPA currently knows about the visitor's session. */
export type AuthStatus = "checking" | "authenticated" | "guest";

/**
 * What a page may read about the session.
 *
 * Deliberately has no member for *ending* a session: a feature request that
 * comes back unauthenticated is handled by `useSessionRequest`, so no page has
 * to remember to relay it.
 */
export interface AuthContextValue {
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  status: AuthStatus;
  /** True while the current `guest` status came from an expired session rather than a cold visit. */
  sessionExpired: boolean;
  user: AuthUser | null;
}

/** The context value including the session controls only `src/auth` may drive. */
export interface AuthContextState extends AuthContextValue {
  expireSession: () => void;
}

export const AuthContext = createContext<AuthContextState | null>(null);

/** Internal to `src/auth`: the full state, session controls included. */
export function useAuthState(): AuthContextState {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider.");
  return context;
}

export function useAuth(): AuthContextValue {
  return useAuthState();
}
