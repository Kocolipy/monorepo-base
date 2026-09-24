import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { AuthContext, type AuthContextValue } from "@/auth/auth-context-value";

import { Showcase } from "./showcase";
import * as counterApi from "./showcase-api";

vi.mock("./showcase-api");

const count = () => screen.getByTestId("count");
const increment = () => screen.getByRole("button", { name: "Increment" });
const reset = () => screen.getByRole("button", { name: "Reset" });

const auth: AuthContextValue = {
  login: vi.fn(),
  logout: vi.fn(),
  status: "authenticated",
  user: { username: "ada" },
};

function renderShowcase(value: AuthContextValue = auth) {
  return render(
    <AuthContext.Provider value={value}>
      <Showcase />
    </AuthContext.Provider>,
  );
}

describe("Showcase", () => {
  beforeEach(() => {
    vi.mocked(counterApi.getCount).mockReset().mockResolvedValue(0);
    vi.mocked(counterApi.incrementCount).mockReset();
    vi.mocked(counterApi.resetCount).mockReset();
  });

  it("renders the original home page and signed-in user", () => {
    renderShowcase();
    expect(screen.getByRole("heading", { name: "Front End" })).toBeInTheDocument();
    expect(screen.getByText("Signed in as ada")).toBeInTheDocument();
  });

  it("renders safely while authenticated user details are unavailable", () => {
    renderShowcase({ ...auth, user: null });
    expect(screen.getByText("Signed in as")).toBeInTheDocument();
  });

  it("loads the current count when the showcase opens", async () => {
    vi.mocked(counterApi.getCount).mockResolvedValue(3);
    renderShowcase();
    expect(await screen.findByText("Clicked 3 times")).toBeInTheDocument();
    expect(counterApi.getCount).toHaveBeenCalledOnce();
  });

  it("disables counter actions while the initial count is loading", async () => {
    let finishLoading: ((count: number) => void) | undefined;
    vi.mocked(counterApi.getCount).mockReturnValue(
      new Promise((resolve) => {
        finishLoading = resolve;
      }),
    );
    renderShowcase();

    expect(increment()).toBeDisabled();
    expect(reset()).toBeDisabled();

    finishLoading?.(2);
    expect(await screen.findByText("Clicked 2 times")).toBeInTheDocument();
    expect(increment()).toBeEnabled();
    expect(reset()).toBeEnabled();
  });

  it("reports a failed initial counter read", async () => {
    vi.mocked(counterApi.getCount).mockRejectedValue(new Error("backend unavailable"));
    renderShowcase();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Unable to load the counter. Please try again.",
    );
    expect(increment()).toBeEnabled();
  });

  it("counts each click", async () => {
    vi.mocked(counterApi.incrementCount).mockResolvedValueOnce(1).mockResolvedValueOnce(2);
    const user = userEvent.setup();
    renderShowcase();
    await user.click(increment());
    await user.click(increment());
    expect(count()).toHaveTextContent(/^Clicked 2 times$/);
  });

  it("uses the singular label at exactly one", async () => {
    vi.mocked(counterApi.incrementCount).mockResolvedValue(1);
    const user = userEvent.setup();
    renderShowcase();
    await user.click(increment());
    expect(count()).toHaveTextContent(/^Clicked 1 time$/);
  });

  it("disables Reset until there is something to reset", async () => {
    vi.mocked(counterApi.incrementCount).mockResolvedValue(1);
    const user = userEvent.setup();
    renderShowcase();
    expect(reset()).toBeDisabled();
    await user.click(increment());
    expect(reset()).toBeEnabled();
  });

  it("disables counter actions while an update is pending", async () => {
    let finishIncrement: ((count: number) => void) | undefined;
    vi.mocked(counterApi.incrementCount).mockReturnValue(
      new Promise((resolve) => {
        finishIncrement = resolve;
      }),
    );
    const user = userEvent.setup();
    renderShowcase();

    await user.click(increment());
    expect(increment()).toBeDisabled();
    expect(reset()).toBeDisabled();

    finishIncrement?.(1);
    expect(await screen.findByText("Clicked 1 time")).toBeInTheDocument();
    expect(increment()).toBeEnabled();
    expect(reset()).toBeEnabled();
  });

  it("returns the count to zero on reset", async () => {
    vi.mocked(counterApi.incrementCount).mockResolvedValue(1);
    vi.mocked(counterApi.resetCount).mockResolvedValue(0);
    const user = userEvent.setup();
    renderShowcase();
    await user.click(increment());
    await user.click(reset());
    expect(count()).toHaveTextContent(/^Clicked 0 times$/);
    expect(reset()).toBeDisabled();
  });

  it("keeps the count and reports backend failures", async () => {
    vi.mocked(counterApi.incrementCount).mockRejectedValue(new Error("backend unavailable"));
    const user = userEvent.setup();
    renderShowcase();

    await user.click(increment());

    expect(count()).toHaveTextContent(/^Clicked 0 times$/);
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Unable to update the counter. Please try again.",
    );
  });

  it("signs out", async () => {
    const user = userEvent.setup();
    renderShowcase();
    await user.click(screen.getByRole("button", { name: "Sign out" }));
    expect(auth.logout).toHaveBeenCalledOnce();
  });
});
