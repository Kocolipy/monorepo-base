/**
 * The one place the SPA talks to the backend.
 *
 * The backend enforces CSRF with the double-submit pattern documented in
 * `backend/FRONTEND.md`: the `XSRF-TOKEN` cookie is deliberately not
 * `HttpOnly`, and every unsafe request has to echo its value in the
 * `X-XSRF-TOKEN` header or come back `403`.
 */

const CSRF_COOKIE = "XSRF-TOKEN";
const CSRF_HEADER = "X-XSRF-TOKEN";

/** Methods the backend exempts from the CSRF check. */
const SAFE_METHODS = new Set(["GET", "HEAD", "OPTIONS"]);

/**
 * A safe request whose response re-seeds the cookie. Any response does, so the
 * cheapest anonymous-friendly endpoint is enough — a `401` still carries the
 * `Set-Cookie`.
 */
const CSRF_SEED_PATH = "/api/auth/me";

/** Plain-object headers only, so a request can be merged without a `Headers` copy. */
export type ApiRequestInit = Omit<RequestInit, "headers"> & {
  headers?: Record<string, string>;
};

/**
 * The current CSRF token, read from `document.cookie` at call time.
 *
 * Never cache this: login and logout both rotate the token, so a value captured
 * at start-up or held in React state goes stale on the next sign-in.
 */
export function csrfToken(): string | undefined {
  return document.cookie
    .split("; ")
    .find((entry) => entry.startsWith(`${CSRF_COOKIE}=`))
    ?.split("=")[1];
}

const isUnsafe = (method?: string): boolean => !SAFE_METHODS.has((method ?? "GET").toUpperCase());

function withCsrf(init: ApiRequestInit): RequestInit {
  if (!isUnsafe(init.method)) return { credentials: "include", ...init };

  const token = csrfToken();
  return {
    credentials: "include",
    ...init,
    headers: { ...init.headers, ...(token === undefined ? {} : { [CSRF_HEADER]: token }) },
  };
}

/**
 * `fetch` with the session cookie, the CSRF header, and one retry.
 *
 * A `403` on an unsafe request means the token was missing, stale, or rotated
 * underneath us — not that the session ended. So it is retried exactly once
 * after a safe request re-seeds the cookie, which also covers the cold start
 * where `POST /api/auth/login` is the tab's very first API call. A second `403`
 * is handed back to the caller unchanged; `401` is never retried, because that
 * one really is "sign in again".
 */
export async function apiFetch(path: string, init: ApiRequestInit = {}): Promise<Response> {
  const response = await fetch(path, withCsrf(init));
  if (response.status !== 403 || !isUnsafe(init.method)) return response;

  await fetch(CSRF_SEED_PATH, { credentials: "include" });
  return fetch(path, withCsrf(init));
}
