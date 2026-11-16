---
from: csharp
status: draft
concept: "Host lifetime"
from_way: "The Generic Host handles SIGTERM, calls IHostApplicationLifetime.ApplicationStopping, waits ShutdownTimeout for Kestrel to drain, and health checks separate liveness and readiness by tags."
native_way: "Your entry point handles SIGTERM, flips a ready flag, closes the server, waits for in-flight requests with a timeout, and exposes /live and /ready."
sensei_tip: "IHostApplicationLifetime.ApplicationStopping is the moment to set ready = false; HostOptions.ShutdownTimeout is your forced exit timer."
---

| ASP.NET Core | Node.js |
| --- | --- |
| `ApplicationStopping` token | The SIGTERM handler |
| `HostOptions.ShutdownTimeout` | Forced exit timer |
| `MapHealthChecks("/live", tags)` | `/live` |
| `MapHealthChecks("/ready", tags)` | `/ready` |
| `IAsyncDisposable` services | Close the pool and clients in order |
