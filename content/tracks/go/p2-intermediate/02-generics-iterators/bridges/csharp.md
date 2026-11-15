---
from: csharp
status: draft
concept: "Generics, IEnumerable and LINQ"
from_way: "Reified generics with where constraints, generic methods everywhere, IEnumerable<T> with yield return, and LINQ chains such as Where, Select and ToList."
native_way: "Reified-style generics with constraints as type sets, generic methods since Go 1.27 (not on interfaces), iter.Seq with yield, and loops or small helpers instead of LINQ."
sensei_tip: "yield return maps almost one to one: a Go iterator calls yield(v) and returns when it gets false, which is what a foreach with break does to your C# iterator."
---

| C# | Go |
| --- | --- |
| `T Max<T>(IEnumerable<T> xs) where T : IComparable<T>` | `func Max[T cmp.Ordered](xs []T) T` |
| `where T : struct` / `notnull` | constraints are type sets: `comparable`, `cmp.Ordered` or your own |
| `class Stack<T>` | `type Stack[T any] struct{ ... }` |
| generic method `Map<U>(...)` | `func (s *Stack[T]) Map[U any](...)` (1.27) |
| `IEnumerable<T>` with `yield return` | `iter.Seq[T]` calling `yield(v)` |
| `.Where(...).Select(...).ToList()` | a loop, or helpers plus `slices.Collect` |
| `dict.Keys.OrderBy(k => k)` | `slices.Sorted(maps.Keys(m))` |
| `default(T)` | `var zero T` |
