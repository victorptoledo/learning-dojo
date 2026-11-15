---
from: python
status: draft
concept: "Generics and generators"
from_way: "TypeVar and the def f[T](...) syntax checked only by mypy, generator functions with yield, itertools for lazy pipelines, and comprehensions to collect."
native_way: "Type parameters checked by the compiler, constraints instead of bounds, and iterators written as functions that call yield; the caller's break makes yield return false."
sensei_tip: "A Go iterator is a generator turned inside out: instead of the consumer calling next(), the producer calls yield. Check the bool it returns, the way a generator would notice close()."
---

| Python | Go |
| --- | --- |
| `def first[T](xs: list[T]) -> T` | `func First[T any](xs []T) T` |
| `T: (int, float)` constraint | `[N Number]`, where `Number` lists `~int` and `~float64` |
| `class Stack[T]` | `type Stack[T any] struct{ ... }` |
| `def gen(): yield 1` | `func(yield func(int) bool) { yield(1) }` |
| `Iterator[int]` | `iter.Seq[int]` |
| `list(gen())` | `slices.Collect(seq)` |
| `sorted(d.keys())` | `slices.Sorted(maps.Keys(d))` |
| `itertools.islice(gen, 3)` | a `Take` helper that stops after 3 |
| `next(it)` | `iter.Pull(seq)` gives `next()` and `stop()` |
