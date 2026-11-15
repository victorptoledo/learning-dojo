---
from: csharp
status: draft
concept: "SOLID principles"
from_way: "A fat `OrderService` that news up its repositories and reads `DateTime.Now`."
native_way: "Classes split by reason to change, dependencies injected through the constructor by the built-in container, `TimeProvider` injected for time, small role interfaces."
sensei_tip: ".NET 8 added `TimeProvider` for the same reason Java has `Clock`: time is a dependency. Inject it and use `FakeTimeProvider` in tests."
---

| C# | SOLID idea |
| --- | --- |
| separate controllers, services, repositories | single responsibility |
| adding an `IMiddleware` or a filter | open/closed |
| `ReadOnlyCollection<T>` implementing `IList<T>` | a Liskov surprise |
| `IReadOnlyList<T>` vs `IList<T>` | interface segregation |
| constructor injection + `TimeProvider` | dependency inversion |
