---
from: javascript
status: draft
concept: "Structural types and duck typing"
from_way: "Duck typing in JavaScript and structural interfaces in TypeScript, typeof and instanceof checks, union types narrowed by checks, and streams as objects with read and write methods."
native_way: "Interfaces are structural like TypeScript's, but they describe only methods, are checked by the compiler for every assignment, and are narrowed with type assertions and type switches."
sensei_tip: "TypeScript interfaces describe data shapes; Go interfaces describe behaviour. Data shapes in Go are structs."
---

| TypeScript | Go |
| --- | --- |
| `interface Shape { area(): number }` | `type Shape interface{ Area() float64 }` |
| any object with `area()` fits | any type with `Area() float64` fits |
| `interface Point { x: number }` (data) | `type Point struct{ X float64 }` |
| Node `Readable` / `Writable` streams | `io.Reader` / `io.Writer` |
| `typeof x === "string"` | `s, ok := x.(string)` |
| narrowing a union in a `switch` | `switch v := x.(type)` |
| `unknown` | `any` |
| `toString()` | `String() string` |
