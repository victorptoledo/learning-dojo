---
from: csharp
status: draft
concept: "Health endpoints and sized containers"
from_way: "One health check for the load balancer and an App Service plan size for memory."
native_way: "Tagged ASP.NET Core health checks back separate probes, and requests and limits size each container."
sensei_tip: ".NET reads the cgroup limits for its GC heap; set DOTNET_GCHeapHardLimitPercent if the app shares the container with other processes."
---

| .NET world | Kubernetes |
| --- | --- |
| `MapHealthChecks("/health")` | separate `/health/live` and `/health/ready` |
| an App Service plan | requests and limits |
| `IHealthCheck` for the database | readiness only |
| Server GC sizing | the container memory limit |
