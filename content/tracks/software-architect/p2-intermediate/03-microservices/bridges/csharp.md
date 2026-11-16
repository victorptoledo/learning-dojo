---
from: csharp
status: draft
concept: "Microservices"
from_way: "Services that share a `Company.Domain` NuGet package of entities and one SQL Server database, calling each other with `HttpClient` in chains."
native_way: "Services with their own database, contracts packages or OpenAPI, `HttpClientFactory` with Polly resilience, MassTransit or NServiceBus for messaging, and .NET Aspire or OpenTelemetry for observability."
sensei_tip: "`Microsoft.Extensions.Http.Resilience` adds timeouts, retries and circuit breakers to every `HttpClient` with one call. Without them, partial failure turns into a cascade."
---

| C# / .NET | Microservice concern |
| --- | --- |
| an ASP.NET Core service with its own database | independent deployability, data ownership |
| `AddStandardResilienceHandler()`, Polly | partial failure |
| MassTransit, NServiceBus, Azure Service Bus | asynchronous integration |
| Pact.NET | contract tests |
| .NET Aspire, OpenTelemetry | observability across services |
| a shared `Domain` NuGet package | distributed monolith warning |
