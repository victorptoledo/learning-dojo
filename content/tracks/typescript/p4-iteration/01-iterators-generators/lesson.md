# Iterators, Generators and Lazy Sequences

`for...of`, spread, destructuring, `Array.from`, `Map` and `Set` all speak one small protocol:
**iteration**. Generators make that protocol easy to implement, and they evaluate **lazily**:
nothing is computed until someone asks for the next value. This lesson covers the protocol,
how TypeScript types it, generators, the iterator helpers of ES2025 and the traps of
single-use iterators.

## The iteration protocol

```ts
interface IteratorResult<T, TReturn = any> { done: boolean; value: T | TReturn }  // simplified
interface Iterator<T, TReturn = any, TNext = any> {
  next(...[value]: [] | [TNext]): IteratorResult<T, TReturn>;
  return?(value?: TReturn): IteratorResult<T, TReturn>;   // called when the consumer stops early
  throw?(e?: any): IteratorResult<T, TReturn>;
}
interface Iterable<T> { [Symbol.iterator](): Iterator<T> }
```

- An **iterable** is something you can iterate: it has a `[Symbol.iterator]()` method that
  returns a fresh iterator. Arrays, strings, `Map`, `Set`, `arguments` and generator objects
  are iterables.
- An **iterator** is the cursor: each `next()` gives `{ done: false, value }` until
  `{ done: true }`.
- `for...of` calls `[Symbol.iterator]()` once, then `next()` until `done`. If the loop ends
  early (`break`, `return`, `throw`), it calls the iterator's `return()` so it can clean up.

For parameters, accept the **widest** type you need: `Iterable<T>` takes arrays, sets, map
keys and generators; `readonly T[]` only when you need `length` or indexing.

```ts
function sum(values: Iterable<number>): number {
  let total = 0;
  for (const value of values) total += value;
  return total;
}
sum([1, 2, 3]);
sum(new Set([4, 5]));
sum(new Map([["a", 1]]).values());
```

## Generators

A generator function (`function*`) returns a generator object, which is both an iterator and
an iterable. Its body runs **only when `next()` is called**, and pauses at each `yield`:

```ts
function* countdown(from: number): Generator<number, string, undefined> {
  for (let i = from; i > 0; i -= 1) {
    yield i;                // pauses here and hands i to the consumer
  }
  return "lift-off";        // the TReturn value: done: true, value: "lift-off"
}

const it = countdown(3);    // nothing ran yet
it.next();                  // { value: 3, done: false }
[...countdown(3)];          // [3, 2, 1]: for...of and spread ignore the return value
```

`Generator<T, TReturn, TNext>` has three type parameters: what `yield` produces, what
`return` produces, and what the consumer may pass to `next(value)` (the result of the `yield`
expression, rarely used). TypeScript infers them from the body; annotate the return type of
exported generators so the API does not change by accident. `IterableIterator<T>` and
`IteratorObject<T>` describe iterators that are also iterable, such as those of arrays and maps.

### Lazy and infinite

Because values are produced on demand, a generator can describe an infinite sequence; the
consumer decides when to stop:

```ts
function* naturals(): Generator<number, never, undefined> {
  for (let n = 0; ; n += 1) yield n;
}

for (const n of naturals()) {
  if (n > 3) break;         // calls return(): the generator is finished
}
```

The return type `never` says this generator never completes on its own.

### yield* and recursion

`yield*` delegates to another iterable, which makes recursive structures easy to walk:

```ts
type Tree<T> = { value: T; children: readonly Tree<T>[] };

function* depthFirst<T>(tree: Tree<T>): Generator<T, void, undefined> {
  yield tree.value;
  for (const child of tree.children) yield* depthFirst(child);
}
```

### Cleanup with finally

When the consumer stops early, the generator's `return()` resumes it **as if `return`
happened at the paused `yield`**: `finally` blocks run. That is how a generator closes a file
or a cursor it opened:

```ts
function* rows(db: Database): Generator<Row, void, undefined> {
  const cursor = db.open();
  try {
    for (let row = cursor.read(); row; row = cursor.read()) yield row;
  } finally {
    cursor.close();          // runs on completion, on break and on errors
  }
}
```

### The body is lazy, argument checks too

