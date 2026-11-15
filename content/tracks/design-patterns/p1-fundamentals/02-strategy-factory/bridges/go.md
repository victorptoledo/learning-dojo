---
from: go
status: draft
concept: "Strategy and factory"
from_way: "A `switch method {}` that grows, or an interface type with a struct per variant even when each has no state."
native_way: "Function types (`type Rule func(Parcel) int64`) for stateless strategies, interfaces for stateful ones, a map as the registry and `NewX` constructor functions as factories."
sensei_tip: "`database/sql` is a registry: drivers call `sql.Register(\"postgres\", driver)` in their `init`, and `sql.Open(\"postgres\", dsn)` picks one by name."
---

| Go | Pattern |
| --- | --- |
| `sort.Slice(s, less)` | strategy |
| `map[string]Rule` | registry |
| `NewClient(...)` | factory function |
| `sql.Register` / `sql.Open` | registry with factory |
| `http.HandlerFunc` | a function adapted to an interface |
