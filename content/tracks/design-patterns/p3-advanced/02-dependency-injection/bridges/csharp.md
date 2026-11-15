---
from: csharp
status: draft
concept: "Dependency injection"
from_way: "`new` inside services, static helpers, and `IServiceProvider.GetService` calls in business code."
native_way: "Constructor injection everywhere with the built-in container (`builder.Services.AddScoped<IAccounts, SqlAccounts>()`), `Program.cs` as the composition root, and `IOptions<T>` for configuration."
sensei_tip: "Resolving a scoped service from a singleton is the classic .NET DI bug (a captive dependency); enable scope validation in development so the container throws at startup."
---

| C# | DI idea |
| --- | --- |
| constructor parameters | constructor injection |
| `Program.cs` registrations | composition root |
| `AddSingleton`, `AddScoped`, `AddTransient` | scopes |
| `IServiceProvider.GetService` in business code | service locator (avoid) |
| `IEnumerable<IExporter>` | many implementations |
