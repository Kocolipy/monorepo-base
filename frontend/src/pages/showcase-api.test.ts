import { afterEach, describe, expect, it, vi } from "vitest";

import { getCount, incrementCount, resetCount } from "./showcase-api";

describe("showcase counter API", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("gets the authenticated user's current counter", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json({ count: 3 })));

    await expect(getCount()).resolves.toBe(3);
    expect(fetch).toHaveBeenCalledWith("/api/count", { credentials: "include" });
  });

  it("increments the authenticated user's counter", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json({ count: 4 })));

    await expect(incrementCount()).resolves.toBe(4);
    expect(fetch).toHaveBeenCalledWith("/api/count/increment", {
      credentials: "include",
      method: "POST",
    });
  });

  it("resets the authenticated user's counter", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json({ count: 0 })));

    await expect(resetCount()).resolves.toBe(0);
    expect(fetch).toHaveBeenCalledWith("/api/count/reset", {
      credentials: "include",
      method: "POST",
    });
  });

  it("rejects unsuccessful counter updates", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 503 })));

    await expect(incrementCount()).rejects.toThrow(
      "Unable to update the counter. Please try again.",
    );
  });

  it("rejects an unsuccessful initial counter read", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 503 })));

    await expect(getCount()).rejects.toThrow("Unable to load the counter. Please try again.");
  });
});
