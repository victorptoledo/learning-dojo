// Lazy sequence helpers built on generators. Each one pulls from its source only when its
// consumer asks, and closes the source when it stops early.

function* rangeGenerator(start: number, end: number, step: number): Generator<number, void, undefined> {
  for (let value = start; step > 0 ? value < end : value > end; value += step) {
    yield value;
  }
}

/** start, start + step, ... up to end (excluded); with no end, forever. */
export function range(start: number, end: number = Infinity, step: number = 1): Generator<number, void, undefined> {
  if (step === 0 || !Number.isFinite(step)) {
    throw new RangeError("step must be a finite number other than 0");
  }
  return rangeGenerator(start, end, step);
}

export function* map<T, U>(source: Iterable<T>, fn: (item: T, index: number) => U): Generator<U, void, undefined> {
  let index = 0;
  for (const item of source) {
    yield fn(item, index);
    index += 1;
  }
}

export function filter<T, S extends T>(
  source: Iterable<T>,
  predicate: (item: T) => item is S,
): Generator<S, void, undefined>;
export function filter<T>(source: Iterable<T>, predicate: (item: T) => boolean): Generator<T, void, undefined>;
export function* filter<T>(source: Iterable<T>, predicate: (item: T) => boolean): Generator<T, void, undefined> {
  for (const item of source) {
    if (predicate(item)) {
      yield item;
    }
  }
}

function* takeGenerator<T>(source: Iterable<T>, count: number): Generator<T, void, undefined> {
  if (count === 0) {
    return;
  }
  let taken = 0;
  for (const item of source) {
    yield item;
    taken += 1;
    if (taken === count) {
      return; // leaving the loop calls return() on the source
    }
  }
}

/** The first count items; pulls no more than count from the source. */
export function take<T>(source: Iterable<T>, count: number): Generator<T, void, undefined> {
  if (!Number.isInteger(count) || count < 0) {
    throw new RangeError("count must be an integer of at least 0");
  }
  return takeGenerator(source, count);
}

function* chunkGenerator<T>(source: Iterable<T>, size: number): Generator<T[], void, undefined> {
  let current: T[] = [];
  for (const item of source) {
    current.push(item);
    if (current.length === size) {
      yield current;
      current = [];
    }
  }
  if (current.length > 0) {
    yield current;
  }
}

/** Groups items into arrays of size items; the last one may be shorter. */
export function chunk<T>(source: Iterable<T>, size: number): Generator<T[], void, undefined> {
  if (!Number.isInteger(size) || size < 1) {
    throw new RangeError("size must be an integer of at least 1");
  }
  return chunkGenerator(source, size);
}

/** Tuples of the n-th item of every source; stops at the shortest and closes the others. */
export function* zip<T extends unknown[]>(
  ...sources: { [K in keyof T]: Iterable<T[K]> }
): Generator<T, void, undefined> {
  const iterators: Iterator<unknown>[] = sources.map((source) => source[Symbol.iterator]());
  const finished = new Set<Iterator<unknown>>();
  if (iterators.length === 0) {
    return;
  }
  try {
    while (true) {
      const values: unknown[] = [];
      for (const iterator of iterators) {
        const result = iterator.next();
        if (result.done) {
          finished.add(iterator);
          return;
        }
        values.push(result.value);
      }
      yield values as T; // the one cast: one value per source, in order
    }
  } finally {
    for (const iterator of iterators) {
      if (!finished.has(iterator)) {
        iterator.return?.();
      }
    }
  }
}

/** Each item with the one after it: [a, b], [b, c], ... */
export function* pairwise<T>(source: Iterable<T>): Generator<[T, T], void, undefined> {
  let first = true;
  let previous: T | undefined;
  for (const item of source) {
    if (!first) {
      yield [previous as T, item];
    }
    previous = item;
    first = false;
  }
}

/** A re-iterable, lazy pipeline: each iteration starts again from the source. */
export class Seq<T> implements Iterable<T> {
  readonly #make: () => Iterator<T>;

  private constructor(make: () => Iterator<T>) {
    this.#make = make;
  }

  static from<T>(source: Iterable<T>): Seq<T> {
    return new Seq(() => source[Symbol.iterator]());
  }

  [Symbol.iterator](): Iterator<T> {
    return this.#make();
  }

  map<U>(fn: (item: T, index: number) => U): Seq<U> {
    return new Seq(() => map(this, fn));
  }

  filter<S extends T>(predicate: (item: T) => item is S): Seq<S>;
  filter(predicate: (item: T) => boolean): Seq<T>;
  filter(predicate: (item: T) => boolean): Seq<T> {
    return new Seq(() => filter(this, predicate));
  }

  take(count: number): Seq<T> {
    take([], count); // checks the argument now, not on the first iteration
    return new Seq(() => take(this, count));
  }

  toArray(): T[] {
    return [...this];
  }
}
