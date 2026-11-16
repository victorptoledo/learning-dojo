---
from: go
status: draft
concept: "Tactical DDD"
from_way: "Exported struct fields changed from anywhere, and rules in handlers."
native_way: "Small value types with constructors that validate, aggregate root structs with unexported fields and methods, a repository interface per root, and events collected in a slice on the root."
sensei_tip: "Unexported fields plus a `New...` constructor are Go's way to make invalid states unrepresentable outside the package; the package becomes the aggregate's boundary."
---

| Go | Tactical DDD idea |
| --- | --- |
| `type Money struct{ cents int64; currency string }` | value object |
| `type OrderID string` | identity as a value |
| a struct with unexported fields and methods | aggregate root |
| `type Orders interface { Find(...); Save(...) }` | repository per aggregate |
| `o.events = append(o.events, ...)` | domain events recorded by the root |
| a version checked in `UPDATE ... WHERE version = $n` | concurrency on the aggregate boundary |
