---
from: go
status: draft
concept: "From interface{} and channels to generics and iterators"
from_way: "Containers of interface{}, code generation with go generate for typed copies, iterators built from channels and goroutines (which leak when the consumer stops), and callback-style Walk functions."
native_way: "Type parameters (1.18), range over func with iter.Seq (1.23), generic type aliases (1.24), self-referential constraints (1.26) and generic methods (1.27)."
sensei_tip: "Replace every channel-based iterator with an iter.Seq: it needs no goroutine, cannot leak, and stops cleanly when the caller breaks."
---

| Older Go | Go 1.27 |
| --- | --- |
| `func Keys(m map[string]int) []string` copied per type | `slices.Collect(maps.Keys(m))` |
| `go generate` + genny for typed containers | `type Set[T comparable] struct{ ... }` |
| `func (t *Tree) Iter() <-chan T` with a goroutine | `func (t *Tree) All() iter.Seq[T]` |
| `Walk(fn func(T) error)` callbacks | `for v := range t.All()` |
| a free function because methods could not be generic | `func (s *Stack[T]) Map[U any](...)` (1.27) |
| `type Set = map[string]struct{}` only | `type Set[T comparable] = map[T]struct{}` (1.24) |
