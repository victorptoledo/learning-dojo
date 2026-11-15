---
from: csharp
status: draft
concept: "Running services in production"
from_way: "Web Deploy or Octopus to IIS servers, appsettings per environment and app pool recycling."
native_way: "One image promoted through environments, configuration from environment variables, health checks mapped to probes, and rolling updates by the orchestrator."
sensei_tip: "Tag health checks in ASP.NET Core (live, ready) and map them to separate endpoints, so a slow database makes a pod unready instead of restarted."
---

| C# / .NET | Production containers |
| --- | --- |
| Web Deploy to IIS | push a digest, roll it out |
| `appsettings.Production.json` | environment variables |
| app pool recycling | replacing replicas |
| `MapHealthChecks` with tags | liveness and readiness |
