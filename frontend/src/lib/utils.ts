import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

/**
 * Joins class names and resolves Tailwind conflicts, last one winning.
 *
 * Both halves matter: clsx flattens the conditional forms (arrays, objects,
 * falsy values) and twMerge then drops the earlier of any two utilities that
 * set the same property — so a caller's `px-8` overrides a component's default
 * `px-4` instead of landing in the same class list and losing to CSS source
 * order.
 *
 * This is the `cn` the shadcn generator emits, and the in-house component
 * library expects to find it at this path.
 */
export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs));
}
