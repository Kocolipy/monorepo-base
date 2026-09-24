import { Link } from "react-router-dom";

import { useAuth } from "@/auth/auth-context-value";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

/** Placeholder for account administration; the ADMIN route guard is already active. */
export function Accounts() {
  const { logout, user } = useAuth();

  return (
    <main className="mx-auto flex min-h-svh max-w-2xl flex-col justify-center gap-6 p-8">
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
          <CardDescription>This area is available to administrators only.</CardDescription>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">Account management is coming soon.</p>
        </CardContent>
      </Card>

      <Link className="text-sm font-medium underline underline-offset-4" to="/showcase">
        Back to counter
      </Link>
    </main>
  );
}
