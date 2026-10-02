import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";

import { discardCsrfToken } from "@/lib/http";

import * as authApi from "./api";
import type { AuthUser } from "./api";
import { AuthContext, type AuthContextState, type AuthStatus } from "./auth-context-value";
import { IdleSignOut } from "./idle-sign-out";

/** Why the visitor is a guest now, for the login page to say. Cleared by every transition. */
interface Provenance {
  passwordChanged: boolean;
  sessionExpired: boolean;
  signedOutForInactivity: boolean;
}

const NO_PROVENANCE: Provenance = {
  passwordChanged: false,
  sessionExpired: false,
  signedOutForInactivity: false,
};

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>("checking");
  const [user, setUser] = useState<AuthUser | null>(null);
  const [provenance, setProvenance] = useState<Provenance>(NO_PROVENANCE);

  useEffect(() => {
    let active = true;
    void authApi
      .getCurrentUser()
      .then((currentUser) => {
        if (!active) return;
        setUser(currentUser);
        setStatus(currentUser ? "authenticated" : "guest");
      })
      .catch(() => {
        if (active) setStatus("guest");
      });
    return () => {
      active = false;
    };
  }, []);

  /** Moves to `guest`, recording why. */
  const endSession = useCallback((why: Partial<Provenance>) => {
    setUser(null);
    setProvenance({ ...NO_PROVENANCE, ...why });
    setStatus("guest");
  }, []);

  /**
   * Ends the session the backend has already refused.
   *
   * Driven by `useSessionRequest`, never by a page: the distinction between an
   * expired session and a cold visit is set here and read by the route guards.
   */
  const expireSession = useCallback(() => {
    // The session's CSRF token ended with it; the next login fetches its own.
    discardCsrfToken();
    endSession({ sessionExpired: true });
  }, [endSession]);

  /**
   * Ends a session the user left idle. Driven by `IdleSignOut` only.
   *
   * The logout is attempted, but its outcome cannot keep the session on screen:
   * the point is that an unattended page stops showing, so a logout that fails
   * still clears the state, and the token is forgotten either way.
   */
  const signOutForInactivity = useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      // Signed out locally regardless; the backend's own idle bound ends the session.
    }
    discardCsrfToken();
    endSession({ signedOutForInactivity: true });
  }, [endSession]);

  const value = useMemo<AuthContextState>(
    () => ({
      changePassword: async (currentPassword, newPassword) => {
        const outcome = await authApi.changePassword(currentPassword, newPassword);
        if (outcome.kind === "changed") {
          // The backend has already ended this session along with every other
          // one the User held; mirror that, recording why for the login page.
          endSession({ passwordChanged: true });
        }
        return outcome;
      },
      expireSession,
      login: async (username, password) => {
        const currentUser = await authApi.login(username, password);
        setUser(currentUser);
        setProvenance(NO_PROVENANCE);
        setStatus("authenticated");
      },
      logout: async () => {
        await authApi.logout();
        endSession({});
      },
      passwordChanged: provenance.passwordChanged,
      sessionExpired: provenance.sessionExpired,
      signOutForInactivity,
      signedOutForInactivity: provenance.signedOutForInactivity,
      status,
      user,
    }),
    [endSession, expireSession, provenance, signOutForInactivity, status, user],
  );

  return (
    <AuthContext.Provider value={value}>
      {children}
      {/* Mounted only while signed in, so the idle clock starts at authentication. */}
      {user ? <IdleSignOut idleTimeoutSeconds={user.idleTimeoutSeconds} /> : null}
    </AuthContext.Provider>
  );
}
