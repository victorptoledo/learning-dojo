---
from: go
status: draft
concept: "Range-over-func iterators and generators"
from_way: "for range over slices, maps and channels, bufio.Scanner for large files, goroutines feeding channels as pipelines, and iter.Seq range-over-func iterators since Go 1.23."
native_way: "for over any iterable, file objects yielding lines, generator functions with yield as pipeline stages, and itertools for slicing, batching and grouping."
sensei_tip: "Go 1.23's iter.Seq with yield is almost exactly a Python generator. A pipeline of generators gives you the goroutine-and-channel pipeline shape without any concurrency."
---

| Go | Python |
| --- | --- |
| `func Count(n int) iter.Seq[int]` with `yield(i)` | `def count(n): ... yield i` |
| `for v := range seq` | `for v in gen:` |
| `bufio.NewScanner(f)` + `Scan()` | `for line in f:` |
| goroutines and channels as stages | generator functions as stages |
| `slices.Chunk(xs, 500)` | `itertools.batched(xs, 500)` |
| stopping early with `break` (yield returns false) | `break`, then `gen.close()` for cleanup |
