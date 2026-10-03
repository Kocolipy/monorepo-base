import { useCallback, useEffect, useState, type FormEvent, type ReactNode } from "react";

import { refusalMessage, useSessionRequest, type SessionResult } from "@/auth/use-session-request";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { jsonDecoder } from "@/lib/decode";

import {
  connectorPath,
  CONNECTORS_PATH,
  decodeConnector,
  decodeConnectors,
  decodeIssuedToken,
  formatDate,
  jsonBody,
  tokenActionPath,
  tokensPath,
  type Connector,
  type ConnectorToken,
  type IssuedToken,
  type TokenScope,
} from "./accounts-api";

/** Module-level, so each is one stable function across renders and hook dependencies. */
const readConnectors = jsonDecoder(decodeConnectors);
const readConnector = jsonDecoder(decodeConnector);
const readIssuedToken = jsonDecoder(decodeIssuedToken);

const INPUT_CLASS =
  "flex h-9 rounded-md border border-input bg-background px-3 py-1 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring";

/**
 * A plaintext the backend has just disclosed, held in component state and
 * nowhere else — not storage, not the URL, not a cache — so navigating away or
 * reloading loses it for good, which is the point: the backend stored a digest
 * and cannot produce it again either.
 */
interface Disclosure {
  connectorName: string;
  token: IssuedToken;
}

function tokenStatus(token: ConnectorToken): string {
  if (token.revokedAt !== null) return `Revoked ${formatDate(token.revokedAt)}`;
  return token.active ? "Active" : "Expired";
}

/** Copy for a refused request, keyed on what the backend refused. */
function failure(what: string, status?: number): string {
  if (status === 400) return `Refused: ${what} — check the values and try again.`;
  if (status === 404) return `${what} failed: it no longer exists. Reload the page.`;
  return `${what} failed. Please try again.`;
}

function TokenDisclosure({
  disclosure,
  onDismiss,
}: {
  disclosure: Disclosure;
  onDismiss: () => void;
}) {
  return (
    <section
      aria-labelledby="token-disclosure-heading"
      className="flex flex-col gap-2 rounded-md border border-primary p-4"
    >
      <h3 className="font-medium" id="token-disclosure-heading">
        New token for {disclosure.connectorName}
      </h3>
      <p className="text-sm text-muted-foreground">
        Copy it now. It is shown only this once and cannot be retrieved again — not after you
        dismiss this, leave the page or reload it.
      </p>
      <code
        aria-label="New token value"
        className="break-all rounded-md bg-muted px-3 py-2 font-mono text-sm"
      >
        {disclosure.token.presentedValue}
      </code>
      <p className="text-sm text-muted-foreground">
        {disclosure.token.scope} · expires {formatDate(disclosure.token.expiresAt)}
      </p>
      <Button className="self-start" onClick={onDismiss} size="sm" variant="outline">
        Dismiss token
      </Button>
    </section>
  );
}

