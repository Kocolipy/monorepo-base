import { act, renderHook } from "@testing-library/react";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";

import { CSRF_EXPIRED_MESSAGE, FORBIDDEN_MESSAGE } from "@/lib/http";

import type { SessionResult } from "./use-session-request";
import { useGatedWrite, type SupersededRead } from "./use-gated-write";

/**
 * A write plus the one read it supersedes, as a session's own `useState`
 * would carry it — a plain object with a getter never schedules the
 * re-render a real `GatedRead`'s state update always does, so a test
 * exercising the interplay with `supersedes` needs real state behind it.
 */
function useHarness() {
  const [readError, setReadError] = useState<string | null>(null);
  const clearError = vi.fn(() => setReadError(null));
  const read: SupersededRead = { clearError, error: readError };
  const write = useGatedWrite({ supersedes: [read] });
  return { clearError, setReadError, write };
}

/** A request held open until the test answers it. */
function deferredRequest<T>() {
  let settle!: (result: SessionResult<T>) => void;
  const promise = new Promise<SessionResult<T>>((resolve) => {
    settle = resolve;
  });
  return { request: () => promise, settle };
}

/** An `after` step held open until the test lets it resolve. */
function deferredAfter() {
  let settle!: () => void;
  const promise = new Promise<void>((resolve) => {
    settle = resolve;
  });
  return { after: () => promise, settle };
}

