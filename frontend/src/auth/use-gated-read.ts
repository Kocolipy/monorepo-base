/**
 * The one Permission-gated read every page's listing goes through.
 *
 * A page names a path, a decoder, the Permission the backend requires for the
 * read, and the copy for a plain failure; the hook does the rest. Without the
 * Permission no request is sent at all — the read would only be refused — and
 * with it the result lands as data or as a refusal with its message, the
 * message read through the seam's own {@link refusalMessage}.
 */

import { useCallback, useEffect, useRef, useState } from "react";

import type { ApiDecoder } from "@/lib/http";

import type { Permission } from "./api";
import { useAuth } from "./auth-context-value";
import { holds } from "./permissions";
import { refusalMessage, useSessionRequest, type SessionResult } from "./use-session-request";

export interface GatedReadOptions<T> {
  path: string;
  /** Must be a stable reference (module-level): the read reruns when it changes. */
  decode: ApiDecoder<T>;
  /** The Permission the read requires; the request is sent only when the session holds it. */
  permission: Permission;
  /** The copy for a plain `failed` result, which only the page can describe. */
  failureMessage: string;
}

export interface GatedRead<T> {
  /** The last successful read, or `null` before one — and while gated off. */
  data: T | null;
  /** Whether the latest read was refused or failed. */
  failed: boolean;
  /** The latest read's refusal copy, or `null`. */
  error: string | null;
  /** Whether the first read for the current path is still in flight. */
  loading: boolean;
  /** Reads again, keeping the current data if the re-read is refused. */
  reload: () => Promise<void>;
  /** Replaces the data locally, from a response that already states it. */
  update: (next: (current: T | null) => T | null) => void;
  /** Withdraws the refusal copy, when a later action supersedes it on the page. */
  clearError: () => void;
}

interface ReadState<T> {
  data: T | null;
  error: string | null;
  failed: boolean;
  settled: boolean;
}

const UNREAD: ReadState<never> = { data: null, error: null, failed: false, settled: false };

/**
 * The read state, tied to what it was read for. When the gate closes or the
 * path moves, the old answer is not this read's answer, so it is dropped during
 * the render that notices — never painted alongside the new key.
 */
function useKeyedState<T>(key: string | null) {
  const [state, setState] = useState<ReadState<T>>(UNREAD);
  const [readFor, setReadFor] = useState(key);
  if (readFor !== key) {
    setReadFor(key);
    setState(UNREAD);
  }
  return [state, setState] as const;
}

/**
 * Opens a read whenever `allowed`, and returns the re-read. The open read is
 * `null` while none is (gated off, or unmounted), and a response lands only
 * while its read is still the open one, so an answer for a path the page has
 * moved off, or for a gate that has since closed, is discarded rather than
 * painted.
 */
function useOpenRead<T>(
  allowed: boolean,
  read: () => Promise<SessionResult<T>>,
  land: (result: SessionResult<T>) => void,
): () => Promise<void> {
  const openRead = useRef<object | null>(null);

  useEffect(() => {
    if (!allowed) return;
    const opened = {};
    openRead.current = opened;
    void read().then((result) => {
      if (opened === openRead.current) land(result);
    });
    return () => {
      openRead.current = null;
    };
  }, [allowed, read, land]);

  return useCallback(async () => {
    const opened = openRead.current;
    if (opened === null) return;
    const result = await read();
    if (opened === openRead.current) land(result);
  }, [read, land]);
}

export function useGatedRead<T>({
  decode,
  failureMessage,
  path,
  permission,
}: GatedReadOptions<T>): GatedRead<T> {
  const { user } = useAuth();
  const allowed = holds(user, permission);
  const request = useSessionRequest();
  const [state, setState] = useKeyedState<T>(allowed ? path : null);

  const read = useCallback(() => request(path, {}, decode), [decode, path, request]);

  const land = useCallback(
    (result: SessionResult<T>) => {
      if (result.kind === "ok") {
        setState({ data: result.data, error: null, failed: false, settled: true });
        return;
      }
      const error = refusalMessage(result, failureMessage);
      setState((current) => ({ data: current.data, error, failed: true, settled: true }));
    },
    [failureMessage, setState],
  );

  const reload = useOpenRead(allowed, read, land);

  // Neither needs memoising: `setState` is stable, and both are called from
  // event handlers, never named as a dependency.
  const update = (next: (current: T | null) => T | null) => {
    setState((current) => ({ ...current, data: next(current.data) }));
  };

  const clearError = () => {
    setState((current) => ({ ...current, error: null }));
  };

  return {
    clearError,
    data: state.data,
    error: state.error,
    failed: state.failed,
    loading: allowed && !state.settled,
    reload,
    update,
  };
}
