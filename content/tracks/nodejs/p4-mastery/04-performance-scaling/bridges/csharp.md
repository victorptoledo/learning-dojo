---
from: csharp
status: draft
concept: "Diagnostics and scale"
from_way: "dotnet-trace and dotnet-counters, dotnet-gcdump for memory, server GC with container-aware limits, BenchmarkDotNet, and Kestrel using every core."
native_way: "--cpu-prof and perf_hooks, heap snapshots, --max-old-space-size, careful microbenchmarks, and one process per core."
sensei_tip: "dotnet-counters' thread-pool queue length is to .NET what event loop delay is to Node.js: the first sign that work waits. Scale-out replaces the thread pool."
---

| C# / .NET | Node.js |
| --- | --- |
| dotnet-trace | `--cpu-prof` |
| dotnet-gcdump | Heap snapshot |
| `GCHeapHardLimit` | `--max-old-space-size` |
| BenchmarkDotNet | mitata |
| `IMemoryCache` with size limits | lru-cache |
| Kestrel on all cores | Replicas / `cluster` |
