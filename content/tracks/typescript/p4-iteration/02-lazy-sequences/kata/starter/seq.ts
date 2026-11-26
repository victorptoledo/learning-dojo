// Lazy sequence helpers built on generators. Each one pulls from its source only when its
// consumer asks, and closes the source when it stops early. See README.md.

/** start, start + step, ... up to end (excluded); with no end, forever. */
export function range(start: number, end: number = Infinity, step: number = 1): Generator<number, void, undefined> {
  throw new Error("TODO: range");
}

export function map<T, U>(source: Iterable<T>, fn: (item: T, index: number) => U): Generator<U, void, undefined> {
  throw new Error("TODO: map");
}

// TODO: an overload that takes a type guard and narrows the items.
export function filter<T>(source: Iterable<T>, predicate: (item: T) => boolean): Generator<T, void, undefined> {
  throw new Error("TODO: filter");
}

/** The first count items; pulls no more than count from the source. */
export function take<T>(source: Iterable<T>, count: number): Generator<T, void, undefined> {
  throw new Error("TODO: take");
}

/** Groups items into arrays of size items; the last one may be shorter. */
export function chunk<T>(source: Iterable<T>, size: number): Generator<T[], void, undefined> {
  throw new Error("TODO: chunk");
}

/** Tuples of the n-th item of every source; stops at the shortest and closes the others. */
// TODO: one type per source: zip([1], ["a"]) should give Generator<[number, string], ...>.
export function zip(...sources: Iterable<unknown>[]): Generator<unknown[], void, undefined> {
  throw new Error("TODO: zip");
}

/** Each item with the one after it: [a, b], [b, c], ... */
export function pairwise<T>(source: Iterable<T>): Generator<[T, T], void, undefined> {
  throw new Error("TODO: pairwise");
}

/** A re-iterable, lazy pipeline: each iteration starts again from the source. */
export class Seq<T> implements Iterable<T> {
  static from<T>(source: Iterable<T>): Seq<T> {
    throw new Error("TODO: Seq.from");
  }

  [Symbol.iterator](): Iterator<T> {
    throw new Error("TODO: Seq iterator");
  }

  map<U>(fn: (item: T, index: number) => U): Seq<U> {
    throw new Error("TODO: Seq.map");
  }

  // TODO: an overload that takes a type guard, as for filter().
  filter(predicate: (item: T) => boolean): Seq<T> {
    throw new Error("TODO: Seq.filter");
  }

  take(count: number): Seq<T> {
    throw new Error("TODO: Seq.take");
  }

  toArray(): T[] {
    throw new Error("TODO: Seq.toArray");
  }
}
