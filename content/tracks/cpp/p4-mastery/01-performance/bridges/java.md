---
from: java
status: draft
concept: "Performance"
from_way: "The JIT optimises hot code, the GC and allocators handle memory, JMH benchmarks, and async-profiler or JFR find hot spots."
native_way: "Code is optimised ahead of time, you choose memory layout and allocations, Google Benchmark measures, and perf or VTune profile."
sensei_tip: "C++ gives you control over layout that the JVM decides for you: a std::vector<Point> is one contiguous block, unlike an ArrayList of references."
---

| Java | C++ |
| --- | --- |
| JMH | Google Benchmark |
| async-profiler, JFR | perf, VTune, Visual Studio profiler |
| `ArrayList<Point>` (references) | `std::vector<Point>` (contiguous values) |
| escape analysis, TLAB allocation | you choose stack, `reserve`, arenas |
| `@Contended` | `alignas(std::hardware_destructive_interference_size)` |
| JIT warm-up | `-O2`/`-O3`, LTO, PGO at build time |
