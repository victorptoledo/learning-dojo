---
from: java
status: draft
concept: "Generics and streams"
from_way: "Generics with type erasure and bounded wildcards, List<Integer> boxing, Comparable<T> bounds, Iterator and Iterable, and the Stream API with map, filter and collect."
native_way: "Type parameters compiled without erasure or boxing, constraints that list types or methods, generic methods since 1.27, and iterators as functions with range, composed by small helper functions."
sensei_tip: "Go has no Stream API on purpose. A plain for loop is often the clearest pipeline; reach for iter.Seq helpers when laziness or reuse really matters."
---

| Java | Go |
| --- | --- |
| `<T extends Comparable<T>> T max(List<T> xs)` | `func Max[T cmp.Ordered](xs []T) T` |
| `class Stack<T>` | `type Stack[T any] struct{ items []T }` |
| `<U> Stack<U> map(Function<T, U> f)` | `func (s *Stack[T]) Map[U any](f func(T) U) *Stack[U]` (1.27) |
| `List<Integer>` (boxed) | `[]int` (no boxing) |
| `Iterable<T>` | `iter.Seq[T]` |
| `stream().filter(...).map(...).toList()` | loops, or `slices.Collect(Map(Filter(seq, keep), f))` |
| `Collectors.groupingBy` | a loop appending into `map[K][]V` |
| `? super T` wildcards | not needed: no variance |
