---
from: csharp
status: draft
concept: "Software architecture"
from_way: "Architecture means the solution template: Web, Application, Domain and Infrastructure projects created by `dotnet new` and never questioned."
native_way: "Architecture as the decisions that are expensive to change (project and module boundaries, data ownership, sync calls or messages, deployment units), each justified by the quality attributes that matter here."
sensei_tip: "A clean-architecture template is a starting point, not an architecture. The decisions that matter (one deployable or several, who owns the database, how parts integrate) are still yours to make and record."
---

| .NET world | Architectural question behind it |
| --- | --- |
| one ASP.NET Core app or several services | deployment units, availability, team independence |
| projects and references in a solution | module structure and allowed dependencies |
| one `DbContext` for the whole system | data ownership and coupling |
| MassTransit, Azure Service Bus | synchronous versus event-driven runtime structure |
| health checks, OpenTelemetry | observability as a quality attribute |
| BenchmarkDotNet, NBomber | checking performance scenarios |
