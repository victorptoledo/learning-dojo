---
from: csharp
status: draft
concept: "From components to a whole system"
from_way: "Designing an ASP.NET Core solution with clean layers and EF Core."
native_way: "Designing the system: data ownership, consistency boundaries, async flows, capacity and failure containment."
sensei_tip: "The Azure Architecture Center's reference architectures are worked end-to-end designs; compare one with your own design for the exercise and note every difference as a trade-off."
---

| C# habit | End-to-end design habit |
| --- | --- |
| a solution with projects per layer | services with data ownership |
| SignalR hubs | stateful connection gateways |
| `SaveChangesAsync` in one DbContext | one consistent step per business operation |
| Application Insights | SLIs, burn-rate alerts, distributed traces |
| a Visio diagram | ADRs plus a request-path diagram |
