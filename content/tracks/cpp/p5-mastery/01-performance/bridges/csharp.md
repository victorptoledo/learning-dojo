---
from: csharp
status: draft
concept: "Performance"
from_way: "The JIT with tiered compilation, Span<T> and stackalloc, ArrayPool, BenchmarkDotNet and dotnet-trace."
native_way: "Ahead-of-time optimisation, std::span and stack objects, std::pmr arenas, Google Benchmark and perf or VTune."
sensei_tip: "Span<T>, stackalloc and ArrayPool are .NET's way to do what C++ does by default; std::pmr is ArrayPool for whole containers."
---

| C# | C++ |
| --- | --- |
| BenchmarkDotNet | Google Benchmark |
| dotnet-trace, PerfView | perf, VTune |
| `Span<T>` | `std::span<T>` |
| `stackalloc` | `std::array` / local objects |
| `ArrayPool<T>` | `std::pmr` memory resources |
| struct layout attributes | member order, `alignas` |
