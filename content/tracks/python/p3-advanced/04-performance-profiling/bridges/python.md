---
from: python
status: draft
concept: "Modern performance tooling"
from_way: "time.time() around code, print-based timing, profile (the pure-Python profiler), guessing at hot spots, staying on an old interpreter for years, and C extensions written by hand."
native_way: "time.perf_counter and timeit, cProfile with snakeviz, py-spy and Scalene sampling profilers, tracemalloc and memray, upgrading to faster CPython releases, and Rust with PyO3 and maturin."
sensei_tip: "Moving from 3.10 to 3.15 is a free speed-up for most code thanks to the specialising interpreter; measure your own workload, then upgrade before you micro-optimise."
---

| Older habit | Python 3.15 |
| --- | --- |
| `start = time.time()` | `time.perf_counter()` |
| `import profile` (deprecated in 3.15) | `python -m profiling.sampling` (3.15), `cProfile`, `py-spy` |
| reading raw `pstats` text | `snakeviz`, flame graphs from `py-spy record` |
| guessing memory use | `tracemalloc` snapshots, `memray` |
| hand-written C extensions | Cython, or Rust with PyO3 and maturin |
| staying on 3.8 for stability | 3.11+ (specialising interpreter), 3.13+ (experimental JIT, upgraded in 3.15) |
| `@memoize` recipes | `functools.cache` |
