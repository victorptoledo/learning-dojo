---
from: csharp
status: draft
concept: "Ports and adapters"
from_way: "Controllers that use `DbContext` and vendor clients directly, in one project."
native_way: "Separate projects for Domain, Application and Infrastructure, where Domain references nothing; ports as interfaces in Application, adapters in Infrastructure, wiring in the web project's `Program.cs`."
sensei_tip: "Project references enforce the rule for free: if the Domain project does not reference Infrastructure, a violation is a compile error. NetArchTest adds finer rules."
---

| C# | Ports and adapters |
| --- | --- |
| `IExchangeRates` in Application | driven port |
| ASP.NET Core controllers | driving adapters |
| EF Core repository in Infrastructure | driven adapter |
| `Program.cs` | composition root |
| project references, NetArchTest | enforcing the dependency rule |
