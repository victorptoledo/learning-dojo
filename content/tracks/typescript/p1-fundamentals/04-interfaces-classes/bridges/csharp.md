---
from: csharp
status: draft
concept: "Interfaces, classes and overloads"
from_way: "Nominal interfaces, access modifiers enforced at runtime, primary constructors, method overloads with separate bodies, properties with get and set."
native_way: "Structural interfaces, private and protected checked only at compile time, #fields enforced at runtime, overload signatures sharing one body."
sensei_tip: "TypeScript's parameter properties look like C# primary constructors, but they are not erasable: in codebases that run .ts files directly, declare fields explicitly."
---

| C# | TypeScript |
| --- | --- |
| `class Person(string name)` (primary constructor) | Declared field + constructor assignment |
| `public string Name { get; }` | `readonly name: string` or `get name()` |
| `private` (runtime enforced) | `#field` (runtime) / `private` (compile time) |
| `override` | `override` |
| `Func<int, string>` | `(n: number) => string` |
| `params string[] parts` | `...parts: string[]` |