function ConnectorSection({
  canIssue,
  canManage,
  connector,
  onDelete,
  onIssue,
  onRevoke,
  onRotate,
  pending,
}: {
  /** `connector:token`: issue, rotate and revoke are offered. */
  canIssue: boolean;
  /** `connector:write`: delete is offered. */
  canManage: boolean;
  connector: Connector;
  onDelete: () => void;
  onIssue: (scope: TokenScope, lifetimeDays: number | null) => void;
  onRevoke: (token: ConnectorToken) => void;
  onRotate: (token: ConnectorToken) => void;
  pending: boolean;
}) {
  const [scope, setScope] = useState<TokenScope>("READ_ONLY");
  const [lifetime, setLifetime] = useState("");
  const scopeId = `scope-${connector.id}`;
  const lifetimeId = `lifetime-${connector.id}`;

  const issue = (event: FormEvent) => {
    event.preventDefault();
    onIssue(scope, lifetime === "" ? null : Number(lifetime));
  };

  return (
    <section
      aria-label={`Connector ${connector.displayName}`}
      className="flex flex-col gap-3 border-t pt-4"
    >
      <div className="flex items-center justify-between gap-4">
        <div>
          <h3 className="font-medium">{connector.displayName}</h3>
          <p className="text-sm text-muted-foreground">Created {formatDate(connector.createdAt)}</p>
        </div>
        {canManage ? (
          <Button disabled={pending} onClick={onDelete} size="sm" variant="destructive">
            Delete {connector.displayName}
          </Button>
        ) : null}
      </div>

      {connector.tokens.length === 0 ? (
        <p className="text-sm text-muted-foreground">No tokens issued.</p>
      ) : (
        <table className="w-full border-collapse text-left text-sm">
          <caption className="sr-only">Tokens of {connector.displayName}</caption>
          <thead>
            <tr className="border-b text-muted-foreground">
              {["Token", "Scope", "Issued", "Expires", "Status", "Actions"].map((heading) => (
                <th className="py-2 pr-4 font-medium" key={heading} scope="col">
                  {heading}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {connector.tokens.map((token) => (
              <tr className="border-b last:border-0" key={token.id}>
                <th className="py-2 pr-4 font-mono font-normal" scope="row">
                  {token.id.slice(0, 8)}
                </th>
                <td className="py-2 pr-4">{token.scope}</td>
                <td className="py-2 pr-4 text-muted-foreground">{formatDate(token.issuedAt)}</td>
                <td className="py-2 pr-4 text-muted-foreground">{formatDate(token.expiresAt)}</td>
                <td className="py-2 pr-4">{tokenStatus(token)}</td>
                <td className="py-2">
                  {token.active && canIssue ? (
                    <span className="flex gap-2">
                      <Button
                        disabled={pending}
                        onClick={() => onRotate(token)}
                        size="sm"
                        title="Issues a replacement and ends this token immediately"
                        variant="outline"
                      >
                        Rotate token {token.id.slice(0, 8)}
                      </Button>
                      <Button
                        disabled={pending}
                        onClick={() => onRevoke(token)}
                        size="sm"
                        variant="outline"
                      >
                        Revoke token {token.id.slice(0, 8)}
                      </Button>
                    </span>
                  ) : null}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {canIssue ? (
        <form className="flex flex-wrap items-end gap-3" onSubmit={issue}>
          <div className="flex flex-col gap-1">
            <label className="text-sm font-medium" htmlFor={scopeId}>
              Scope for {connector.displayName}
            </label>
            <select
              className={INPUT_CLASS}
              id={scopeId}
              onChange={(event) => setScope(event.target.value as TokenScope)}
              value={scope}
            >
              <option value="READ_ONLY">Read only</option>
              <option value="READ_WRITE">Read and write</option>
            </select>
          </div>
          <div className="flex flex-col gap-1">
            <label className="text-sm font-medium" htmlFor={lifetimeId}>
              Lifetime in days for {connector.displayName} (default 365)
            </label>
            <input
              className={INPUT_CLASS}
              id={lifetimeId}
              inputMode="numeric"
              max={365}
              min={1}
              onChange={(event) => setLifetime(event.target.value)}
              type="number"
              value={lifetime}
            />
          </div>
          <Button disabled={pending} size="sm" type="submit">
            Issue token for {connector.displayName}
          </Button>
        </form>
      ) : null}
    </section>
  );
}

/** The four operations on one connector, as its section invokes them. */
interface ConnectorActions {
  onDelete: () => void;
  onIssue: (scope: TokenScope, lifetimeDays: number | null) => void;
  onRevoke: (token: ConnectorToken) => void;
  onRotate: (token: ConnectorToken) => void;
}

/**
 * The listing, the one-time disclosure and every mutation, as state.
 *
 * Every change reloads the listing rather than patching it locally: a rotation
 * shortens the old token, a revocation stamps it, a delete revokes everything
 * the connector held — and the listing is the only answer that states all of
 * that at once.
 */
function useConnectors() {
  const request = useSessionRequest();

  const [connectors, setConnectors] = useState<Connector[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const [disclosure, setDisclosure] = useState<Disclosure | null>(null);
  // A failed read is reported as a failure, never as an empty list.
  const [unread, setUnread] = useState(false);

  const applyFailure = useCallback(
    (result: Exclude<SessionResult<unknown>, { kind: "ok" }>, message: string) => {
      setError(refusalMessage(result, message));
    },
    [],
  );

  const fetchConnectors = useCallback(
    () => request(CONNECTORS_PATH, {}, readConnectors),
    [request],
  );

  const applyListing = useCallback(
    (result: SessionResult<Connector[]>) => {
      if (result.kind === "ok") {
        setConnectors(result.data);
        setUnread(false);
        return;
      }
      setConnectors((current) => current ?? []);
      setUnread(true);
      applyFailure(result, "Unable to load the connectors. Please try again.");
    },
    [applyFailure],
  );

  useEffect(() => {
    void fetchConnectors().then(applyListing);
  }, [applyListing, fetchConnectors]);

  /** One mutation: clear the last error, run it, report a refusal, then re-read. */
  const mutate = async <T,>(
    what: string,
    run: () => Promise<SessionResult<T>>,
    onOk?: (data: T) => void,
  ) => {
    setError(null);
    setPending(true);
    try {
      const result = await run();
      if (result.kind === "ok") {
        onOk?.(result.data);
      } else {
        applyFailure(result, failure(what, result.kind === "failed" ? result.status : undefined));
      }
      applyListing(await fetchConnectors());
    } finally {
      setPending(false);
    }
  };

  const create = (displayName: string, onCreated: () => void) =>
    void mutate(
      `Creating ${displayName}`,
      () => request(CONNECTORS_PATH, jsonBody("POST", { displayName }), readConnector),
      onCreated,
    );

  const actionsFor = (connector: Connector): ConnectorActions => {
    const disclose = (token: IssuedToken) =>
      setDisclosure({ connectorName: connector.displayName, token });
    return {
      onDelete: () =>
        void mutate(`Deleting ${connector.displayName}`, () =>
          request(connectorPath(connector.id), { method: "DELETE" }),
        ),
      onIssue: (scope, lifetimeDays) =>
        void mutate(
          `Issuing a token for ${connector.displayName}`,
          () =>
            request(
              tokensPath(connector.id),
              jsonBody("POST", { scope, lifetimeDays }),
              readIssuedToken,
            ),
          disclose,
        ),
      onRevoke: (token) =>
        void mutate("Revoking the token", () =>
          request(tokenActionPath(connector.id, token.id, "revoke"), { method: "POST" }),
        ),
      onRotate: (token) =>
        void mutate(
          "Rotating the token",
          () =>
            request(
              tokenActionPath(connector.id, token.id, "rotate"),
              jsonBody("POST", {}),
              readIssuedToken,
            ),
          disclose,
        ),
    };
  };

  return {
    actionsFor,
    connectors,
    create,
    disclosure,
    dismiss: () => setDisclosure(null),
    error,
    pending,
    unread,
  };
}

function CreateConnectorForm({
  onCreate,
  pending,
}: {
  onCreate: (displayName: string, onCreated: () => void) => void;
  pending: boolean;
}) {
  const [name, setName] = useState("");

  const submit = (event: FormEvent) => {
    event.preventDefault();
    const displayName = name.trim();
    if (displayName !== "") onCreate(displayName, () => setName(""));
  };

  return (
    <form className="flex flex-wrap items-end gap-3" onSubmit={submit}>
      <div className="flex flex-col gap-1">
        <label className="text-sm font-medium" htmlFor="new-connector-name">
          New connector name
        </label>
        <input
          className={INPUT_CLASS}
          id="new-connector-name"
          maxLength={200}
          onChange={(event) => setName(event.target.value)}
          required
          value={name}
        />
      </div>
      <Button disabled={pending} size="sm" type="submit">
        Create connector
      </Button>
    </form>
  );
}

/**
 * Connector and token management: create, list and delete connectors, and
 * issue, rotate and revoke their tokens. A token's plaintext appears exactly
 * once, in the response to the request that minted it, and is shown in
 * {@link TokenDisclosure}.
 *
 * Rendered only for a session holding `connector:read`. Creating and deleting
 * a connector is offered only with `connector:write`, and issuing, rotating
 * and revoking a token only with `connector:token` — each its own Permission
 * on the backend, which refuses them independently.
 */
export function Connectors({
  canIssueTokens,
  canManageConnectors,
}: {
  canIssueTokens: boolean;
  canManageConnectors: boolean;
}) {
  const { actionsFor, connectors, create, disclosure, dismiss, error, pending, unread } =
    useConnectors();

  let listing: ReactNode;
  if (connectors === null) {
    listing = <p className="text-sm text-muted-foreground">Loading connectors…</p>;
  } else if (connectors.length === 0) {
    listing = unread ? null : <p className="text-sm text-muted-foreground">No connectors exist.</p>;
  } else {
    listing = connectors.map((connector) => (
      <ConnectorSection
        canIssue={canIssueTokens}
        canManage={canManageConnectors}
        connector={connector}
        key={connector.id}
        pending={pending}
        {...actionsFor(connector)}
      />
    ));
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Connectors</CardTitle>
        <CardDescription>
          The directory integrations allowed to provision over SCIM, and the bearer tokens they
          authenticate with. A token&apos;s value is shown once, when it is issued or rotated, and
          can never be retrieved again.
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {error ? (
          <p className="text-sm text-destructive" role="alert">
            {error}
          </p>
        ) : null}
        {disclosure ? <TokenDisclosure disclosure={disclosure} onDismiss={dismiss} /> : null}
        {canManageConnectors ? <CreateConnectorForm onCreate={create} pending={pending} /> : null}
        {listing}
      </CardContent>
    </Card>
  );
}
