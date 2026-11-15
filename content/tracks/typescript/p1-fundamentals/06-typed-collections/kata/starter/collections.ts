// Generic helpers for arrays. Each body is yours to write, and each signature is too loose:
// make it generic and precise so the type tests in tests/*.types.ts pass too.

/** The value of `key` in each item, in order. */
export function pluck<T>(items: readonly T[], key: string): unknown[] {
  throw new Error("todo: write pluck()");
}

/** A map from keyOf(item) to the item; when two items share a key, the last one wins. */
export function indexBy<T>(items: readonly T[], keyOf: (item: T) => unknown): Map<unknown, T> {
  throw new Error("todo: write indexBy()");
}

/**
 * Splits items into [matching, rest], both in order. When the predicate is a type guard
 * (`(item): item is S`), the matching part has type S[].
 */
export function partition<T>(items: readonly T[], predicate: (item: T) => boolean): [T[], T[]] {
  throw new Error("todo: write partition()");
}

/** The item with the highest score, the first one on a tie, undefined for no items. */
export function maxBy<T>(items: readonly T[], score: (item: T) => number): T | undefined {
  throw new Error("todo: write maxBy()");
}

/** Consecutive groups of `size` items, the last one maybe shorter. A RangeError when size is not a positive integer. */
export function chunk<T>(items: readonly T[], size: number): T[][] {
  throw new Error("todo: write chunk()");
}

/** An object with one property per key, valued by value(key). */
export function keyed(keys: readonly string[], value: (key: string) => unknown): Record<string, unknown> {
  throw new Error("todo: write keyed()");
}
