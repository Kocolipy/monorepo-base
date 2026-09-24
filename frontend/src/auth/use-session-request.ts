/**
 * The seam feature modules make requests through.
 *
 * `apiFetch` classifies a `401` as `unauthenticated`; responding to that is a
 * session concern, not a feature one, so it is handled here once. Callers
 * receive a `SessionResult`, which has no `unauthenticated` member — there is
 * no case for a future page to forget.
 */

import { useCallback } from "react";

import { apiFetch, type ApiDecoder, type ApiRequestInit, type ApiResult } from "@/lib/http";

import { useAuthState } from "./auth-context-value";

/**
 * Re-exported so a feature reads the whole result contract — the cases and the
 * copy for the one case it does not own — from this seam alone.
 */
export { CSRF_EXPIRED_MESSAGE } from "@/lib/http";

/** An `ApiResult` whose session outcome the seam has already handled. */
export type SessionResult<T> = Exclude<ApiResult<T>, { kind: "unauthenticated" }>;

export interface SessionRequest {
  (path: string, init?: ApiRequestInit): Promise<SessionResult<void>>;
  <T>(path: string, init: ApiRequestInit, decode: ApiDecoder<T>): Promise<SessionResult<T>>;
}

export function useSessionRequest(): SessionRequest {
  const { expireSession } = useAuthState();

  return useCallback(
    async <T>(
      path: string,
      init: ApiRequestInit = {},
      decode?: ApiDecoder<T>,
    ): Promise<SessionResult<T | void>> => {
      const result =
        decode === undefined ? await apiFetch(path, init) : await apiFetch(path, init, decode);

      if (result.kind === "unauthenticated") {
        // The session is ended here; the route guard redirects to the login
        // route on the same update, so whatever the caller does with this
        // result is superseded before it can paint.
        expireSession();
        return { kind: "failed", status: 401 };
      }
      return result;
    },
    [expireSession],
  ) as SessionRequest;
}
