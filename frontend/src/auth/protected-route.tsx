import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";

import { useAuth } from "./auth-context-value";

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { status } = useAuth();
  const location = useLocation();

  if (status === "checking") {
    return (
      <p className="grid min-h-svh place-items-center text-muted-foreground">
        Checking your session…
      </p>
    );
  }
  if (status === "guest") {
    return <Navigate replace state={{ from: location.pathname }} to="/" />;
  }
  return children;
}
