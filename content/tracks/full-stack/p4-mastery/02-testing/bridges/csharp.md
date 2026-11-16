---
from: csharp
status: reviewed
concept: "WebApplicationFactory and real databases"
from_way: "The EF Core in-memory provider in tests, which ignores constraints and transactions."
native_way: "WebApplicationFactory for HTTP-level tests, Testcontainers for .NET with PostgreSQL or SQL Server, Pact for contracts, and Playwright for .NET for journeys."
sensei_tip: "The EF Core in-memory provider is not a relational database; Microsoft's own docs advise against it for testing."
---

| .NET | Across the stack |
| --- | --- |
| EF Core InMemory | Testcontainers with the real engine |
| `WebApplicationFactory<Program>` | HTTP-level tests |
| PactNet | consumer-driven contracts |
| Playwright for .NET | end-to-end journeys |
