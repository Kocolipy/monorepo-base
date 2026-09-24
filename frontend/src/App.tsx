import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";

import { AuthProvider } from "@/auth/auth-context";
import { ProtectedRoute } from "@/auth/protected-route";
import { Login } from "@/pages/login";
import { Showcase } from "@/pages/showcase";

/**
 * The application root owns routing and the session-backed authentication state.
 */
export function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/" element={<Login />} />
          <Route
            path="/showcase"
            element={
              <ProtectedRoute>
                <Showcase />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<Navigate replace to="/" />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
