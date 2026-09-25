import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";

import type { AuthRole } from "@/auth/api";
import { useAuth } from "@/auth/auth-context-value";
import {
  CSRF_EXPIRED_MESSAGE,
  useSessionRequest,
  type SessionResult,
} from "@/auth/use-session-request";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

/**
 * One row of the **account listing**, exactly as `GET /api/admin/accounts` reports
 * it.
 *
 * `enabled` and `locked` are two separate refusal mechanisms and both are shown:
 * an account serving a lockout right now looks healthy if only `enabled` is
 * rendered, and an administrator would have no way to tell which accounts need
 * unlocking. A lockout carries no expiry: it stands until an administrator
 * unlocks the account, so there is nothing to count down to. `createdAt` is null
 * only for a row written before that column existed.
 */
export interface AdminAccount {
  username: string;
  role: AuthRole;
  enabled: boolean;
  locked: boolean;
  createdAt: string | null;
}

/** The three account actions, named as the backend's path segments. */
type AccountAction = "disable" | "enable" | "unlock";

const decodeAccounts = (response: Response): Promise<AdminAccount[]> =>
  response.json() as Promise<AdminAccount[]>;

const decodeAccount = (response: Response): Promise<AdminAccount> =>
  response.json() as Promise<AdminAccount>;

/**
 * Timestamps are rendered from the ISO instant rather than through
 * `toLocaleString`, so what an administrator reads does not depend on the
 * machine's locale and a test can assert an exact string.
 */
const formatDate = (instant: string | null): string =>
  instant === null ? "—" : instant.slice(0, 10);

/** Copy for a refused action, keyed on what the backend refused. */
function actionFailure(action: AccountAction, username: string, status?: number): string {
  if (status === 409) {
    return `Refused: disabling ${username} would leave nobody able to restore access.`;
  }
  if (status === 404) {
    return `${username} no longer exists. Reload the page for the current list.`;
  }
  return `Unable to ${action} ${username}. Please try again.`;
}

function StatusCell({ account }: { account: AdminAccount }) {
  if (account.enabled && !account.locked) {
    return <span className="text-sm text-muted-foreground">Active</span>;
  }

  return (
    <span className="flex flex-col gap-1">
      {account.enabled ? null : (
        <span className="text-sm font-medium text-destructive">Disabled</span>
      )}
      {account.locked ? <span className="text-sm font-medium text-destructive">Locked</span> : null}
    </span>
  );
}

/**
 * Account administration: the whole listing, and the controls for the two
 * refusal mechanisms behind it.
 *
 * The `ADMIN` route guard is what keeps a `USER` out of this page; the backend
 * restricts `/api/admin/**` to `ROLE_ADMIN` independently, so the guard is a
 * rendering decision and never the authorization.
 */
