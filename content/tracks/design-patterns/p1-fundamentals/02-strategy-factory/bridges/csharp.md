---
from: csharp
status: draft
concept: "Strategy and factory"
from_way: "An `IStrategy` interface, one class per variant, and a `StrategyFactory` with a switch."
native_way: "`Func<T, TResult>` delegates or small interfaces for strategies; the DI container registers implementations and keyed services pick one by name."
sensei_tip: ".NET 8 keyed services (`AddKeyedSingleton<IShippingRule, Express>(\"express\")` and `[FromKeyedServices(\"express\")]`) are a registry built into the container."
---

| C# | Pattern |
| --- | --- |
| `list.Sort(comparison)` | strategy |
| `Dictionary<string, Func<Parcel, long>>` | registry |
| `static Create(...)` methods | static factory |
| keyed services in DI | registry by the container |
| `IEnumerable<IRule>` injection | all strategies of a type |
