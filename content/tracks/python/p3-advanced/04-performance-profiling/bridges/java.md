---
from: java
status: draft
concept: "Profiling without a JIT doing the work for you"
from_way: "JMH for micro-benchmarks, JFR and async-profiler for profiles, VisualVM, a JIT that inlines and optimises hot code, and heap dumps for memory."
native_way: "timeit and pyperf for benchmarks, cProfile and py-spy for profiles, tracemalloc and memray for memory, a specialising interpreter (and an experimental JIT), and native extensions for hot loops."
sensei_tip: "The HotSpot JIT forgives inefficient code in hot loops; CPython mostly does not. Algorithmic fixes and pushing loops into C (built-ins, NumPy) matter far more in Python than in Java."
---

| Java | Python |
| --- | --- |
| JMH | `python -m timeit`, `pyperf` |
| async-profiler / JFR | `profiling.sampling` (Tachyon, 3.15), `py-spy`, `cProfile` |
| flame graphs from async-profiler | `py-spy record -o profile.svg` |
| heap dump + MAT | `tracemalloc`, `memray` |
| HotSpot C2 JIT | specialising interpreter (3.11+), experimental JIT (3.13, upgraded in 3.15) |
| `StringBuilder` | `"".join(parts)` |
| JNI | C extensions, Cython, Rust with PyO3 |
| `System.nanoTime()` | `time.perf_counter()` |
