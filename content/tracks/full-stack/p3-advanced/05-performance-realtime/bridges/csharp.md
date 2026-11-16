---
from: csharp
status: reviewed
concept: "SignalR and the rest of the path"
from_way: "SignalR for everything live, and performance work limited to the server's CPU."
native_way: "Measure the page in the browser first; for live updates, SSE (TypedResults.ServerSentEvents in .NET 10) or SignalR with a Redis backplane to fan out across instances."
sensei_tip: "SignalR needs a backplane (Redis or Azure SignalR Service) as soon as you run more than one instance."
---

| .NET | Performance and real-time |
| --- | --- |
| `TypedResults.ServerSentEvents(...)` | Server-Sent Events |
| SignalR + Redis backplane | WebSockets with fan-out |
| `Task.WhenAll` | parallel calls |
| `AsNoTracking` + projections | lighter queries |
