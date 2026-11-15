---
from: csharp
status: draft
concept: "Derived types without code generation"
from_way: "Types are declared explicitly; with expressions and source generators help create variants such as DTOs."
native_way: "Variants are computed by the type system: Partial, Pick, Omit, mapped types and conditional types."
sensei_tip: "C# developers often generate DTOs; in TypeScript, derive them. A conditional type is roughly a compile-time `is` check that returns a type."
---

| C# | TypeScript |
| --- | --- |
| A DTO class per use case | `Pick`, `Omit`, `Partial` |
| `Dictionary<Status, int>` | `Record<Status, number>` |
| `Task<T>` unwrap with `await` | `Awaited<T>` |
| Source generators | Mapped types with `as` |
| No equivalent | Template literal types |
