import { apiFetch } from "@/lib/http";

export interface AuthUser {
  username: string;
}

/** Shown when a request still fails CSRF after `apiFetch` retried it. */
const CSRF_MESSAGE = "Your security token expired. Please try again.";

export async function getCurrentUser(): Promise<AuthUser | null> {
  const response = await apiFetch("/api/auth/me");
  if (response.status === 401) return null;
  if (!response.ok) throw new Error("Unable to check the current session.");
  return response.json() as Promise<AuthUser>;
}

export async function login(username: string, password: string): Promise<AuthUser> {
  const response = await apiFetch("/api/auth/login", {
    body: JSON.stringify({ username, password }),
    headers: { "Content-Type": "application/json" },
    method: "POST",
  });
  if (response.status === 401) throw new Error("The username or password is incorrect.");
  if (response.status === 403) throw new Error(CSRF_MESSAGE);
  if (!response.ok) throw new Error("Unable to sign in. Please try again.");
  return response.json() as Promise<AuthUser>;
}

export async function logout(): Promise<void> {
  const response = await apiFetch("/api/auth/logout", { method: "DELETE" });
  if (response.status === 403) throw new Error(CSRF_MESSAGE);
  if (!response.ok && response.status !== 401) {
    throw new Error("Unable to sign out. Please try again.");
  }
}
