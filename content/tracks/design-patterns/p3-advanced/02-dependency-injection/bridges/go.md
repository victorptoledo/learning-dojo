---
from: go
status: draft
concept: "Dependency injection"
from_way: "Package-level variables for the database and clients, initialised in `init()`."
native_way: "Constructor functions that take interfaces (`NewTransferService(accounts, rates)`), all wiring in `main`, and Wire or fx only for very large graphs."
sensei_tip: "Go programmers mostly do DI by hand, and it scales further than you would think: `main` is the composition root, and a compile error tells you when a dependency is missing."
---

| Go | DI idea |
| --- | --- |
| `NewService(store Store, now func() time.Time)` | constructor injection |
| `main.go` wiring | composition root |
| google/wire | compile-time container |
| uber/fx | runtime container |
| package-level `var db *sql.DB` | global state (avoid) |
