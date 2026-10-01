import { Component, type ErrorInfo, type ReactNode } from "react";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

interface ErrorBoundaryProps {
  children: ReactNode;
}

interface ErrorBoundaryState {
  hasError: boolean;
}

/**
 * The top-level catch for render and lifecycle errors. Without it React 19
 * unmounts the whole root on an uncaught render exception and leaves a blank
 * page.
 *
 * The fallback is deliberately generic. It shows no error message, stack or
 * component name, because those are internal details. The error goes to
 * `console.error` only: there is no frontend telemetry, so this component adds
 * no reporting endpoint.
 *
 * React has no hook form of an error boundary, so this has to be a class.
 * There is no app shell either: navigation and sign-out live inside each page,
 * so a per-route boundary would replace them along with the page. `App.tsx`
 * therefore wraps the whole tree, router and `AuthProvider` included.
 */
export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  override state: ErrorBoundaryState = { hasError: false };

  static getDerivedStateFromError(): ErrorBoundaryState {
    return { hasError: true };
  }

  override componentDidCatch(error: unknown, info: ErrorInfo) {
    console.error("[ErrorBoundary] Unhandled render error", error, info.componentStack);
  }

  override render() {
    if (!this.state.hasError) {
      return this.props.children;
    }

    return (
      <main className="mx-auto flex min-h-svh max-w-md flex-col items-center justify-center p-8">
        <Card className="w-full" role="alert">
          <CardHeader>
            <CardTitle>Something went wrong</CardTitle>
            <CardDescription>
              An unexpected error stopped this page. Reload to try again.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Button onClick={() => window.location.reload()}>Reload page</Button>
          </CardContent>
        </Card>
      </main>
    );
  }
}
