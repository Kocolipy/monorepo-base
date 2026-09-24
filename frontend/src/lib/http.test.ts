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

  it("sends the session cookie and no CSRF header on a safe request", async () => {
    setCookie("token-1");
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null)));

    await apiFetch("/api/count");

    expect(fetch).toHaveBeenCalledWith("/api/count", { credentials: "include" });
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

  it("re-seeds the cookie and retries once after a 403", async () => {
    setCookie("stale");
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(new Response(null, { status: 403 }))
      .mockImplementationOnce(() => {
        setCookie("fresh");
        return Promise.resolve(new Response(null, { status: 401 }));
      })
      .mockResolvedValueOnce(Response.json({ count: 1 }));

    vi.stubGlobal("fetch", fetchMock);

    const response = await apiFetch("/api/count/increment", { method: "POST" });

    expect(response.status).toBe(200);
    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(fetchMock).toHaveBeenNthCalledWith(2, "/api/auth/me", { credentials: "include" });
    expect(fetchMock).toHaveBeenNthCalledWith(3, "/api/count/increment", {
      credentials: "include",
      headers: { "X-XSRF-TOKEN": "fresh" },
      method: "POST",
    });
  });

  it("gives up after a second 403 rather than looping", async () => {
    setCookie("stale");
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 403 }));
    vi.stubGlobal("fetch", fetchMock);

    const response = await apiFetch("/api/count/increment", { method: "POST" });

    expect(response.status).toBe(403);
    expect(fetchMock).toHaveBeenCalledTimes(3);
  });

  it("does not retry a safe request that was forbidden", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 403 }));
    vi.stubGlobal("fetch", fetchMock);

    const response = await apiFetch("/api/count");

    expect(response.status).toBe(403);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("does not retry a 401, which means the session ended", async () => {
    setCookie("token-1");
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 401 }));
    vi.stubGlobal("fetch", fetchMock);

    const response = await apiFetch("/api/auth/logout", { method: "DELETE" });

    expect(response.status).toBe(401);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
});