```ts
function* chunk<T>(items: Iterable<T>, size: number) {
  if (size < 1) throw new RangeError("size must be at least 1");  // only on the first next()!
  // ...
}
const chunks = chunk(data, 0);   // no error here
```

To fail at the call, validate in a normal function and return the generator:

```ts
export function chunk<T>(items: Iterable<T>, size: number): Generator<T[], void, undefined> {
  if (!Number.isInteger(size) || size < 1) throw new RangeError("size must be at least 1");
  return chunkGenerator(items, size);
}
```

## Writing an iterable class

Make `[Symbol.iterator]` a generator method and the class works with every consumer:

```ts
class Playlist implements Iterable<Song> {
  readonly #songs: Song[] = [];
  add(song: Song): void { this.#songs.push(song); }
  *[Symbol.iterator](): Generator<Song, void, undefined> {
    yield* this.#songs;
  }
}
const titles = [...playlist].map((song) => song.title);
```

## Single-use iterators vs re-iterable iterables

An array can be iterated as often as you like; **an iterator (and a generator object) only
once**, because it is its own cursor:

```ts
const evens = naturals().filter((n) => n % 2 === 0).take(3);
[...evens];   // [0, 2, 4]
[...evens];   // []: already exhausted
```

If callers need to iterate twice, store an array, or return an object whose
`[Symbol.iterator]()` creates a new generator each time.

## Iterator helpers (ES2025)

Node.js 22 and later have methods on every built-in iterator, typed by `lib` `esnext` (or
`es2025`). They are lazy, like generators:

| Helper | Does |
| --- | --- |
| `map`, `filter`, `flatMap` | Transform lazily, one value at a time |
| `take(n)`, `drop(n)` | Stop after n values (calling `return()` on the source), skip n |
| `toArray()`, `reduce`, `forEach` | Consume everything |
| `some`, `every`, `find` | Consume until the answer is known, then close the source |
| `Iterator.from(iterable)` | Wrap any iterable (or iterator) to get the helpers |

```ts
const firstBigOrders = Iterator.from(orders)
  .filter((order) => order.totalCents > 10_000)
  .map((order) => order.id)
  .take(5)
  .toArray();                 // stops reading orders after the fifth match
```

Arrays are not iterators: `orders.values()` or `Iterator.from(orders)` gives you one. Unlike
the array methods, nothing runs until a consuming method (or `for...of`) pulls the values.

## Typing generic helpers

Your own helpers take `Iterable<T>` and return `Generator<U, void, undefined>`:

```ts
export function* map<T, U>(source: Iterable<T>, fn: (item: T, index: number) => U): Generator<U, void, undefined> {
  let index = 0;
  for (const item of source) yield fn(item, index++);
}
```

Two type-level tricks from earlier phases apply directly:

- **Overloads with a type guard**, so `filter(values, isString)` returns `Generator<string>`:
  `filter<T, S extends T>(source: Iterable<T>, predicate: (item: T) => item is S): Generator<S, void, undefined>`.
- **Mapped tuple types** for variadic helpers. `zip` infers one type per argument:

  ```ts
  function zip<T extends unknown[]>(...sources: { [K in keyof T]: Iterable<T[K]> }): Generator<T, void, undefined>;
  zip([1, 2], ["a", "b"]);    // Generator<[number, string], void, undefined>
  ```

## Summary

- Iterables have `[Symbol.iterator]()`; iterators have `next()` and, optionally, `return()`.
- Generators implement both lazily; `Generator<T, TReturn, TNext>` types them.
- Early exit calls `return()`, which runs the generator's `finally` blocks.
- A generator's body, argument checks included, waits for the first `next()`.
- Iterators are single-use; arrays and iterable objects can be iterated again.
- ES2025 iterator helpers give lazy `map`, `filter`, `take` and friends on every iterator.

## Further reading

- MDN, "Iterators and generators": <https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Iterators_and_generators>
- MDN, "Iteration protocols": <https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Iteration_protocols>
- MDN, `Iterator` (iterator helpers): <https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Iterator>
- TypeScript handbook, "Iterators and Generators": <https://www.typescriptlang.org/docs/handbook/iterators-and-generators.html>
- TypeScript 5.6 release notes, iterator helper types and `IteratorObject`: <https://www.typescriptlang.org/docs/handbook/release-notes/typescript-5-6.html>
