---
from: java
status: draft
concept: "Database internals seen from Java"
from_way: "Treating the database as a remote HashMap."
native_way: "JDBC sends SQL to a server that parses, plans and executes it over B+tree indexes and a WAL; the isolation level and autocommit mode decide what your transaction sees and when it hits disk; embedded engines (H2, SQLite, RocksDB JNI) run in your process."
sensei_tip: "Set the isolation level explicitly where it matters, retry serialization failures (SQLState 40001), and batch writes so commits share one WAL flush."
---

| In Java | Underneath |
| --- | --- |
| `connection.setAutoCommit(false)` | one transaction for many statements |
| `TRANSACTION_SERIALIZABLE` | SSI on PostgreSQL: be ready to retry |
| `addBatch` / `executeBatch` | fewer round trips and commits |
| `SELECT ... FOR UPDATE` | explicit row locks against write skew |
| RocksDB JNI | an LSM engine inside the JVM |
