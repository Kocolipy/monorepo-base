import { useEffect, useMemo, useState, type ReactNode } from "react";

import * as authApi from "./api";
import type { AuthUser } from "./api";
import { AuthContext, type AuthContextValue, type AuthStatus } from "./auth-context-value";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>("checking");
  const [user, setUser] = useState<AuthUser | null>(null);

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

  const value = useMemo<AuthContextValue>(
    () => ({
      login: async (username, password) => {
        const currentUser = await authApi.login(username, password);
        setUser(currentUser);
        setStatus("authenticated");
      },
      logout: async () => {
        await authApi.logout();
        setUser(null);
        setStatus("guest");
      },
      status,
      user,
    }),
    [status, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
