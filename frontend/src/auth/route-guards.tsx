import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";

import { useAuth } from "./auth-context-value";
import {
  resolveSessionRoute,
  type SessionRequirement,
  type SessionRouteState,
} from "./session-route";

/** The one pending view: every route waits for the session check the same way. */
function CheckingSession() {
  return (
    <p className="grid min-h-svh place-items-center text-muted-foreground">
      Checking your session…
    </p>
  );
}

function SessionRoute({
  children,
  requires,
}: {
  children: ReactNode;
  requires: SessionRequirement;
}) {
  const { sessionExpired, status } = useAuth();
  const location = useLocation();
  const carried = location.state as SessionRouteState | null;

  const route = resolveSessionRoute({
    pathname: location.pathname,
    requires,
    returnTo: carried?.from,
    sessionExpired,
    status,
  });

  switch (route.kind) {
    case "pending":
      return <CheckingSession />;
    case "render":
      return children;
    case "redirect":
      return <Navigate replace state={route.state} to={route.to} />;
  }
}

/** A route only an authenticated visitor may see. */
export function ProtectedRoute({ children }: { children: ReactNode }) {
  return <SessionRoute requires="authenticated">{children}</SessionRoute>;
}

/** A route only a guest may see; an authenticated visitor is sent onward. */
export function GuestRoute({ children }: { children: ReactNode }) {
  return <SessionRoute requires="guest">{children}</SessionRoute>;
}
