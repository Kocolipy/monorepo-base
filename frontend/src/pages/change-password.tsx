import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";

import type { PasswordChangeOutcome } from "@/auth/api";
import { useAuth } from "@/auth/auth-context-value";
import { DEFAULT_DESTINATION } from "@/auth/session-route";
import { CSRF_EXPIRED_MESSAGE } from "@/auth/use-session-request";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

const MISMATCH_MESSAGE = "The new password and its confirmation do not match.";

/**
 * What a refused change means to the User. None of these contain either
 * submitted value: a policy refusal shows the backend's statement of the rule,
 * which names the rule and never the password.
 */
function refusalMessage(outcome: Exclude<PasswordChangeOutcome, { kind: "changed" }>): string {
  switch (outcome.kind) {
    case "policy-violation":
      return outcome.message;
    case "current-password-rejected":
      return "The current password is incorrect.";
    case "locked":
      return "Too many incorrect passwords: the account is now locked and this session has ended. An Admin must Unlock the account before you can sign in again.";
    case "csrf-expired":
      return CSRF_EXPIRED_MESSAGE;
    case "failed":
      return "Unable to change the password. Please try again.";
  }
}

const inputClass =
  "flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring";

/**
 * The self-service password change, at `/change-password`.
 *
 * Where it is offered is the route guard's decision: every authenticated
 * visitor may open it, and a session with the change-required flag is offered
 * nothing else. On success the backend ends every session of the User, so the
 * auth state is cleared and the guard returns the visitor to login.
 *
 * The fields are uncontrolled on purpose. A controlled input mirrors its value
 * into the DOM `value` attribute, and a password belongs in no attribute; read
 * once from the form on submit, it lives only in the input and the request.
 */
export function ChangePassword() {
  const { changePassword, logout, user } = useAuth();
  const [error, setError] = useState("");
  const [locked, setLocked] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const confined = user?.passwordChangeRequired === true;

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    // Captured before the first await: React clears `currentTarget` once the
    // handler has returned, and the form is still needed to clear the fields.
    const form = event.currentTarget;
    const data = new FormData(form);
    const currentPassword = String(data.get("currentPassword"));
    const newPassword = String(data.get("newPassword"));
    setError("");

    if (newPassword !== String(data.get("confirmPassword"))) {
      form.reset();
      setError(MISMATCH_MESSAGE);
      return;
    }

    setSubmitting(true);
    try {
      const outcome = await changePassword(currentPassword, newPassword);
      // On success the guard has already moved on; there is nothing to show.
      if (outcome.kind === "changed") return;
      form.reset();
      setLocked(outcome.kind === "locked");
      setError(refusalMessage(outcome));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="mx-auto grid min-h-svh max-w-md place-items-center p-8">
      <Card className="w-full">
        <CardHeader>
          <CardTitle>Change your password</CardTitle>
          <CardDescription>
            {confined
              ? "Your password must be replaced before you can continue."
              : "Choose a new password for your account."}{" "}
            Every session you hold ends when it changes, so you will sign in again.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <p className="mb-4 text-sm text-muted-foreground">Signed in as {user?.username}</p>
          <form className="space-y-4" onSubmit={(event) => void handleSubmit(event)}>
            <div className="space-y-2">
              <label className="text-sm font-medium" htmlFor="currentPassword">
                Current password
              </label>
              <input
                autoComplete="current-password"
                className={inputClass}
                disabled={locked}
                id="currentPassword"
                name="currentPassword"
                required
                type="password"
              />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium" htmlFor="newPassword">
                New password
              </label>
              <input
                autoComplete="new-password"
                className={inputClass}
                disabled={locked}
                id="newPassword"
                name="newPassword"
                required
                type="password"
              />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium" htmlFor="confirmPassword">
                Confirm new password
              </label>
              <input
                autoComplete="new-password"
                className={inputClass}
                disabled={locked}
                id="confirmPassword"
                name="confirmPassword"
                required
                type="password"
              />
            </div>
            {error ? (
              <p className="text-sm text-destructive" role="alert">
                {error}
              </p>
            ) : null}
            <Button className="w-full" disabled={submitting || locked} type="submit">
              {submitting ? "Changing password…" : "Change password"}
            </Button>
          </form>
        </CardContent>
        <CardFooter className="justify-between gap-2">
          <Button variant="outline" onClick={() => void logout()}>
            Sign out
          </Button>
          {confined || locked ? null : (
            <Link
              className="text-sm font-medium underline underline-offset-4"
              to={DEFAULT_DESTINATION}
            >
              Back
            </Link>
          )}
        </CardFooter>
      </Card>
    </main>
  );
}
