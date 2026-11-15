---
from: csharp
status: draft
concept: "Interfaces and pattern matching"
from_way: "Interfaces implemented explicitly with a colon, an IService for every class to please the DI container, default interface methods, is and as casts, and switch expressions with type patterns."
native_way: "Implicit interfaces kept small and owned by the consumer, constructors that return concrete types, and type assertions and type switches for pattern matching on types."
sensei_tip: "Go has no DI container by default: you pass dependencies to NewX functions in main. Small consumer-side interfaces make that wiring and your test fakes trivial."
---

| C# | Go |
| --- | --- |
| `class Rect : IShape` | nothing to declare: `Rect` has the methods |
| `IUserService` next to `UserService` | an interface in the consumer package, if any |
| `Stream` | `io.Reader` / `io.Writer` |
| `override ToString()` | `String() string` |
| `if (s is Rect r)` | `if r, ok := s.(Rect); ok` |
| `s as Rect` | `r, ok := s.(Rect)` |
| `switch (s) { case Rect r: ... }` | `switch v := s.(type) { case Rect: ... }` |
| `object` | `any` |
| `IDisposable` | `io.Closer` |
