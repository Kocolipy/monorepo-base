import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { App } from "./App";

describe("App", () => {
  beforeEach(() => {
    document.cookie = "XSRF-TOKEN=test-token; path=/";
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState(null, "", "/");
    document.cookie = "XSRF-TOKEN=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT";
  });

  it("renders login at the home route for a guest", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 401 })));
    render(<App />);

    expect(await screen.findByRole("heading", { name: "Welcome back" })).toBeInTheDocument();
  });

  it("redirects a guest away from the showcase", async () => {
    window.history.replaceState(null, "", "/showcase");
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 401 })));
    render(<App />);

    expect(await screen.findByRole("heading", { name: "Welcome back" })).toBeInTheDocument();
    expect(window.location.pathname).toBe("/");
  });

  it("renders the showcase when the session is authenticated", async () => {
    window.history.replaceState(null, "", "/showcase");
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ username: "ada" }), {
          headers: { "Content-Type": "application/json" },
          status: 200,
        }),
      ),
    );
    render(<App />);

    expect(await screen.findByRole("heading", { name: "Front End" })).toBeInTheDocument();
    expect(screen.getByText("Signed in as ada")).toBeInTheDocument();
  });

  it("returns to login when the showcase discovers an expired session", async () => {
    window.history.replaceState(null, "", "/showcase");
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValueOnce(Response.json({ username: "ada" }))
        .mockResolvedValueOnce(new Response(null, { status: 401 })),
    );
    render(<App />);

    expect(await screen.findByRole("heading", { name: "Welcome back" })).toBeInTheDocument();
    expect(window.location.pathname).toBe("/");
    expect(await screen.findByRole("status")).toHaveTextContent(
      "Your session ended. Please sign in again.",
    );
  });

  it("does not claim a session ended for a visitor who never had one", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 401 })));
    render(<App />);

    expect(await screen.findByRole("heading", { name: "Welcome back" })).toBeInTheDocument();
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
  });

  it("signs in and sends the guest to the showcase", async () => {
    const [username, password] = ["ada", "correct-password"];
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ username: "ada" }), {
          headers: { "Content-Type": "application/json" },
          status: 200,
        }),
      );
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    render(<App />);

    await user.type(await screen.findByLabelText("Username"), username);
    await user.type(screen.getByLabelText("Password"), password);
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByRole("heading", { name: "Front End" })).toBeInTheDocument();
    expect(window.location.pathname).toBe("/showcase");
    expect(fetchMock).toHaveBeenCalledWith("/api/auth/login", {
      body: JSON.stringify({ username, password }),
      credentials: "include",
      headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": "test-token" },
      method: "POST",
    });
  });

  it("shows invalid credential errors", async () => {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValueOnce(new Response(null, { status: 401 }))
        .mockResolvedValueOnce(new Response(null, { status: 401 })),
    );
    const user = userEvent.setup();
    render(<App />);

    await user.type(await screen.findByLabelText("Username"), "ada");
    await user.type(screen.getByLabelText("Password"), "wrong");
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "The username or password is incorrect.",
    );
  });
});
