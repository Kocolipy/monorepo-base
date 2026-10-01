import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";

import { discardCsrfToken } from "@/lib/http";

import * as authApi from "./api";
import type { AuthUser } from "./api";
import { AuthContext, type AuthContextState, type AuthStatus } from "./auth-context-value";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>("checking");
  const [user, setUser] = useState<AuthUser | null>(null);
  const [sessionExpired, setSessionExpired] = useState(false);
  const [passwordChanged, setPasswordChanged] = useState(false);

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

  /**
   * Ends the session the backend has already refused.
   *
   * Driven by `useSessionRequest`, never by a page: the distinction between an
   * expired session and a cold visit is set here and read by the route guards.
   */
  const expireSession = useCallback(() => {
    // The session's CSRF token ended with it; the next login fetches its own.
    discardCsrfToken();
    setUser(null);
    setSessionExpired(true);
    setPasswordChanged(false);
    setStatus("guest");
  }, []);

  const value = useMemo<AuthContextState>(
    () => ({
      changePassword: async (currentPassword, newPassword) => {
        const outcome = await authApi.changePassword(currentPassword, newPassword);
        if (outcome.kind === "changed") {
          // The backend has already ended this session along with every other
          // one the User held; mirror that, recording why for the login page.
          setUser(null);
          setSessionExpired(false);
          setPasswordChanged(true);
          setStatus("guest");
        }
        return outcome;
      },
      expireSession,
      login: async (username, password) => {
        const currentUser = await authApi.login(username, password);
        setUser(currentUser);
        setSessionExpired(false);
        setPasswordChanged(false);
        setStatus("authenticated");
      },
      logout: async () => {
        await authApi.logout();
        setUser(null);
        setSessionExpired(false);
        setPasswordChanged(false);
        setStatus("guest");
      },
      passwordChanged,
      sessionExpired,
      status,
      user,
    }),
    [expireSession, passwordChanged, sessionExpired, status, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
