import { apiFetch, CSRF_EXPIRED_MESSAGE, type ApiResult } from "@/lib/http";

export type AuthRole = "USER" | "ADMIN";

export interface AuthUser {
  role: AuthRole;
  username: string;
}

const decodeUser = (response: Response): Promise<AuthUser> => response.json() as Promise<AuthUser>;

export async function getCurrentUser(): Promise<AuthUser | null> {
  const result = await apiFetch("/api/auth/me", {}, decodeUser);
  switch (result.kind) {
    case "ok":
      return result.data;
    case "unauthenticated":
      return null;
    case "csrf-expired":
    case "failed":
      throw new Error("Unable to check the current session.");
  }
}

export async function login(username: string, password: string): Promise<AuthUser> {
  const result = await apiFetch(
    "/api/auth/login",
    {
      body: JSON.stringify({ username, password }),
      headers: { "Content-Type": "application/json" },
      method: "POST",
    },
    decodeUser,
  );

  switch (result.kind) {
    case "ok":
      return result.data;
    case "unauthenticated":
      throw new Error("The username or password is incorrect.");
    case "csrf-expired":
      throw new Error(CSRF_EXPIRED_MESSAGE);
    case "failed":
      throw new Error("Unable to sign in. Please try again.");
  }
}

export async function logout(): Promise<void> {
  const result: ApiResult<void> = await apiFetch("/api/auth/logout", { method: "DELETE" });
  switch (result.kind) {
    case "ok":
    case "unauthenticated":
      return;
    case "csrf-expired":
      throw new Error(CSRF_EXPIRED_MESSAGE);
    case "failed":
      throw new Error("Unable to sign out. Please try again.");
  }
}
