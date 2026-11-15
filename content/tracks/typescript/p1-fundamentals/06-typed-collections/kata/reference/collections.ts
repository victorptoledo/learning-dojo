// Generic helpers for arrays. Each body is yours to write, and each signature is too loose:
// make it generic and precise so the type tests in tests/*.types.ts pass too.

/** The value of `key` in each item, in order. */
export function pluck<T, K extends keyof T>(items: readonly T[], key: K): T[K][] {
  return items.map((item) => item[key]);
}

/** A map from keyOf(item) to the item; when two items share a key, the last one wins. */
export function indexBy<T, K>(items: readonly T[], keyOf: (item: T) => K): Map<K, T> {
  const index = new Map<K, T>();
  for (const item of items) {
    index.set(keyOf(item), item);
  }
  return index;
}

/**
 * Splits items into [matching, rest], both in order. When the predicate is a type guard
 * (`(item): item is S`), the matching part has type S[].
 */
export function partition<T, S extends T>(
  items: readonly T[],
  predicate: (item: T) => item is S,
): [S[], Exclude<T, S>[]];
export function partition<T>(items: readonly T[], predicate: (item: T) => boolean): [T[], T[]];
export function partition<T>(items: readonly T[], predicate: (item: T) => boolean): [T[], T[]] {
  const matching: T[] = [];
  const rest: T[] = [];
  for (const item of items) {
    (predicate(item) ? matching : rest).push(item);
  }
  return [matching, rest];
}

/** The item with the highest score, the first one on a tie, undefined for no items. */
export function maxBy<T>(items: readonly T[], score: (item: T) => number): T | undefined {
  let best: T | undefined;
  let bestScore = Number.NEGATIVE_INFINITY;
  for (const item of items) {
    const current = score(item);
    if (best === undefined || current > bestScore) {
      best = item;
      bestScore = current;
    }
  }
  return best;
}

/** Consecutive groups of `size` items, the last one maybe shorter. A RangeError when size is not a positive integer. */
export function chunk<T>(items: readonly T[], size: number): T[][] {
  if (!Number.isInteger(size) || size < 1) {
    throw new RangeError(`chunk size must be a positive integer, got ${size}`);
  }
  const groups: T[][] = [];
  for (let start = 0; start < items.length; start += size) {
    groups.push(items.slice(start, start + size));
  }
  return groups;
}

/** An object with one property per key, valued by value(key). */
export function keyed<K extends string, V>(keys: readonly K[], value: (key: K) => V): Record<K, V> {
  const result = {} as Record<K, V>;
  for (const key of keys) {
    result[key] = value(key);
  }
  return result;
}
