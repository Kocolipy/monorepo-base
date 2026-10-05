/**
 * The one gated write every page's action goes through.
 *
 * A page names the request (already behind {@link useSessionRequest}), what to
 * do on success, and the copy for the statuses its action cares about; the hook
 * owns the rest — the pending flag, the single error line, mapping a refusal to
 * copy (a transport refusal through the seam's own {@link refusalMessage}, a
 * `failed` one through the page's copy and this hook's defaults), and withdrawing the error
 * of whatever read(s) the write supersedes the moment it starts.
 *
 * ADR 0006 holds a protected-resource refusal to `400 scimType: mutability`,
 * distinct from a generic `400`. None of `AdminAccountController` or
 * `AdminConnectorController` — the endpoints Accounts, Connectors and Showcase
 * call — ever answers that shape today: `mutability` is scoped to the SCIM
 * write surface (`ScimExceptionHandler`), which these pages never reach. The
 * hook still keys its own default for it, and a page's `messages.mutability`
 * overrides that default the same way `messages[404]` overrides the hook's
 * `404` — so the distinction exists the moment a page's endpoint starts
 * answering it, with nothing to add here first. A result only carries the
 * `detail` this reads when the page's request asked `apiFetch` to decode a
 * failure body; none of today's three pages do, so the branch is reachable but
 * unexercised by them.
 *
 * Precedence when a `failed` result's message is resolved: the mutability
 * default always wins over a generic `400` default, because ADR 0006 holds the
 * two apart however a page configures its copy. Short of that, a page's own
 * `messages[status]` wins, then its own `messages.default`, then this hook's
 * per-status default, then this hook's own last resort — so a page's `default`
 * stands for every status it does not name, exactly as it did before this hook
 * existed.
 */

import { useState } from "react";

import { readObject } from "@/lib/decode";
import type { ApiResult } from "@/lib/http";

import { refusalMessage } from "./use-session-request";

/**
 * What a write's request settles to: a `SessionResult` whose `failed` member
 * may carry a decoded failure body of type `E`. `E` is `never` — no body —
 * unless the request asked `apiFetch` to decode one, so a plain
 * `SessionResult<T>` is one of these as it stands.
 */
export type WriteResult<T, E = never> = Exclude<ApiResult<T, E>, { kind: "unauthenticated" }>;

/** The subset of a `GatedRead` a write supersedes: cleared on start, consulted as the fallback. */
export interface SupersededRead {
  readonly error: string | null;
  readonly clearError: () => void;
}

/** Copy for a `failed` refusal, keyed by status; `default` covers everything unmapped. */
export type RefusalMessages = Partial<Record<number, string>> & {
  default?: string;
  /** ADR 0006's protected-resource refusal — `400` with `scimType: "mutability"` — kept apart from a generic `400`. */
  mutability?: string;
};

export interface RunOptions<T> {
  /** The copy a page cares about for this action; falls back to the hook's own defaults. */
  messages?: RefusalMessages;
  /** Called once, only on success, with the decoded data. */
  onOk?: (data: T) => void | Promise<void>;
  /**
   * Runs after the request settles — success or refusal — while still inside
   * the pending window: a listing reload, say. Supplying it marks the write as
   * one whose `supersedes` is the latest news for as long as this hook lives:
   * `error` then shows the first non-null error among `supersedes` ahead of
   * this request's own, since whatever `after` touches is read live and is
   * necessarily the newer of the two.
   */
  after?: () => Promise<void>;
}

export interface GatedWriteOptions {
  /**
   * The read(s) this write supersedes: each has its error withdrawn the moment
   * a write starts, and the first of their current errors is this write's
   * fallback — shown before any write has run, and again once a write's own
   * outcome has nothing to say.
   */
  supersedes?: readonly SupersededRead[];
}

