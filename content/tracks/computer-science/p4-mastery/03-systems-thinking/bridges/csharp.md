---
from: csharp
status: draft
concept: "Observing every layer from .NET"
from_way: "Attaching a debugger in production."
native_way: "dotnet-counters shows GC, thread pool and exception rates live; dotnet-trace records EventPipe traces; dotnet-dump captures memory; OpenTelemetry exports traces and metrics."
sensei_tip: "Watch `dotnet-counters monitor` for thread-pool queue length: a growing queue with low CPU usually means blocking calls starving the pool."
---

| In C# | Underneath |
| --- | --- |
| `dotnet-counters` | live runtime metrics |
| `dotnet-trace` | CPU and event traces |
| `dotnet-dump analyze` | heap and stacks |
| thread-pool queue length | Little's law in a single number |
| OpenTelemetry | traces across services |
