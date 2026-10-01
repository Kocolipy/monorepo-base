import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { ErrorBoundary } from "./error-boundary";

// A distinctive message, so a leak into the DOM cannot hide among ordinary copy.
const ERROR_MESSAGE = "internal-detail-7f3a: secret table users_v2";
const thrown = new Error(ERROR_MESSAGE);

function ThrowingChild(): never {
  throw thrown;
}

describe("ErrorBoundary", () => {
  let consoleError: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    // React reports every caught error through console.error as well; silence
    // it so the run stays readable, and keep the spy to assert our own call.
    consoleError = vi.spyOn(console, "error").mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("renders its children when nothing throws", () => {
    render(
      <ErrorBoundary>
        <p>Page content</p>
      </ErrorBoundary>,
    );

    expect(screen.getByText("Page content")).toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("shows the generic fallback and nothing of the error when a child throws", () => {
    render(
      <ErrorBoundary>
        <ThrowingChild />
      </ErrorBoundary>,
    );

    const alert = screen.getByRole("alert");
    expect(screen.getByRole("heading", { name: "Something went wrong" })).toBeInTheDocument();
    expect(alert).toHaveTextContent(
      /^Something went wrongAn unexpected error stopped this page\. Reload to try again\.Reload page$/,
    );

    const text = document.body.textContent ?? "";
    expect(text).not.toContain(ERROR_MESSAGE);
    expect(text).not.toContain("internal-detail-7f3a");
    expect(text).not.toContain("ThrowingChild");

    // Prove the stack is a real one before asserting none of it leaked.
    const frames = (thrown.stack ?? "")
      .split("\n")
      .slice(1)
      .map((line) => line.trim())
      .filter(Boolean);
    expect(frames.length).toBeGreaterThan(0);
    for (const frame of frames) {
      expect(text).not.toContain(frame);
    }
  });

  it("logs the caught error to console.error with the component stack", () => {
    render(
      <ErrorBoundary>
        <ThrowingChild />
      </ErrorBoundary>,
    );

    expect(consoleError).toHaveBeenCalledWith(
      "[ErrorBoundary] Unhandled render error",
      thrown,
      expect.stringContaining("ThrowingChild"),
    );
  });

  it("reloads the page from the fallback's reload action", async () => {
    const reload = vi.spyOn(window.location, "reload").mockImplementation(() => {});
    render(
      <ErrorBoundary>
        <ThrowingChild />
      </ErrorBoundary>,
    );

    expect(reload).not.toHaveBeenCalled();
    await userEvent.click(screen.getByRole("button", { name: "Reload page" }));

    expect(reload).toHaveBeenCalledTimes(1);
  });
});
