---
from: csharp
status: draft
concept: "Observer, command and chain of responsibility"
from_way: "Direct calls to each service, and events that are never unsubscribed (memory leaks)."
native_way: "`event` and delegates or `IObservable<T>` for observer, MediatR requests as commands with a mediator, and ASP.NET Core middleware as chain of responsibility."
sensei_tip: "MediatR combines three patterns from this lesson: requests are commands, the `IMediator` is a mediator, and pipeline behaviours form a chain of responsibility around each handler."
---

| C# | Pattern |
| --- | --- |
| `event EventHandler<T>`, `+=` / `-=` | observer |
| `IObservable<T>` / Rx.NET | observer with operators |
| `ICommand` (WPF), MediatR `IRequest` | command |
| `app.Use(...)` middleware | chain of responsibility |
| MediatR `IPipelineBehavior` | pipeline |
