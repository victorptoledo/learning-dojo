---
from: csharp
status: draft
concept: "Give teams paved roads instead of a box of parts"
from_way: "dotnet new templates and a Directory.Build.props shared across a solution."
native_way: "The same idea at organisation scale: templates, reusable pipelines and infrastructure modules that every new service starts from."
sensei_tip: ".NET Aspire's service defaults (telemetry, health checks, resilience) are an example of a golden path packaged as code."
---

| C# world | Platform engineering |
| --- | --- |
| `dotnet new` templates | software templates |
| `Directory.Build.props` | shared defaults |
| Aspire service defaults | golden-path building blocks |
| an internal NuGet feed | a platform capability |
