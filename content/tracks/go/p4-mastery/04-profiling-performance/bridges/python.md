---
from: python
status: draft
concept: "cProfile, py-spy and memory tools"
from_way: "cProfile and snakeviz, py-spy for sampling a running process, tracemalloc and memray for memory, pytest-benchmark or timeit, and rewriting hot loops in C or NumPy."
native_way: "pprof samples CPU and memory with little overhead, net/http/pprof profiles live services, benchmarks are part of go test, and hot loops stay in Go: fewer allocations is usually the fix."
sensei_tip: "In Python you often optimise by moving work into C. In Go the code is already compiled; the profile usually points at allocations or contention instead."
---

| Python | Go |
| --- | --- |
| `python -m cProfile` | `go test -cpuprofile=cpu.out` |
| `py-spy top --pid` | `go tool pprof http://host:6060/debug/pprof/profile` |
| `tracemalloc` / memray | heap and allocs profiles |
| `timeit` / pytest-benchmark | `testing.B` with `b.Loop()` |
| `gc.disable()` tricks | `GOGC`, `GOMEMLIMIT` |
| `faulthandler.dump_traceback` | goroutine profile, SIGQUIT dump |
| rewrite in C / Cython | reduce allocations, preallocate, PGO |
