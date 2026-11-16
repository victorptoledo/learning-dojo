---
from: csharp
status: draft
concept: "Virtual memory and .NET"
from_way: "Assuming GC.GetTotalMemory is the process footprint."
native_way: "The GC reserves and commits regions of virtual memory; the working set is what is resident; MemoryMappedFile maps files; containers set GCHeapHardLimit from the cgroup."
sensei_tip: "In containers, check `DOTNET_GCHeapHardLimitPercent`, and use `dotnet-counters` to compare GC heap size with working set."
---

| In C# | Underneath |
| --- | --- |
| `GC.GetTotalMemory` | managed heap bytes |
| `Process.WorkingSet64` | resident memory |
| `MemoryMappedFile` | mmap |
| Server GC | more heaps, more reserved memory |
| `OutOfMemoryException` | the runtime could not commit |
