---
from: java
status: draft
concept: "Sealed hierarchies vs discriminated unions"
from_way: "A sealed interface with records as permitted subtypes, matched with switch pattern matching (Java 21+), and exhaustiveness checked by the compiler."
native_way: "A union of object types with a literal tag field, narrowed by switch on the tag; exhaustiveness via a never check."
sensei_tip: "A discriminated union is a sealed interface without the classes: plain objects with a kind field. Reach for it wherever you would write a sealed hierarchy of records."
---

| Java | TypeScript |
| --- | --- |
| `sealed interface Shape permits Circle, Square` | `type Shape = Circle \| Square` |
| `record Circle(double r) implements Shape` | `type Circle = { kind: "circle"; r: number }` |
| `switch (s) { case Circle c -> ... }` | `switch (s.kind) { case "circle": ... }` |
| Compiler checks exhaustiveness | `default: assertNever(s)` |
| `instanceof` pattern | `instanceof` for classes, `in` or tags for objects |
| `Object` | `unknown` |
