---
from: csharp
status: draft
concept: "Creational patterns"
from_way: "Constructor overloads, a static `Instance` property with locking, and `ICloneable`."
native_way: "Records with `init` properties and object initialisers replace most builders, `with` expressions make copies, and `AddSingleton` in the DI container replaces hand-written singletons."
sensei_tip: "`var b = a with { Retries = 5 };` is the prototype pattern built into the language for records. Avoid `ICloneable`: it does not say whether the copy is deep or shallow."
---

| C# | Pattern |
| --- | --- |
| `record` + `init` + object initialiser | builder-like construction |
| `static Create(...)` | static factory |
| `with` expressions | prototype |
| `services.AddSingleton<T>()` | singleton by the container |
| `Lazy<T>` | lazy initialisation |
| `IHttpClientFactory` | factory |
