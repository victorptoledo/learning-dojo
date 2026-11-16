---
from: go
status: draft
concept: "Database internals seen from Go"
from_way: "Using db.Query for each step of what should be one transaction."
native_way: "database/sql's *sql.DB is a pool: consecutive calls may use different connections; *sql.Tx pins one connection for BEGIN … COMMIT; embedded engines such as bbolt (B+tree) and Pebble (LSM) are written in Go."
sensei_tip: "Use `db.BeginTx(ctx, &sql.TxOptions{Isolation: sql.LevelSerializable})` when it matters, and always `defer tx.Rollback()`."
---

| In Go | Underneath |
| --- | --- |
| `db.Exec` | any pooled connection, autocommit |
| `tx, _ := db.BeginTx(...)` | one connection, one transaction |
| `sql.LevelSerializable` | the strictest isolation |
| bbolt | a B+tree key-value store |
| Pebble | an LSM engine (used by CockroachDB) |
