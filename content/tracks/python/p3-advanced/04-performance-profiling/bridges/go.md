---
from: go
status: draft
concept: "pprof habits in Python"
from_way: "go test -bench, pprof CPU and heap profiles, net/http/pprof on a live server, compiled code that is fast by default, escape analysis and fewer allocations, and cgo for C libraries."
native_way: "timeit and pyperf, cProfile and py-spy, tracemalloc, an interpreter where algorithmic fixes and C-backed built-ins matter most, and NumPy, Cython or Rust for hot loops."
sensei_tip: "In Go a loop over a million items is cheap; in CPython it can be tens of times slower. Profile like you would with pprof, but expect the fix to be a better data structure or a call into C."
---

| Go | Python |
| --- | --- |
| `go test -bench .` | `python -m timeit`, `pytest-benchmark` |
| `pprof` CPU profile | `cProfile`, `py-spy record` |
| `net/http/pprof` on a live server | `py-spy top --pid ...` |
| heap profile | `tracemalloc`, `memray` |
| `strings.Builder` | `"".join(parts)` |
| fewer allocations, value types | `__slots__`, tuples, arrays |
| cgo | `ctypes`, `cffi`, Cython, PyO3 |