export interface GatedWrite {
  /** Whether a write is in flight; every control a write drives disables on this. */
  pending: boolean;
  /** The single error line: this write's own outcome, or the fallback described above. */
  error: string | null;
  /** Runs one write. Never rejects: a request that throws is the caller's bug, not this hook's to catch. */
  run: <T, E = never>(
    request: () => Promise<WriteResult<T, E>>,
    options?: RunOptions<T>,
  ) => Promise<void>;
}

/** Copy for a status this hook recognises without a page naming its own. */
const DEFAULT_REFUSAL_MESSAGES: Record<number, string> = {
  400: "Refused: check the values and try again.",
  404: "It no longer exists. Reload the page for the current list.",
  409: "Refused: the request conflicts with the resource's current state.",
};

/** ADR 0006's default for a protected-resource refusal, distinct from a generic `400`. */
const DEFAULT_MUTABILITY_MESSAGE = "This can't be changed. Reload the page for its current state.";

/** The last resort, when neither the page nor the hook names anything for the status. */
const DEFAULT_REFUSAL_MESSAGE = "Unable to complete the action. Please try again.";

/**
 * Whether a decoded failure body is ADR 0006's protected-resource shape:
 * `scimType: "mutability"`. `detail` is `undefined` when the page's request
 * never asked `apiFetch` to decode a failure body at all; that, and any body
 * the decoder refuses, is simply not this shape.
 */
function isMutabilityRefusal(detail: unknown): boolean {
  try {
    readObject(detail, "refusal").oneOf("scimType", ["mutability"]);
    return true;
  } catch {
    return false;
  }
}

function messageFor<E>(
  result: Extract<WriteResult<unknown, E>, { kind: "failed" }>,
  messages: RefusalMessages,
): string {
  if (isMutabilityRefusal(result.detail)) {
    return messages.mutability ?? DEFAULT_MUTABILITY_MESSAGE;
  }
  const { status } = result;
  if (status !== undefined) {
    const named = messages[status] ?? messages.default ?? DEFAULT_REFUSAL_MESSAGES[status];
    if (named !== undefined) return named;
  }
  return messages.default ?? DEFAULT_REFUSAL_MESSAGE;
}

/** The first non-null error among the reads a write supersedes, or `null` when none has one. */
function firstError(supersedes: readonly SupersededRead[]): string | null {
  for (const dependency of supersedes) {
    if (dependency.error !== null) return dependency.error;
  }
  return null;
}

export function useGatedWrite({ supersedes = [] }: GatedWriteOptions = {}): GatedWrite {
  const [ownError, setOwnError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  // Whether the most recent run supplied an `after` step: when it did, that
  // step's own outcome (read live off `supersedes` below, never off a value
  // captured before it ran) is the latest event and is checked first. A page
  // that never passes `after` never flips this, so its own result stays
  // first — the two pages' real precedences, read reactively rather than
  // frozen into state the moment `after` resolves. State, not a ref: the
  // value feeds straight into what this render returns, and a ref read
  // during render is not guaranteed to reflect the latest commit.
  const [afterIsLatest, setAfterIsLatest] = useState(false);

  const run = async <T, E = never>(
    request: () => Promise<WriteResult<T, E>>,
    { after, messages = {}, onOk }: RunOptions<T> = {},
  ): Promise<void> => {
    for (const dependency of supersedes) dependency.clearError();
    setAfterIsLatest(after !== undefined);
    setOwnError(null);
    setPending(true);
    try {
      const result = await request();
      if (result.kind === "ok") {
        await onOk?.(result.data);
      } else if (result.kind === "failed") {
        setOwnError(messageFor(result, messages));
      } else {
        // `forbidden` or `csrf-expired`: the seam's own copy, whatever the page names.
        setOwnError(refusalMessage(result, DEFAULT_REFUSAL_MESSAGE));
      }
      if (after) await after();
    } finally {
      setPending(false);
    }
  };

  const supersededError = firstError(supersedes);
  return {
    error: afterIsLatest ? (supersededError ?? ownError) : (ownError ?? supersededError),
    pending,
    run,
  };
}
