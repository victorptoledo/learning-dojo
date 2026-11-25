---
from: csharp
status: draft
concept: "The runtime under load"
from_way: "The CLR's tiered JIT, server and workstation GC, and dotnet-counters or PerfView."
native_way: "HotSpot's tiered JIT, G1 or ZGC, and JFR with jcmd."
sensei_tip: "Server GC versus workstation GC is roughly G1 versus SerialGC choices; ZGC is the low-pause option like .NET's background GC aims for. dotnet-counters is jcmd plus JFR streaming."
---

| C# | Java 25 |
| --- | --- |
| dotnet-counters | jcmd, JFR event streaming |
| dotnet-trace | JFR recording |
| Server GC | G1 (default) / ZGC |
| DOTNET_GCHeapHardLimit | -Xmx / MaxRAMPercentage |
| ReadyToRun | AOT cache (JEP 483+) |
