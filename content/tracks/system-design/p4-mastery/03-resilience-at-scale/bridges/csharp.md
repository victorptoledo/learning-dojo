---
from: csharp
status: draft
concept: "Containing failure"
from_way: "Polly policies and health checks inside one ASP.NET Core service."
native_way: "System-wide containment: SLOs and budgets, zone-redundant capacity, cells, shuffle sharding and rehearsed multi-region recovery."
sensei_tip: "Azure's deployment stamps pattern is the same idea as cells: independent copies of the whole stack, each serving a group of tenants, deployed one stamp at a time."
---

| C# / .NET | Resilience concept |
| --- | --- |
| OpenTelemetry metrics with histograms | SLIs |
| deployment stamps (Azure Architecture Center) | cells |
| `Microsoft.FeatureManagement` | degradation switches |
| Azure Chaos Studio | failure drills |
| Front Door with priority routing | regional failover |
