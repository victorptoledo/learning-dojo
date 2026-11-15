# Typed Collections

`collections.ts` has six helpers for arrays. Two things are yours: the **body** of each
function, and its **signature**, which the starter gives in a loose form (`unknown`, `string`)
that loses type information. Make each one generic so callers get precise types back.

| Function | Returns | The precise type |
| --- | --- | --- |
| `pluck(items, key)` | The value of `key` in each item, in order | Only keys of the item are accepted, and `pluck(users, "id")` is `number[]` |
| `indexBy(items, keyOf)` | A `Map` from `keyOf(item)` to the item; the last item wins on a repeated key | `indexBy(users, u => u.id)` is `Map<number, User>` |
| `partition(items, predicate)` | `[matching, rest]`, both in order | With a type guard `(p): p is Cat` it is `[Cat[], Dog[]]`; with a plain predicate, `[T[], T[]]` |
| `maxBy(items, score)` | The item with the highest score, the first on a tie, `undefined` for none | `T \| undefined` |
| `chunk(items, size)` | Groups of `size` items, the last one maybe shorter; a `RangeError` when `size` is not a positive integer | `T[][]` |
| `keyed(keys, value)` | An object with one property per key | `keyed(["a", "bb"], k => k.length)` is `Record<"a" \| "bb", number>` |

The type-level tests are in `tests/*.types.ts`: each `// test:` section is one test, and it
passes when `tsc` finds no error in it. `Expect<Equal<A, B>>` (from `tests/expect.ts`) only
compiles when `A` and `B` are the same type, and a `// @ts-expect-error` line only compiles
when the next line **is** an error, so it checks that wrong calls are rejected.

`partition` needs two **overload signatures** (one for a type guard, one for a plain
predicate) above one implementation.

Run the tests with `npm test` after `npm install`. There are 18 tests: 2 visible
(`tests/visible.test.ts` and `tests/visible.types.ts`) and 16 hidden, 7 of them type-level.
