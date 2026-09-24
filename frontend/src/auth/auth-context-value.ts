import { createContext, useContext } from "react";

import type { AuthUser } from "./api";

export type AuthStatus = "checking" | "authenticated" | "guest";

export interface AuthContextValue {
  expireSession: () => void;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  status: AuthStatus;
  user: AuthUser | null;
}

export const AuthContext = createContext<AuthContextValue | null>(null);

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider.");
  return context;
}
