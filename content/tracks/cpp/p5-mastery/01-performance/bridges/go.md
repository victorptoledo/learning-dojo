---
from: go
status: draft
concept: "Performance"
from_way: "Escape analysis, the GC, pprof and benchmarks with testing.B, slices with append growth."
native_way: "You decide stack or heap, there is no GC, perf and Google Benchmark measure, and vector growth works like append."
sensei_tip: "Go's `make([]T, 0, n)` is `v.reserve(n)`; pprof's flame graphs come from perf in C++."
---

| Go | C++ |
| --- | --- |
| `go test -bench` | Google Benchmark |
| pprof | perf (+ flame graphs), VTune |
| `make([]T, 0, n)` | `v.reserve(n)` |
| escape analysis decides | you choose: local, `std::make_unique`, arena |
| sync.Pool | object pools, `std::pmr` |
| `-gcflags=-m` | Compiler Explorer, `-fopt-info` |
