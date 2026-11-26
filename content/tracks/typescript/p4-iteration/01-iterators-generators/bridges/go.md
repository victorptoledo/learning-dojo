---
from: go
status: draft
concept: "Range-over-func iterators"
from_way: "iter.Seq[V] functions that call yield and stop when it returns false, for range over them, defer for cleanup, slices.Collect to materialise."
native_way: "Generator functions that pause at each yield, for...of over any Iterable<T>, finally run by return() on early exit, toArray() or spread to materialise."
sensei_tip: "Go pushes values into yield and checks its bool; a TypeScript generator is pulled by next() and stopped by return(). Both run your cleanup when the loop breaks: defer there, finally here."
---

| Go | TypeScript |
| --- | --- |
| `func All() iter.Seq[int]` | `function* all(): Generator<number, void, undefined>` |
| `if !yield(v) { return }` | `yield v;` (the consumer stops it with `return()`) |
| `for v := range seq` | `for (const v of seq)` |
| `defer f.Close()` | `try { ... } finally { file.close() }` |
| `slices.Collect(seq)` | `[...seq]` or `seq.toArray()` |
| `iter.Seq2[K, V]` | `Iterable<[K, V]>` |
