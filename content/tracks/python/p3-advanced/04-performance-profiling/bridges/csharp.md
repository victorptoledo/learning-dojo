---
from: csharp
status: draft
concept: "Benchmarks and profilers"
from_way: "BenchmarkDotNet, the Visual Studio profiler and dotnet-trace, dotnet-counters, the RyuJIT with tiered compilation, Span<T> to avoid allocations, and P/Invoke for native code."
native_way: "timeit and pyperf, cProfile and py-spy, tracemalloc and memray, a specialising interpreter with an experimental JIT, fewer and smaller objects, and native extensions."
sensei_tip: "dotnet-trace attaching to a live process is what py-spy does for Python. Use it on the real workload before changing code."
---

| .NET | Python |
| --- | --- |
| BenchmarkDotNet | `pyperf`, `python -m timeit` |
| `dotnet-trace` | `py-spy record` |
| `dotnet-counters` | `py-spy top`, production metrics |
| memory profiler | `tracemalloc`, `memray` |
| tiered JIT | specialising interpreter, experimental JIT |
| `StringBuilder` | `"".join(parts)` |
| `struct` and `Span<T>` | `__slots__`, tuples, `array`, NumPy |
| P/Invoke | `ctypes`, `cffi`, PyO3 |
