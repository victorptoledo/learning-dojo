---
from: go
status: draft
concept: "Repository and unit of work"
from_way: "Handlers that hold a `*sql.DB` and write SQL inline."
native_way: "A repository interface per aggregate defined by the service package, implemented over `database/sql` or sqlc-generated code, with an explicit `*sql.Tx` passed through the use case."
sensei_tip: "Go has no implicit transactions: a common shape is `store.WithTx(ctx, func(tx Store) error { ... })`, which commits when the function returns nil and rolls back otherwise."
---

| Go | Pattern |
| --- | --- |
| `type Accounts interface { Find(ctx, id) (Account, error) }` | repository |
| `db.BeginTx`, `tx.Commit`, `tx.Rollback` | unit of work boundary |
| sqlc-generated queries | table data gateway |
| GORM `db.Save(&obj)` | active record-ish |
| a map-backed fake | in-memory repository for tests |
