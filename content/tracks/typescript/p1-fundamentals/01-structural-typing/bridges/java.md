---
from: java
status: draft
concept: "Structural vs nominal typing"
from_way: "A class is assignable to an interface only when it declares `implements`; two classes with the same methods are unrelated types."
native_way: "Any value with the required properties is assignable, whatever its class or declaration; types are shapes."
sensei_tip: "Stop thinking in class hierarchies: describe the shape a function needs with a small interface, and any object that fits can be passed, plain object literals included."
---

| Java | TypeScript |
| --- | --- |
| `class Dog implements Named` required | `Dog` is a `Named` if it has `name` |
| `record Point(int x, int y)` | `type Point = { x: number; y: number }` or a plain object |
| `final` field | `readonly` property (compile time only) |
| `List<String>` | `string[]` |
| `Optional<String>` | `string \| undefined`, or an optional property `email?` |
| Distinct wrapper class `UserId` | Branded type `string & { __brand: "UserId" }` |
