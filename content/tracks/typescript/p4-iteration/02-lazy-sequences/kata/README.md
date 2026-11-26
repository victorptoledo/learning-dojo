# Lazy Sequences

`seq.ts` is a small library of **lazy** sequence helpers. Each one is a generator (or returns
one): it pulls an item from its source only when its own consumer asks for one, and when it
stops before the source is exhausted it **closes** the source (leaving a `for...of` early does
that for you, by calling `return()`).

| Function | Does |
| --- | --- |
| `range(start, end = Infinity, step = 1)` | `start`, `start + step`, ... while below `end` (above it for a negative step). A `step` of 0 (or not finite) throws a `RangeError` **when `range` is called** |
| `map(source, fn)` | `fn(item, index)` for each item |
| `filter(source, predicate)` | The items that match. With a type guard (`(x) => x is S`) the result is narrowed to `S`: add an **overload** |
| `take(source, count)` | The first `count` items, pulling **exactly** `count` from the source (none for 0) and closing it. A count that is not an integer of at least 0 throws a `RangeError` at the call |
| `chunk(source, size)` | Arrays of `size` items, the last one possibly shorter. A size that is not an integer of at least 1 throws a `RangeError` at the call |
| `zip(...sources)` | A tuple with the n-th item of each source, stopping at the shortest. It closes every source that is not finished, including when the consumer stops early. `zip()` gives nothing. Typed with **one type per source**: `zip([1], ["a"])` is `Generator<[number, string], void, undefined>` |
| `pairwise(source)` | `[a, b]`, `[b, c]`, ... (nothing for fewer than two items; `undefined` is an item like any other) |

`Seq<T>` wraps a pipeline: `Seq.from(source)`, then `map`, `filter` (with the same type-guard
overload), `take` and `toArray()`. Building the pipeline pulls nothing. A `Seq` is
**re-iterable**: each iteration starts again from its source, so a `Seq` over an array can be
iterated as often as you like. `take` checks its count when it is called.

> [!TIP]
> "At the call" means before the first `next()`. A generator's body, its checks included,
> only runs on the first `next()`: put the check in an ordinary function that returns the
> generator.

Run the tests with `npm test`. There are 22 tests: 1 in `tests/visible.test.ts` and 21 hidden
ones, 5 of them type-level.
