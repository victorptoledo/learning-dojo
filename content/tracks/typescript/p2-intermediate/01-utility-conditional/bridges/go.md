---
from: go
status: draft
concept: "Computing types at compile time"
from_way: "No type-level computation: each struct is written by hand, often generated with go generate."
native_way: "Types derive from types: Omit, Pick, mapped types over keys, conditional types and template literal types."
sensei_tip: "Go keeps types simple on purpose, and that is a good instinct here too: derive a type when it removes duplication, not to show off."
---

| Go | TypeScript |
| --- | --- |
| Separate struct for a patch with pointer fields | `Partial<User>` |
| `go generate` + templates | Mapped types |
| `map[Status]int` | `Record<Status, number>` |
| No equivalent | Conditional types, `infer`, template literal types |
