import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";

import * as authApi from "./api";
import type { AuthUser } from "./api";
import { AuthContext, type AuthContextState, type AuthStatus } from "./auth-context-value";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>("checking");
  const [user, setUser] = useState<AuthUser | null>(null);
  const [sessionExpired, setSessionExpired] = useState(false);

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
    setUser(null);
    setSessionExpired(true);
    setStatus("guest");
  }, []);

  const value = useMemo<AuthContextState>(
    () => ({
      expireSession,
      login: async (username, password) => {
        const currentUser = await authApi.login(username, password);
        setUser(currentUser);
        setSessionExpired(false);
        setStatus("authenticated");
      },
      logout: async () => {
        await authApi.logout();
        setUser(null);
        setSessionExpired(false);
        setStatus("guest");
      },
      sessionExpired,
      status,
      user,
    }),
    [expireSession, sessionExpired, status, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
