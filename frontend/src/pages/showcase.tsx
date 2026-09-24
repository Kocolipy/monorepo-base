import { useCallback, useEffect, useState } from "react";

import { useAuth } from "@/auth/auth-context-value";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { apiFetch, type ApiResult } from "@/lib/http";

interface CountResponse {
  count: number;
}

const CSRF_MESSAGE = "Your security token expired. Please try again.";

const decodeCount = async (response: Response): Promise<number> => {
  const result = (await response.json()) as CountResponse;
  return result.count;
};

const getCount = (): Promise<ApiResult<number>> => apiFetch("/api/count", {}, decodeCount);
const incrementCount = (): Promise<ApiResult<number>> =>
  apiFetch("/api/count/increment", { method: "POST" }, decodeCount);
const resetCount = (): Promise<ApiResult<number>> =>
  apiFetch("/api/count/reset", { method: "POST" }, decodeCount);

/** The original home page, now available to authenticated users at /showcase. */
export function Showcase() {
  const [count, setCount] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [isUpdating, setIsUpdating] = useState(true);
  const { expireSession, logout, user } = useAuth();

  const applyResult = useCallback(
    (result: ApiResult<number>, failureMessage: string) => {
      switch (result.kind) {
        case "ok":
          setCount(result.data);
          return;
        case "unauthenticated":
          expireSession();
          return;
        case "csrf-expired":
          setError(CSRF_MESSAGE);
          return;
        case "failed":
          setError(failureMessage);
      }
    },
    [expireSession],
  );

  useEffect(() => {
    void getCount()
      .then((result) => applyResult(result, "Unable to load the counter. Please try again."))
      .finally(() => {
        setIsUpdating(false);
      });
  }, [applyResult]);

  const updateCount = async (request: () => Promise<ApiResult<number>>) => {
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
