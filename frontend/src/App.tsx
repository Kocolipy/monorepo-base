import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";

import { AuthProvider } from "@/auth/auth-context";
import { GuestRoute, ProtectedRoute } from "@/auth/route-guards";
import { Accounts } from "@/pages/accounts";
import { Login } from "@/pages/login";
import { Showcase } from "@/pages/showcase";

/**
 * The application root owns routing and the session-backed authentication state.
 *
 * Every route states what it requires of the session by its guard; the guards
 * share one transition table, so no page decides where a visitor goes.
 */
export function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route
            path="/"
            element={
              <GuestRoute>
                <Login />
              </GuestRoute>
            }
          />
          <Route
            path="/showcase"
            element={
              <ProtectedRoute>
                <Showcase />
              </ProtectedRoute>
            }
          />
          <Route
            path="/accounts"
            element={
              <ProtectedRoute requiredRole="ADMIN">
                <Accounts />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<Navigate replace to="/" />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
