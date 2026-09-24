/**
 * The one place the SPA talks to the backend.
 *
 * It owns session credentials, CSRF recovery, HTTP status meaning, and successful
 * response decoding. Feature modules receive semantic results rather than raw
 * `Response` objects.
 */

const CSRF_COOKIE = "XSRF-TOKEN";
const CSRF_HEADER = "X-XSRF-TOKEN";

/** Methods the backend exempts from the CSRF check. */
const SAFE_METHODS = new Set(["GET", "HEAD", "OPTIONS"]);

/** A safe request whose response re-seeds the CSRF cookie. */
const CSRF_SEED_PATH = "/api/auth/me";

/** Plain-object headers only, so a request can be merged without a `Headers` copy. */
export type ApiRequestInit = Omit<RequestInit, "headers"> & {
  headers?: Record<string, string>;
};

export type ApiResult<T> =
  | { kind: "ok"; data: T }
  | { kind: "unauthenticated" }
  | { kind: "csrf-expired" }
  | { kind: "failed"; status?: number };

/**
 * Copy for a `csrf-expired` result, owned here because the condition is a
 * transport one that no feature has an opinion about. Feature-specific copy for
 * a `failed` result stays with the feature that knows what failed.
 */
export const CSRF_EXPIRED_MESSAGE = "Your security token expired. Please try again.";

export type ApiDecoder<T> = (response: Response) => Promise<T> | T;

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

export function apiFetch(path: string, init?: ApiRequestInit): Promise<ApiResult<void>>;
export function apiFetch<T>(
  path: string,
  init: ApiRequestInit,
  decode: ApiDecoder<T>,
): Promise<ApiResult<T>>;

/**
 * Performs an API request and returns its meaning rather than a raw response.
 *
 * Unsafe requests retry exactly once after a `403` and CSRF re-seed. The final
 * response is then classified consistently for every feature. Successful body
 * decoding is explicit, so no-content responses remain type-safe.
 */
export async function apiFetch<T>(
  path: string,
  init: ApiRequestInit = {},
  decode?: ApiDecoder<T>,
): Promise<ApiResult<T | void>> {
  try {
    let response = await fetch(path, withCsrf(init));
    if (response.status === 403 && isUnsafe(init.method)) {
      await fetch(CSRF_SEED_PATH, { credentials: "include" });
      response = await fetch(path, withCsrf(init));
    }

    if (response.status === 401) return { kind: "unauthenticated" };
    if (response.status === 403) return { kind: "csrf-expired" };
    if (!response.ok) return { kind: "failed", status: response.status };
    if (decode === undefined) return { kind: "ok", data: undefined };

    return { kind: "ok", data: await decode(response) };
  } catch {
    return { kind: "failed" };
  }
}
