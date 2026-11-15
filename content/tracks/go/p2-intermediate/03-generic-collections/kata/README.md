# Generic Collections

Write a generic set and five generic helpers in `collections.go`. The `Number` constraint
and the `Set` struct are already declared.

**The set**, `Set[T comparable]`, stores its items in a `map[T]struct{}`. Its **zero value
must work**: `var s Set[int]; s.Add(1)` creates the map on first use.

| Method | Does |
| --- | --- |
| `NewSet[T comparable](items ...T) *Set[T]` | A set holding `items` (duplicates count once) |
| `Add(items ...T)` | Puts items in |
| `Has(item T) bool`, `Len() int` | Membership and size (both work on an empty zero value) |
| `All() iter.Seq[T]` | Every item once, in any order; stops when the loop breaks |
| `Union(other)`, `Intersect(other)` | **New** sets; the inputs do not change |

**The helpers** work on any type. `Map`, `Filter` and `Take` return **lazy** iterators:
building them produces nothing, and they pull from the source only as far as the consumer
asks. When `yield` returns `false`, stop at once (calling `yield` again panics).

| Function | Does |
| --- | --- |
| `Map[T, U any](seq iter.Seq[T], f func(T) U) iter.Seq[U]` | Yields `f(v)` for every `v` |
| `Filter[T any](seq iter.Seq[T], keep func(T) bool) iter.Seq[T]` | Yields the values `keep` accepts |
| `Take[T any](seq iter.Seq[T], n int) iter.Seq[T]` | Yields at most `n` values, and pulls exactly that many from `seq` (none when `n <= 0`) |
| `GroupBy[T any, K comparable](items []T, key func(T) K) map[K][]T` | Groups items by key, keeping their order inside each group |
| `Sum[N Number](values ...N) N` | The total, `0` for no values; works for `type Cents int64` thanks to `~` |

```go
evens := collections.Filter(naturals(), func(n int) bool { return n%2 == 0 })
squares := collections.Map(evens, func(n int) int { return n * n })
slices.Collect(collections.Take(squares, 3)) // [0 4 16], even though naturals() never ends
```

Tools: `maps.Keys`, `slices.Values`, `slices.Collect`, `slices.Sorted`, `var zero T`, and
`for v := range seq` inside your own iterator functions.

Run the visible test with `go test`. The dojo runs 11 tests: 1 in `collections_test.go` and
10 hidden ones, plus `go vet` and `gofmt` on `collections.go`.
