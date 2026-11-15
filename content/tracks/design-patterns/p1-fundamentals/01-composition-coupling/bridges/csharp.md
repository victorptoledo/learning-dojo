---
from: csharp
status: draft
concept: "Composition, coupling and cohesion"
from_way: "Base classes with `virtual` methods everywhere and `protected` state, and static helper classes."
native_way: "`sealed` classes by default, constructor-injected interfaces, records for data, and extension methods for small helpers instead of a utility base class."
sensei_tip: "C# makes methods non-virtual by default, so overriding is opt-in. Combine that with `sealed` on classes you did not design for extension."
---

| C# | Idea |
| --- | --- |
| `sealed class`, `record` | composition over inheritance |
| interface fields set in the constructor | message coupling |
| `static` mutable fields | common coupling |
| `virtual` everywhere | fragile base classes |
| extension methods | helpers without inheritance |
