import { afterEach, describe, expect, it, vi } from "vitest";

import { apiFetch, csrfToken } from "./http";

function setCookie(value: string) {
  document.cookie = `XSRF-TOKEN=${value}; path=/`;
}

function clearCookies() {
  for (const entry of document.cookie.split("; ")) {
    const name = entry.split("=")[0];
    if (name) document.cookie = `${name}=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT`;
  }
}

const decodeCount = async (response: Response): Promise<number> => {
  const body = (await response.json()) as { count: number };
  return body.count;
};

describe("csrfToken", () => {
  afterEach(clearCookies);

  it("is undefined when the cookie has not been seeded", () => {
    expect(csrfToken()).toBeUndefined();
  });

  it("picks the token out of unrelated cookies", () => {
    document.cookie = "other=first; path=/";
    setCookie("token-1");
    document.cookie = "another=last; path=/";

    expect(csrfToken()).toBe("token-1");
  });
});

describe("apiFetch", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    clearCookies();
  });

  it("sends the session cookie and returns no-content success for a safe request", async () => {
    setCookie("token-1");
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null)));

    await expect(apiFetch("/api/count")).resolves.toEqual({ kind: "ok", data: undefined });
    expect(fetch).toHaveBeenCalledWith("/api/count", { credentials: "include" });
  });

  it("decodes successful response data", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json({ count: 3 })));

    await expect(apiFetch("/api/count", {}, decodeCount)).resolves.toEqual({
      kind: "ok",
      data: 3,
    });
  });

  it("echoes the cookie in the CSRF header on an unsafe request", async () => {
    setCookie("token-1");
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null)));

    await apiFetch("/api/count/increment", { method: "POST" });

    expect(fetch).toHaveBeenCalledWith("/api/count/increment", {
      credentials: "include",
      headers: { "X-XSRF-TOKEN": "token-1" },
      method: "POST",
    });
  });

  it("keeps the caller's own headers alongside the token", async () => {
    setCookie("token-1");
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null)));

    await apiFetch("/api/auth/login", {
      body: "{}",
      headers: { "Content-Type": "application/json" },
      method: "POST",
    });

    expect(fetch).toHaveBeenCalledWith("/api/auth/login", {
      body: "{}",
      credentials: "include",
      headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": "token-1" },
      method: "POST",
    });
  });

  it("omits the header when no token has been seeded yet", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null)));

    await apiFetch("/api/count/increment", { method: "POST" });

    expect(fetch).toHaveBeenCalledWith("/api/count/increment", {
      credentials: "include",
      headers: {},
      method: "POST",
    });
    // The matcher above ignores keys whose value is `undefined`, and a real
    // `fetch` would send such a header as the string "undefined".
    const init = vi.mocked(fetch).mock.calls[0]?.[1];
    expect(Object.keys(init?.headers ?? { missing: true })).toEqual([]);
  });

  it("reads the cookie per request, so a rotated token is picked up", async () => {
    setCookie("token-1");
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null)));

    await apiFetch("/api/auth/logout", { method: "DELETE" });
    setCookie("token-2");
    await apiFetch("/api/auth/logout", { method: "DELETE" });

    expect(fetch).toHaveBeenNthCalledWith(1, "/api/auth/logout", {
      credentials: "include",
      headers: { "X-XSRF-TOKEN": "token-1" },
      method: "DELETE",
    });
    expect(fetch).toHaveBeenNthCalledWith(2, "/api/auth/logout", {
      credentials: "include",
      headers: { "X-XSRF-TOKEN": "token-2" },
      method: "DELETE",
    });
  });

  // 200 is a signed-in session's seed; 401 is a guest's, which still carries a
  // fresh cookie — the case a guest's first login after a lost cookie depends on.
  it.each([200, 401])(
    "re-seeds the cookie and retries once after a 403 (seed %i)",
    async (seedStatus) => {
      setCookie("stale");
      const fetchMock = vi
        .fn()
        .mockResolvedValueOnce(new Response(null, { status: 403 }))
        .mockImplementationOnce(() => {
          setCookie("fresh");
          return Promise.resolve(new Response(null, { status: seedStatus }));
        })
        .mockResolvedValueOnce(Response.json({ count: 1 }));
      vi.stubGlobal("fetch", fetchMock);

      await expect(
        apiFetch("/api/count/increment", { method: "POST" }, decodeCount),
      ).resolves.toEqual({ kind: "ok", data: 1 });
      expect(fetchMock).toHaveBeenCalledTimes(3);
      expect(fetchMock).toHaveBeenNthCalledWith(2, "/api/auth/me", { credentials: "include" });
      expect(fetchMock).toHaveBeenNthCalledWith(3, "/api/count/increment", {
        credentials: "include",
        headers: { "X-XSRF-TOKEN": "fresh" },
        method: "POST",
      });
    },
  );

  it("returns forbidden after a second 403, with exactly one retry", async () => {
    setCookie("stale");
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(new Response(null, { status: 403 }))
      .mockResolvedValueOnce(new Response(null))
      .mockResolvedValueOnce(new Response(null, { status: 403 }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(apiFetch("/api/count/increment", { method: "POST" })).resolves.toEqual({
      kind: "forbidden",
    });
    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(fetchMock).toHaveBeenNthCalledWith(2, "/api/auth/me", { credentials: "include" });
    expect(fetchMock).toHaveBeenNthCalledWith(3, "/api/count/increment", {
      credentials: "include",
      headers: { "X-XSRF-TOKEN": "stale" },
      method: "POST",
    });
  });

  it.each(["GET", "HEAD", "OPTIONS", "get", undefined])(
    "returns forbidden for a 403 on safe method %s without touching the CSRF seed",
    async (method) => {
      const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 403 }));
      vi.stubGlobal("fetch", fetchMock);

      await expect(
        apiFetch("/api/admin/accounts", method === undefined ? {} : { method }),
      ).resolves.toEqual({ kind: "forbidden" });
      expect(fetchMock).toHaveBeenCalledTimes(1);
      expect(fetchMock).not.toHaveBeenCalledWith("/api/auth/me", expect.anything());
    },
  );

  it.each([
    ["answers 500", () => Promise.resolve(new Response(null, { status: 500 }))],
    ["answers 403", () => Promise.resolve(new Response(null, { status: 403 }))],
    ["throws", () => Promise.reject(new TypeError("offline"))],
  ])("returns csrf-expired without retrying when the re-seed %s", async (_, seed) => {
    setCookie("stale");
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(new Response(null, { status: 403 }))
      .mockImplementationOnce(seed)
      .mockResolvedValue(new Response(null));
    vi.stubGlobal("fetch", fetchMock);

    await expect(apiFetch("/api/count/increment", { method: "POST" })).resolves.toEqual({
      kind: "csrf-expired",
    });
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(fetchMock).toHaveBeenNthCalledWith(2, "/api/auth/me", { credentials: "include" });
  });

  it("classifies 401 as unauthenticated without retrying", async () => {
    setCookie("token-1");
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 401 }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(apiFetch("/api/auth/logout", { method: "DELETE" })).resolves.toEqual({
      kind: "unauthenticated",
    });
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("classifies other unsuccessful statuses as failed", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 503 })));

    await expect(apiFetch("/api/count")).resolves.toEqual({ kind: "failed", status: 503 });
  });

  it("classifies network and decoding failures as failed", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValueOnce(new TypeError("offline")));
    await expect(apiFetch("/api/count")).resolves.toEqual({ kind: "failed" });

    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response("not json")));
    await expect(apiFetch("/api/count", {}, decodeCount)).resolves.toEqual({ kind: "failed" });
  });

  const decodeRule = async (response: Response): Promise<string> => {
    const body = (await response.json()) as { rule: string };
    return body.rule;
  };

  it("decodes a failure body only for a caller that asks for one", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(() => Promise.resolve(Response.json({ rule: "TOO_SHORT" }, { status: 400 }))),
    );

    await expect(
      apiFetch("/api/auth/change-password", { method: "POST" }, undefined, decodeRule),
    ).resolves.toStrictEqual({ kind: "failed", status: 400, detail: "TOO_SHORT" });
    // Without a failure decoder the body is not read, and no `detail` key appears.
    await expect(apiFetch("/api/auth/change-password", { method: "POST" })).resolves.toStrictEqual({
      kind: "failed",
      status: 400,
    });
  });

  it("decodes success data alongside a failure decoder", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json({ count: 4 })));

    await expect(apiFetch("/api/count", {}, decodeCount, decodeRule)).resolves.toEqual({
      kind: "ok",
      data: 4,
    });
  });

  it("keeps the status when a failure body does not decode", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response("not json", { status: 400 })));

    await expect(
      apiFetch("/api/auth/change-password", { method: "POST" }, undefined, decodeRule),
    ).resolves.toStrictEqual({ kind: "failed", status: 400, detail: undefined });
  });
});
