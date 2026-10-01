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

/**
 * `E` is what a caller-supplied failure decoder reads out of an unsuccessful
 * response's body. It defaults to `never`, so a result requested without one
 * carries no `detail` to read.
 */
export type ApiResult<T, E = never> =
  | { kind: "ok"; data: T }
  | { kind: "unauthenticated" }
  | { kind: "forbidden" }
  | { kind: "csrf-expired" }
  | { kind: "failed"; status?: number; detail?: E };

/**
 * Copy for a `csrf-expired` result, owned here because the condition is a
 * transport one that no feature has an opinion about. Feature-specific copy for
 * a `failed` result stays with the feature that knows what failed.
 */
export const CSRF_EXPIRED_MESSAGE = "Your security token expired. Please try again.";

/**
 * Copy for a `forbidden` result: the backend refused the request on
 * authorization, so retrying it will not help. Owned here beside
 * `CSRF_EXPIRED_MESSAGE` so every feature says the same thing.
 */
export const FORBIDDEN_MESSAGE = "You don't have permission to do this.";

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

/**
 * A failure body is evidence, not a contract: one that does not decode leaves
 * the result a plain `failed` with its status, rather than turning it into a
 * transport failure that has lost the status too.
 */
async function readDetail<E>(response: Response, decode: ApiDecoder<E>): Promise<E | undefined> {
  try {
    return await decode(response);
  } catch {
    return undefined;
  }
}

/**
 * Asks the backend for a fresh CSRF cookie, and says whether it answered.
 *
 * A `401` counts as answered: a guest's `GET /api/auth/me` is refused but
 * still carries a freshly issued `XSRF-TOKEN`, and that is exactly the seed a
 * guest's first `POST /api/auth/login` needs. Any other unsuccessful status,
 * or no response at all, means no fresh token can be assumed.
 */
async function reseedCsrf(): Promise<boolean> {
  try {
    const seed = await fetch(CSRF_SEED_PATH, { credentials: "include" });
    return seed.ok || seed.status === 401;
  } catch {
    return false;
  }
}

export function apiFetch(path: string, init?: ApiRequestInit): Promise<ApiResult<void>>;
export function apiFetch<T>(
  path: string,
  init: ApiRequestInit,
  decode: ApiDecoder<T>,
): Promise<ApiResult<T>>;
export function apiFetch<T, E>(
  path: string,
  init: ApiRequestInit,
  decode: ApiDecoder<T> | undefined,
  decodeFailure: ApiDecoder<E>,
): Promise<ApiResult<T, E>>;

/**
 * Performs an API request and returns its meaning rather than a raw response.
 *
 * A `403` is either a missing or stale CSRF token or an authorization refusal.
 * CSRF applies only to unsafe methods, so a safe request's `403` is `forbidden`
 * at once. An unsafe request re-seeds the token and retries exactly once; a
 * `403` on the retry was sent with a token just issued, so it too is
 * `forbidden`. `csrf-expired` is left for the one case where the token could
 * not be re-seeded at all. Successful body decoding is explicit, so no-content
 * responses remain type-safe, and so is failure body decoding: only a caller
 * passing `decodeFailure` gets a `detail`.
 */
export async function apiFetch<T, E>(
  path: string,
  init: ApiRequestInit = {},
  decode?: ApiDecoder<T>,
  decodeFailure?: ApiDecoder<E>,
): Promise<ApiResult<T | void, E>> {
  try {
    let response = await fetch(path, withCsrf(init));
    if (response.status === 403 && isUnsafe(init.method)) {
      if (!(await reseedCsrf())) return { kind: "csrf-expired" };
      response = await fetch(path, withCsrf(init));
    }

    if (response.status === 401) return { kind: "unauthenticated" };
    if (response.status === 403) return { kind: "forbidden" };
    if (!response.ok) {
      if (decodeFailure === undefined) return { kind: "failed", status: response.status };
      return {
        kind: "failed",
        status: response.status,
        detail: await readDetail(response, decodeFailure),
      };
    }
    if (decode === undefined) return { kind: "ok", data: undefined };

    return { kind: "ok", data: await decode(response) };
  } catch {
    return { kind: "failed" };
  }
}
