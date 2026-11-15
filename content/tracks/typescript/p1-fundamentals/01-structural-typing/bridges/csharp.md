---
from: csharp
status: draft
concept: "Shapes instead of declared types"
from_way: "Types are nominal: a class fits an interface only when it implements it, and generic types are reified at runtime."
native_way: "Types are structural: anything with the right members fits, and nothing about the type exists at runtime."
sensei_tip: "Anonymous objects in TypeScript are first-class: you can pass `{ name: \"Ada\" }` to any function that needs a name. Use brands where C# would use a strongly typed id record struct."
---

| C# | TypeScript |
| --- | --- |
| `class Dog : INamed` | No declaration needed |
| `string?` with nullable reference types | `string \| undefined` (with `strictNullChecks`) |
| `record struct UserId(string Value)` | `string & { readonly __brand: "UserId" }` |
| `var x = 3;` | `let x = 3;` (inferred `number`) |
| `dynamic` | `any` |
