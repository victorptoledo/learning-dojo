---
from: csharp
status: draft
concept: "dotnet-trace, dotnet-counters and BenchmarkDotNet"
from_way: "dotnet-trace and PerfView for CPU, dotnet-gcdump and dotnet-dump for memory, dotnet-counters for live metrics, BenchmarkDotNet with MemoryDiagnoser, and Server or Workstation GC settings."
native_way: "pprof for CPU and memory, net/http/pprof for live services, runtime/metrics for counters, go test benchmarks with -benchmem, and one GC tuned by GOGC and GOMEMLIMIT."
sensei_tip: "[MemoryDiagnoser] is -benchmem: in both worlds, allocations per operation is the number that usually explains a slow hot path."
---

| .NET | Go |
| --- | --- |
| `dotnet-trace collect` | `go tool pprof .../debug/pprof/profile` |
| `dotnet-gcdump` | heap profile |
| `dotnet-dump analyze` + `clrstack` | goroutine profile |
| `dotnet-counters monitor` | `runtime/metrics`, `/debug/vars` |
| BenchmarkDotNet `[MemoryDiagnoser]` | `go test -bench . -benchmem` |
| `<ServerGarbageCollector>` | nothing to choose; `GOGC` |
| `GCHeapHardLimit` | `GOMEMLIMIT` |
| tiered JIT, Dynamic PGO | PGO from `default.pgo` |
