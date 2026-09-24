import { useCallback, useEffect, useState } from "react";

import { useAuth } from "@/auth/auth-context-value";
import {
  CSRF_EXPIRED_MESSAGE,
  useSessionRequest,
  type SessionResult,
} from "@/auth/use-session-request";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

interface CountResponse {
  count: number;
}

const decodeCount = async (response: Response): Promise<number> => {
  const result = (await response.json()) as CountResponse;
  return result.count;
};

/** The original home page, now available to authenticated users at /showcase. */
export function Showcase() {
  const [count, setCount] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [isUpdating, setIsUpdating] = useState(true);
  const { logout, user } = useAuth();
  const request = useSessionRequest();

  const getCount = useCallback(() => request("/api/count", {}, decodeCount), [request]);
  const incrementCount = useCallback(
    () => request("/api/count/increment", { method: "POST" }, decodeCount),
    [request],
  );
  const resetCount = useCallback(
    () => request("/api/count/reset", { method: "POST" }, decodeCount),
    [request],
  );

  const applyResult = useCallback((result: SessionResult<number>, failureMessage: string) => {
    switch (result.kind) {
      case "ok":
        setCount(result.data);
        return;
      case "csrf-expired":
        setError(CSRF_EXPIRED_MESSAGE);
        return;
      case "failed":
        setError(failureMessage);
    }
  }, []);

  useEffect(() => {
    void getCount()
      .then((result) => applyResult(result, "Unable to load the counter. Please try again."))
      .finally(() => {
        setIsUpdating(false);
      });
  }, [applyResult, getCount]);

  const updateCount = async (request: () => Promise<SessionResult<number>>) => {
    setError(null);
    setIsUpdating(true);
    try {
      applyResult(await request(), "Unable to update the counter. Please try again.");
    } finally {
      setIsUpdating(false);
    }
  };

  return (
    <main className="mx-auto flex min-h-svh max-w-2xl flex-col items-center justify-center gap-6 p-8">
      <div className="flex w-full items-center justify-between gap-4">
        <div>
          <p className="text-sm text-muted-foreground">Signed in as {user?.username}</p>
          <h1 className="text-3xl font-semibold tracking-tight">Front End</h1>
        </div>
        <Button variant="outline" onClick={() => void logout()}>
          Sign out
        </Button>
      </div>

      <Card className="w-full">
        <CardHeader>
          <CardTitle>Baseline is live</CardTitle>
          <CardDescription>
            React + Vite + Tailwind, with the tooling gates wired up.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground" data-testid="count">
            Clicked {count} {count === 1 ? "time" : "times"}
          </p>
          {error ? (
            <p className="mt-2 text-sm text-destructive" role="alert">
              {error}
            </p>
          ) : null}
        </CardContent>
        <CardFooter className="gap-2">
          <Button onClick={() => void updateCount(incrementCount)} disabled={isUpdating}>
            Increment
          </Button>
          <Button
            variant="outline"
            onClick={() => void updateCount(resetCount)}
            disabled={count === 0 || isUpdating}
          >
            Reset
          </Button>
        </CardFooter>
      </Card>
    </main>
  );
}
