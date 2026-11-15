---
from: go
status: draft
concept: "Concurrent changes that stay correct"
from_way: "sync.Mutex and atomic in one process, and db.BeginTx with sql.TxOptions."
native_way: "Transactions and row locks valid across every instance of the service."
sensei_tip: "Write the transaction as a function and wrap it in a retry loop that checks for 40001 and 40P01; always defer tx.Rollback(), it is a no-op after Commit."
---

| Go | SQL |
| --- | --- |
| tx, _ := db.BeginTx(ctx, nil); defer tx.Rollback() | BEGIN ... ROLLBACK unless COMMIT |
| mu.Lock() | SELECT ... FOR UPDATE |
| atomic.AddInt64(&b, -80) | SET balance = balance - 80 WHERE balance >= 80 |
| sql.TxOptions{Isolation: sql.LevelSerializable} | SERIALIZABLE |
| pgErr.Code == "40001" | retry the whole function |
