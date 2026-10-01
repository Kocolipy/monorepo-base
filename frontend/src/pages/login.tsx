import { useState, type FormEvent } from "react";
import { useLocation } from "react-router-dom";

import { useAuth } from "@/auth/auth-context-value";
import type { SessionRouteState } from "@/auth/session-route";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

/** Shown when the visitor arrives here because their session expired. */
const EXPIRED_MESSAGE = "Your session ended. Please sign in again.";

/** Shown when the visitor arrives here because their own password change ended the session. */
const CHANGED_CREDENTIAL_MESSAGE = "Your password was changed. Sign in with your new password.";

export function Login() {
  const { login } = useAuth();
  const location = useLocation();
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  // Where to go afterwards is the guest route's decision, not this page's: it
  // reads the same return destination and redirects once the status changes.
  const carried = location.state as SessionRouteState | null;
  const notice =
    carried?.passwordChanged === true
      ? CHANGED_CREDENTIAL_MESSAGE
      : carried?.expired === true
        ? EXPIRED_MESSAGE
        : null;

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setSubmitting(true);
    const data = new FormData(event.currentTarget);
    try {
      await login(String(data.get("username")), String(data.get("password")));
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Unable to sign in. Please try again.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="mx-auto grid min-h-svh max-w-md place-items-center p-8">
      <Card className="w-full">
        <CardHeader>
          <CardTitle>Welcome back</CardTitle>
          <CardDescription>Sign in to continue.</CardDescription>
        </CardHeader>
        <CardContent>
          {notice && !error ? (
            <p className="mb-4 text-sm text-muted-foreground" role="status">
              {notice}
            </p>
          ) : null}
          <form className="space-y-4" onSubmit={(event) => void handleSubmit(event)}>
            <div className="space-y-2">
              <label className="text-sm font-medium" htmlFor="username">
                Username
              </label>
              <input
                autoComplete="username"
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
                id="username"
                name="username"
                required
              />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium" htmlFor="password">
                Password
              </label>
              <input
                autoComplete="current-password"
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
                id="password"
                name="password"
                required
                type="password"
              />
            </div>
            {error ? (
              <p className="text-sm text-destructive" role="alert">
                {error}
              </p>
            ) : null}
            <Button className="w-full" disabled={submitting} type="submit">
              {submitting ? "Signing in…" : "Sign in"}
            </Button>
          </form>
        </CardContent>
      </Card>
    </main>
  );
}