export function Accounts() {
  const { logout, user } = useAuth();
  const request = useSessionRequest();

  const [accounts, setAccounts] = useState<AdminAccount[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState<string | null>(null);

  const loadAccounts = useCallback(
    () => request("/api/admin/accounts", {}, decodeAccounts),
    [request],
  );

  const applyFailure = useCallback((result: SessionResult<unknown>, failureMessage: string) => {
    setError(result.kind === "csrf-expired" ? CSRF_EXPIRED_MESSAGE : failureMessage);
  }, []);

  useEffect(() => {
    void loadAccounts().then((result) => {
      if (result.kind === "ok") {
        setAccounts(result.data);
        return;
      }
      setAccounts([]);
      applyFailure(result, "Unable to load the accounts. Please try again.");
    });
  }, [applyFailure, loadAccounts]);

  /**
   * Runs one action and replaces just that row from the response, rather than
   * reloading the listing: the response *is* the account's new state, so a
   * refetch would only add a request that could disagree with it.
   */
  const runAction = async (account: AdminAccount, action: AccountAction) => {
    setError(null);
    setPending(account.username);
    try {
      const result = await request(
        `/api/admin/accounts/${account.username}/${action}`,
        { method: "POST" },
        decodeAccount,
      );

      if (result.kind === "ok") {
        const updated = result.data;
        setAccounts((current) =>
          (current ?? []).map((row) => (row.username === updated.username ? updated : row)),
        );
        return;
      }
      applyFailure(
        result,
        actionFailure(
          action,
          account.username,
          result.kind === "failed" ? result.status : undefined,
        ),
      );
    } finally {
      setPending(null);
    }
  };

  const isSelf = (account: AdminAccount) => account.username === user?.username;

  return (
    <main className="mx-auto flex min-h-svh max-w-4xl flex-col justify-center gap-6 p-8">
      <div className="flex items-center justify-between gap-4">
        <div>
          <p className="text-sm text-muted-foreground">Signed in as {user?.username}</p>
          <h1 className="text-3xl font-semibold tracking-tight">Accounts</h1>
        </div>
        <Button variant="outline" onClick={() => void logout()}>
          Sign out
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Account administration</CardTitle>
          <CardDescription>
            Every registered account, whether it is closed to logins, and whether it is serving a
            lockout right now.
          </CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          {error ? (
            <p className="text-sm text-destructive" role="alert">
              {error}
            </p>
          ) : null}

          {accounts === null ? (
            <p className="text-sm text-muted-foreground">Loading accounts…</p>
          ) : (
            <table className="w-full border-collapse text-left text-sm">
              <caption className="sr-only">Registered accounts</caption>
              <thead>
                <tr className="border-b text-muted-foreground">
                  <th className="py-2 pr-4 font-medium" scope="col">
                    Username
                  </th>
                  <th className="py-2 pr-4 font-medium" scope="col">
                    Role
                  </th>
                  <th className="py-2 pr-4 font-medium" scope="col">
                    Status
                  </th>
                  <th className="py-2 pr-4 font-medium" scope="col">
                    Created
                  </th>
                  <th className="py-2 font-medium" scope="col">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody>
                {accounts.map((account) => (
                  <tr className="border-b last:border-0" key={account.username}>
                    <th className="py-3 pr-4 font-medium" scope="row">
                      {account.username}
                    </th>
                    <td className="py-3 pr-4 text-muted-foreground">{account.role}</td>
                    <td className="py-3 pr-4">
                      <StatusCell account={account} />
                    </td>
                    <td className="py-3 pr-4 text-muted-foreground">
                      {formatDate(account.createdAt)}
                    </td>
                    <td className="py-3">
                      <span className="flex gap-2">
                        {account.enabled ? (
                          <Button
                            // Refused with a 409 by the backend as well: an
                            // administrator who closed their own account could
                            // not reopen it. Turned off here so the refusal is
                            // visible before the click, not after it.
                            disabled={pending !== null || isSelf(account)}
                            onClick={() => void runAction(account, "disable")}
                            size="sm"
                            title={
                              isSelf(account) ? "You cannot disable your own account" : undefined
                            }
                            variant="destructive"
                          >
                            Disable {account.username}
                          </Button>
                        ) : (
                          <Button
                            disabled={pending !== null}
                            onClick={() => void runAction(account, "enable")}
                            size="sm"
                          >
                            Enable {account.username}
                          </Button>
                        )}
                        <Button
                          // Unlocking an account serving no lockout is a no-op,
                          // so the control only offers itself when it would do
                          // something.
                          disabled={pending !== null || !account.locked}
                          onClick={() => void runAction(account, "unlock")}
                          size="sm"
                          variant="outline"
                        >
                          Unlock {account.username}
                        </Button>
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          {accounts !== null && accounts.length === 0 && error === null ? (
            <p className="text-sm text-muted-foreground">No accounts are registered.</p>
          ) : null}
        </CardContent>
      </Card>

      <Link className="text-sm font-medium underline underline-offset-4" to="/showcase">
        Back to counter
      </Link>
    </main>
  );
}
