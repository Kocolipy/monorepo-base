import { vi } from "vitest";

import { discardCsrfToken } from "./http";

/** The token a stubbed `GET /api/auth/csrf` hands out. */
const TEST_CSRF_TOKEN = "test-token";

/** The header that stub names, as the backend's session repository does. */
const TEST_CSRF_HEADER = "X-CSRF-TOKEN";

type Backend = (input: string, init?: RequestInit) => Promise<Response>;

/**
 * Stubs the global `fetch` with `backend`, behind a stand-in for the CSRF token
 * endpoint, and forgets any token an earlier test left in memory.
 *
 * For a test about something other than CSRF: `apiFetch` fetches a token before
 * its first unsafe request, and answering that here keeps `backend` — usually a
 * `mockResolvedValueOnce` sequence — seeing exactly the requests the test is
 * about. Assert on `backend` for those; the returned mock records every call,
 * the token fetches included.
 */
export function stubFetchWithCsrf(backend: Backend) {
  discardCsrfToken();
  const outer = vi.fn((input: string, init?: RequestInit) =>
    input === "/api/auth/csrf"
      ? Promise.resolve(Response.json({ headerName: TEST_CSRF_HEADER, token: TEST_CSRF_TOKEN }))
      : backend(input, init),
  );
  vi.stubGlobal("fetch", outer);
  return outer;
}
