---
from: go
status: draft
concept: "Sum types that Go does not have"
from_way: "No sum types: an interface with a type switch, or a struct with a kind field and several optional fields; no exhaustiveness check."
native_way: "Real unions with a literal tag, narrowed by the compiler, and exhaustiveness through never."
sensei_tip: "The struct-with-a-kind-field pattern from Go becomes safe in TypeScript: each kind gets only its own fields, and the compiler checks every switch."
---

| Go | TypeScript |
| --- | --- |
| `switch v := x.(type) { case string: }` | `if (typeof x === "string")` |
| `interface{}` / `any` | `unknown` |
| Struct with `Kind` and optional fields | Discriminated union |
| No exhaustiveness (linters help) | `assertNever` |
| `if err != nil` | `if (!result.ok)` on a `Result` union |
