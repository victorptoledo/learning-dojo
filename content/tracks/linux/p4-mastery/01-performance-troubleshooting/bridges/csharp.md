---
from: csharp
status: draft
concept: "Finding why a service is slow"
from_way: "dotnet-counters, dotnet-trace and dotnet-dump, or Visual Studio's diagnostic tools."
native_way: "System tools first (USE), then the .NET diagnostic tools on the process."
sensei_tip: "dotnet-counters monitor -p PID shows GC, thread pool and exception rates live. If the thread pool queue grows while CPUs are idle, something blocks threads: look for sync-over-async."
---

| C# | Linux |
| --- | --- |
| Task Manager | top, htop |
| dotnet-counters | vmstat, pidstat, then dotnet-counters |
| PerfView | perf, dotnet-trace |
| OutOfMemoryException | GC heap versus the cgroup memory.max |
| Resource Monitor > Disk | iostat -xz |
