// A small form validation library. The README of each stage lists what to export.

/** One member per kind of rule, told apart by `kind` (only the first one is here). */
export type Rule = { kind: "required" };

/** The messages of every rule the value breaks, in the order of `rules`. */
export function checkField(rules: readonly Rule[], value: unknown): string[] {
  throw new Error("todo: write checkField()");
}