describe("useGatedWrite", () => {
  it("is not pending, with no error, before any write runs", () => {
    const { result } = renderHook(() => useGatedWrite());

    expect(result.current).toMatchObject({ error: null, pending: false });
  });

  it("is pending while the request is in flight, and not once it settles", async () => {
    const { result } = renderHook(() => useGatedWrite());
    const { request, settle } = deferredRequest<number>();

    let ran: Promise<void> = Promise.resolve();
    act(() => {
      ran = result.current.run(request);
    });
    expect(result.current.pending).toBe(true);

    await act(async () => {
      settle({ data: 1, kind: "ok" });
      await ran;
    });
    expect(result.current.pending).toBe(false);
  });

  it("calls the success handler exactly once, with the decoded data", async () => {
    const { result } = renderHook(() => useGatedWrite());
    const onOk = vi.fn();

    await act(() => result.current.run(() => Promise.resolve({ data: 42, kind: "ok" }), { onOk }));

    expect(onOk).toHaveBeenCalledTimes(1);
    expect(onOk).toHaveBeenCalledWith(42);
    expect(result.current.error).toBeNull();
  });

  it.each([
    [400, "Refused: check the values and try again."],
    [404, "It no longer exists. Reload the page for the current list."],
    [409, "Refused: the request conflicts with the resource's current state."],
    [503, "Unable to complete the action. Please try again."],
  ])("maps a %i refusal to the hook's own default copy", async (status, message) => {
    const { result } = renderHook(() => useGatedWrite());

    await act(() => result.current.run(() => Promise.resolve({ kind: "failed", status })));

    expect(result.current.error).toBe(message);
  });

  it("maps forbidden and csrf-expired through the shared seam copy, not a page's own", async () => {
    const { result } = renderHook(() => useGatedWrite());

    const messages = { 403: "a page's 403 sentence", default: "a page's default sentence" };

    await act(() => result.current.run(() => Promise.resolve({ kind: "forbidden" }), { messages }));
    expect(result.current.error).toBe(FORBIDDEN_MESSAGE);

    await act(() =>
      result.current.run(() => Promise.resolve({ kind: "csrf-expired" }), { messages }),
    );
    expect(result.current.error).toBe(CSRF_EXPIRED_MESSAGE);
  });

  it("lets a page's own copy for a status win over the hook's default", async () => {
    const { result } = renderHook(() => useGatedWrite());

    await act(() =>
      result.current.run(() => Promise.resolve({ kind: "failed", status: 404 }), {
        messages: { 404: "grace no longer exists. Reload the page for the current list." },
      }),
    );

    expect(result.current.error).toBe(
      "grace no longer exists. Reload the page for the current list.",
    );
  });

  it("lets a page's own default win over the hook's, for a status neither names", async () => {
    const { result } = renderHook(() => useGatedWrite());

    await act(() =>
      result.current.run(() => Promise.resolve({ kind: "failed", status: 503 }), {
        messages: { default: "Unable to update the counter. Please try again." },
      }),
    );

    expect(result.current.error).toBe("Unable to update the counter. Please try again.");
  });

  it.each([400, 404, 409])(
    "lets a page's own default win over the hook's own default for a %i refusal",
    async (status) => {
      const { result } = renderHook(() => useGatedWrite());

      await act(() =>
        result.current.run(() => Promise.resolve({ kind: "failed", status }), {
          messages: { default: "Unable to update the counter. Please try again." },
        }),
      );

      expect(result.current.error).toBe("Unable to update the counter. Please try again.");
    },
  );

  it("maps an undefined status to the hook's own last resort, not a page's per-status copy", async () => {
    const { result } = renderHook(() => useGatedWrite());

    await act(() =>
      result.current.run(() => Promise.resolve({ kind: "failed" }), {
        messages: { 400: "a 400-specific sentence that must not show here" },
      }),
    );

    expect(result.current.error).toBe("Unable to complete the action. Please try again.");
  });

  it("maps a protected-resource (`scimType: mutability`) refusal to its own default, distinct from a generic 400", async () => {
    const { result } = renderHook(() => useGatedWrite());

    await act(() =>
      result.current.run(() =>
        Promise.resolve({
          detail: { scimType: "mutability" },
          kind: "failed",
          status: 400,
        }),
      ),
    );

    expect(result.current.error).toBe(
      "This can't be changed. Reload the page for its current state.",
    );
  });

  it("maps a 400 with a decoded detail that is not `scimType: mutability` to the generic 400 copy", async () => {
    const { result } = renderHook(() => useGatedWrite());

    await act(() =>
      result.current.run(() =>
        Promise.resolve({
          detail: { scimType: "uniqueness" },
          kind: "failed",
          status: 400,
        }),
      ),
    );

    expect(result.current.error).toBe("Refused: check the values and try again.");
  });

  it("maps a 400 whose decoded `scimType` is not a string to the generic 400 copy", async () => {
    const { result } = renderHook(() => useGatedWrite());

    await act(() =>
      result.current.run(() =>
        Promise.resolve({ detail: { scimType: 42 }, kind: "failed", status: 400 }),
      ),
    );

    expect(result.current.error).toBe("Refused: check the values and try again.");
  });

  it("maps a 400 with a `null` decoded detail to the generic 400 copy, not a crash", async () => {
    const { result } = renderHook(() => useGatedWrite());

    await act(() =>
      result.current.run(() => Promise.resolve({ detail: null, kind: "failed", status: 400 })),
    );

    expect(result.current.error).toBe("Refused: check the values and try again.");
  });

  it("lets a page's own mutability copy win over the hook's default", async () => {
    const { result } = renderHook(() => useGatedWrite());

    await act(() =>
      result.current.run(
        () =>
          Promise.resolve({
            detail: { scimType: "mutability" },
            kind: "failed",
            status: 400,
          }),
        { messages: { mutability: "The Bootstrap Admin cannot be changed this way." } },
      ),
    );

    expect(result.current.error).toBe("The Bootstrap Admin cannot be changed this way.");
  });

  it("prefers the mutability mapping over a page's own generic default for the same refusal", async () => {
    const { result } = renderHook(() => useGatedWrite());

    await act(() =>
      result.current.run(
        () =>
          Promise.resolve({
            detail: { scimType: "mutability" },
            kind: "failed",
            status: 400,
          }),
        { messages: { default: "Refused: check the values and try again." } },
      ),
    );

    expect(result.current.error).toBe(
      "This can't be changed. Reload the page for its current state.",
    );
  });

  it("withdraws the superseded read's error the moment a write starts", async () => {
    const { result } = renderHook(() => useHarness());
    act(() => result.current.setReadError("stale failure"));
    expect(result.current.write.error).toBe("stale failure");
    const clearErrorSpy = result.current.clearError;
    const { request, settle } = deferredRequest<number>();

    let ran: Promise<void> = Promise.resolve();
    act(() => {
      ran = result.current.write.run(request);
    });

    expect(clearErrorSpy).toHaveBeenCalledTimes(1);
    expect(result.current.write.error).toBeNull();

    await act(async () => {
      settle({ data: 1, kind: "ok" });
      await ran;
    });
  });

  it("falls back to a superseded read's current error before any write has run", () => {
    const { result } = renderHook(() => useHarness());

    act(() => result.current.setReadError("the mount read failed"));

    expect(result.current.write.error).toBe("the mount read failed");
  });

  it("shows its own refusal over a superseded read's stale one, with no `after` step", async () => {
    const { result } = renderHook(() => useHarness());

    await act(() =>
      result.current.write.run(() => Promise.resolve({ kind: "failed", status: 409 })),
    );
    // The read was cleared on start and never touched again by this write, so
    // a later, unrelated change to it does not displace the write's own.
    act(() => result.current.setReadError("a read error unrelated to this write"));

    expect(result.current.write.error).toBe(
      "Refused: the request conflicts with the resource's current state.",
    );
  });

  it("lets a later `after` step's failure replace an earlier successful write's outcome", async () => {
    const { result } = renderHook(() => useHarness());
    const { after, settle: settleAfter } = deferredAfter();

    let ran: Promise<void> = Promise.resolve();
    act(() => {
      ran = result.current.write.run(() => Promise.resolve({ data: undefined, kind: "ok" }), {
        after,
      });
    });

    await act(async () => {
      result.current.setReadError("the reload failed");
      settleAfter();
      await ran;
    });

    expect(result.current.write.error).toBe("the reload failed");
  });

  it("lets an earlier refusal stand when the later `after` step reports nothing new", async () => {
    const { result } = renderHook(() => useHarness());

    await act(() =>
      result.current.write.run(() => Promise.resolve({ kind: "failed", status: 400 }), {
        after: () => Promise.resolve(),
        messages: { 400: "Refused: check the values and try again." },
      }),
    );

    expect(result.current.write.error).toBe("Refused: check the values and try again.");
  });

  it("prefers the latest of two failures: the `after` step's over the write's own", async () => {
    const { result } = renderHook(() => useHarness());
    const { after, settle: settleAfter } = deferredAfter();

    let ran: Promise<void> = Promise.resolve();
    act(() => {
      ran = result.current.write.run(() => Promise.resolve({ kind: "failed", status: 500 }), {
        after,
      });
    });

    await act(async () => {
      result.current.setReadError("the reload failed too, and that is the newer news");
      settleAfter();
      await ran;
    });

    expect(result.current.write.error).toBe("the reload failed too, and that is the newer news");
  });

  it("returns to its own precedence on a later write that supplies no `after`", async () => {
    const { result } = renderHook(() => useHarness());

    // First write passes `after`: the superseded read's error would lead.
    await act(() =>
      result.current.write.run(() => Promise.resolve({ data: undefined, kind: "ok" }), {
        after: () => Promise.resolve(),
      }),
    );
    // Second write passes none: its own refusal must lead again, even though
    // the read still carries an unrelated error from a moment ago.
    act(() => result.current.setReadError("a read error unrelated to this write"));
    await act(() =>
      result.current.write.run(() => Promise.resolve({ kind: "failed", status: 409 })),
    );

    expect(result.current.write.error).toBe(
      "Refused: the request conflicts with the resource's current state.",
    );
  });
});
