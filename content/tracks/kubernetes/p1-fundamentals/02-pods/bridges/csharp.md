---
from: csharp
status: draft
concept: "A Pod is a co-located group, scaled as one"
from_way: "A Windows server runs the web app, an agent service and a startup task, all sharing the machine."
native_way: "A Pod packages the app container with sidecars and init containers that share its network and volumes, and is scaled as a unit."
sensei_tip: "ASP.NET Core handles SIGTERM through IHostApplicationLifetime; raise HostOptions.ShutdownTimeout if requests take longer than the default to drain."
---

| .NET world | Kubernetes |
| --- | --- |
| `dotnet ef database update` before start | an init container |
| an agent Windows service next to the app | a sidecar |
| `ApplicationStopping` | handling SIGTERM within the grace period |
| solution folders | namespaces |
