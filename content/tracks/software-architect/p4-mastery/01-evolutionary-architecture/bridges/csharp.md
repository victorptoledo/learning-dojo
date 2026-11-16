---
from: csharp
status: draft
concept: "Evolutionary architecture"
from_way: "A new .NET solution built in parallel to the old .NET Framework application, with a planned weekend cut-over."
native_way: "NetArchTest or ArchUnitNET rules in CI, BenchmarkDotNet and NBomber thresholds, `dotnet list package --vulnerable`, SLO alerts, `Microsoft.FeatureManagement` toggles, and YARP as a strangler fig proxy for incremental ASP.NET migration."
sensei_tip: "Microsoft's own guidance for moving ASP.NET Framework apps to ASP.NET Core is a strangler fig: YARP forwards unmigrated routes to the old app while migrated ones are served by the new one."
---

| C# / .NET | Evolutionary idea |
| --- | --- |
| NetArchTest, ArchUnitNET | structural fitness function |
| BenchmarkDotNet, NBomber | performance fitness function |
| `dotnet list package --vulnerable` | security fitness function |
| Application Insights or OpenTelemetry alerts | continuous fitness function |
| `Microsoft.FeatureManagement` | branch by abstraction |
| YARP reverse proxy | strangler fig facade |
