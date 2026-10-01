import { apiFetch, CSRF_EXPIRED_MESSAGE, type ApiResult } from "@/lib/http";

export type AuthRole = "USER" | "ADMIN";

export interface AuthUser {
  /**
   * `null` while `passwordChangeRequired` is set: a confined session holds no
   * role at all, an Admin's included, until the credential is replaced.
   */
  role: AuthRole | null;
  /** The change-required flag: this session may only change its password or log out. */
  passwordChangeRequired: boolean;
  username: string;
}

interface UserResponse {
  passwordChangeRequired?: boolean;
  role?: AuthRole | null;
  username: string;
}

/**
 * The `/me` and login response, read the same way for both.
 *
 * The flag is read strictly — only a literal `true` confines — because the SPA's
 * confinement is a convenience for the User, not the boundary: the backend
 * refuses every other endpoint to a flagged session whatever is rendered.
 */
const decodeUser = async (response: Response): Promise<AuthUser> => {
  const body = (await response.json()) as UserResponse;
  return {
    passwordChangeRequired: body.passwordChangeRequired === true,
    role: body.role ?? null,
    username: body.username,
  };
};

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

/**
 * What a self-service password change came to.
 *
 * - `changed` — `204`: the password was replaced and every session of the User
 *   ended, the submitting one included.
 * - `policy-violation` — `400` naming the unmet rule; `message` is the backend's
 *   statement of that rule, which never contains either submitted value.
 * - `current-password-rejected` — `401` while the session survives it.
 * - `locked` — `401` that ended the session: at the lockout threshold the
 *   backend locks the account and revokes every session it holds.
 */
export type PasswordChangeOutcome =
  | { kind: "changed" }
  | { kind: "policy-violation"; message: string }
  | { kind: "current-password-rejected" }
  | { kind: "locked" }
  | { kind: "csrf-expired" }
  | { kind: "failed" };

/** The `PasswordRuleViolation` body's statement of the rule, or nothing for any other body. */
const decodeRuleMessage = async (response: Response): Promise<string | undefined> => {
  const body = (await response.json()) as { message?: unknown };
  return typeof body.message === "string" ? body.message : undefined;
};

/**
 * Tells a lockout from a wrong current password.
 *
 * The backend answers both with the same bodiless `401`, as it does for Login,
 * so the status alone cannot say which. What does differ is the session: a
 * wrong password leaves it standing, while reaching the threshold locks the
 * account and revokes every session the User holds, this one included. So the
 * session is asked whether it still exists.
 */
async function classifyRejection(): Promise<PasswordChangeOutcome> {
  const probe = await apiFetch("/api/auth/me");
  return probe.kind === "unauthenticated"
    ? { kind: "locked" }
    : { kind: "current-password-rejected" };
}

/**
 * Submits the session's own password change.
 *
 * Reached through `src/auth`, not `useSessionRequest`, because a `401` here is
 * an answer about the current password, not necessarily a session that has
 * ended; the seam would end the session on it unconditionally.
 */
export async function changePassword(
  currentPassword: string,
  newPassword: string,
): Promise<PasswordChangeOutcome> {
  const result = await apiFetch(
    "/api/auth/change-password",
    {
      body: JSON.stringify({ currentPassword, newPassword }),
      headers: { "Content-Type": "application/json" },
      method: "POST",
    },
    undefined,
    decodeRuleMessage,
  );

  switch (result.kind) {
    case "ok":
      return { kind: "changed" };
    case "unauthenticated":
      return classifyRejection();
    case "csrf-expired":
      return { kind: "csrf-expired" };
    case "failed":
      return result.status === 400 && result.detail !== undefined
        ? { kind: "policy-violation", message: result.detail }
        : { kind: "failed" };
  }
}
