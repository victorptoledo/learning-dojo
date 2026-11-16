---
from: csharp
status: draft
concept: "Hexagonal and clean architecture"
from_way: "A `Domain` project that references EF Core for its attributes, and controllers that use `DbContext` directly."
native_way: "A `Domain` project with no package references, interfaces for the driven ports in `Domain` or `Application`, an `Infrastructure` project that implements them, and `Program.cs` as composition root; MSBuild references enforce the rule."
sensei_tip: "Project references are a free fitness function: if `Domain.csproj` has no references, nothing in it can depend on infrastructure. Keep it that way."
---

| C# / .NET | Hexagonal idea |
| --- | --- |
| a `Domain` project with no references | the core |
| `IOrders` interface in `Domain` | driven port |
| controllers, `BackgroundService` consumers | driving adapters |
| EF Core repository in `Infrastructure` | driven adapter |
| `Program.cs` service registrations | composition root |
| project references, NetArchTest | the dependency rule enforced |
