import { describe, expect, it } from "vitest";

import { cn } from "./utils";

describe("cn", () => {
  it("joins class names", () => {
    expect(cn("a", "b")).toBe("a b");
  });

  it("drops falsy values so a conditional class can be inlined", () => {
    const isActive = Boolean(0);

    expect(cn("a", isActive && "b", undefined, null, "c")).toBe("a c");
  });

  it("flattens arrays and object maps", () => {
    expect(cn(["a", "b"], { c: true, d: false })).toBe("a b c");
  });

  it("lets the later of two conflicting Tailwind utilities win", () => {
    // The point of twMerge over plain clsx: both classes set padding-inline,
    // and CSS source order — not argument order — would otherwise decide.
    expect(cn("px-4", "px-8")).toBe("px-8");
  });

  it("keeps utilities that set different properties", () => {
    expect(cn("px-4", "py-8")).toBe("px-4 py-8");
  });

  it("returns an empty string when given nothing", () => {
    expect(cn()).toBe("");
  });
});
