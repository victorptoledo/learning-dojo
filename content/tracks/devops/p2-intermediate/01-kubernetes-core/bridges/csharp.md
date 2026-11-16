---
from: csharp
status: draft
concept: "Declare the service, controllers keep it running"
from_way: "An IIS web farm with ARR, and app pool recycling when something hangs."
native_way: "Replicas replace the farm, the Service replaces ARR, and the liveness probe replaces app pool recycling."
sensei_tip: "ASP.NET Core health checks with tags give you separate readiness and liveness endpoints (MapHealthChecks with a Predicate)."
---

| C# world | Kubernetes |
| --- | --- |
| an IIS web farm | a Deployment |
| app pool recycling | liveness restarts |
| `appsettings.json` | a ConfigMap |
| user secrets / Key Vault | Secrets or an external secrets operator |
