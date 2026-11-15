---
from: javascript
status: draft
concept: "Generics and iteration"
from_way: "TypeScript generics with extends bounds, erased at run time, generator functions with function* and yield, Symbol.iterator, and array methods map, filter and reduce."
native_way: "Generics that exist in the compiled code, constraints as type sets, iterators as functions that receive yield, and plain loops or small helpers instead of array method chains."
sensei_tip: "for (const x of gen()) with a break calls gen's return(); in Go the break makes yield return false. Same idea, one bool to check."
---

| TypeScript | Go |
| --- | --- |
| `function first<T>(xs: T[]): T` | `func First[T any](xs []T) T` |
| `<T extends Comparable>`-style bounds | `[T cmp.Ordered]` (numbers and strings) |
| `class Stack<T>` | `type Stack[T any] struct{ ... }` |
| `function* count() { yield 1 }` | `func(yield func(int) bool) { yield(1) }` |
| `Iterable<T>` | `iter.Seq[T]` |
| `[...gen()]` / `Array.from(gen())` | `slices.Collect(seq)` |
| `xs.map(f).filter(g)` | a loop, or `Filter(Map(...))` helpers over `iter.Seq` |
| `Object.keys(o).sort()` | `slices.Sorted(maps.Keys(m))` |
