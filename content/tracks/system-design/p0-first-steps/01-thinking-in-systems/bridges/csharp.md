---
from: csharp
status: draft
concept: "From one application to a system"
from_way: "One ASP.NET Core app with dependency injection, where a service call is an in-process method."
native_way: "Services and databases on different machines, connected by networks that add latency and fail independently."
sensei_tip: "HttpClient calls default to a 100-second timeout. In a system, a slow dependency without a short timeout ties up your threads; Little's law tells you how fast."
---

| C# habit | System design view |
| --- | --- |
| `await _service.GetAsync(id)` in-process | a remote call with a timeout and retries |
| BenchmarkDotNet | p99 latency under production load |
| `IMemoryCache` | one cache per instance vs a shared cache |
| one App Service instance | scale-out behind a load balancer |
| Azure SLA page | availability of the composition, not of one part |
