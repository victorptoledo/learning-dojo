---
from: go
status: draft
concept: "Structural typing beyond interfaces"
from_way: "Interfaces are satisfied implicitly (structurally), but struct types are nominal: two structs with the same fields are different types."
native_way: "Everything is structural, object types included; a named type alias does not create a distinct type as `type UserID string` does in Go."
sensei_tip: "Go's implicit interfaces are the closest thing to TypeScript, but beware: in Go `type UserID string` is a new type, while in TypeScript `type UserId = string` is just another name. Use a brand."
---

| Go | TypeScript |
| --- | --- |
| Implicit interface satisfaction | Every type works this way |
| `type UserID string` (distinct type) | `type UserId = string` (same type), brand it to make it distinct |
| `[]string` | `string[]` |
| `map[string]int` | `Record<string, number>` or `Map<string, number>` |
| `*string` for optional | `email?: string` |
