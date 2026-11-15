---
from: csharp
status: draft
concept: "Pattern matching on unions"
from_way: "Class hierarchies or records with switch expressions and type patterns; no built-in union types for arbitrary types."
native_way: "Unions are built in: string | number, or tagged object unions narrowed by switch on a literal property."
sensei_tip: "Where C# needs an abstract record and subclasses (or a OneOf library), TypeScript writes `type Result = Ok | Err` in one line."
---

| C# | TypeScript |
| --- | --- |
| `abstract record Shape; record Circle(double R) : Shape;` | `type Shape = { kind: "circle"; r: number } \| ...` |
| `shape switch { Circle c => ..., _ => throw }` | `switch (shape.kind)` + `assertNever` |
| `is string s` | `typeof x === "string"` |
| `string?` | `string \| undefined` |
| `object` | `unknown` |
