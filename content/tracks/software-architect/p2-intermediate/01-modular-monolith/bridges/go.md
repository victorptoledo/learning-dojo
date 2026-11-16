---
from: go
status: draft
concept: "Modular monolith"
from_way: "One binary with packages that all share a `models` package and one `*sql.DB` used for every query."
native_way: "One package tree per module with `internal/` for its insides and a small exported api package, its own store, and `main` wiring the modules; channels or a tiny event bus for in-process events."
sensei_tip: "Put each module's implementation under `orders/internal/...`: Go then refuses any import from outside `orders`, which is a module boundary enforced by the compiler."
---

| Go | Modular monolith idea |
| --- | --- |
| `orders/`, `catalog/` package trees | modules |
| exported package `orders` with a small interface | published interface |
| `orders/internal/...` | hidden implementation, compiler-enforced |
| a store per module | data ownership |
| a small typed event bus | in-process events |
| `cmd/app/main.go` | composition root |
