---
from: python
status: draft
concept: "Database internals seen from Python"
from_way: "Forgetting that Python DB-API connections open transactions implicitly."
native_way: "DB-API drivers start a transaction on the first statement and need commit(); sqlite3 is a B-tree engine with a rollback journal or WAL in your process; ORMs generate SQL that the planner may execute very differently than you expect."
sensei_tip: "Turn on SQLite's WAL mode (`PRAGMA journal_mode=WAL`) for concurrent readers, and look at the plan of ORM queries with EXPLAIN."
---

| In Python | Underneath |
| --- | --- |
| `conn.commit()` | forces the WAL record of the transaction |
| `sqlite3` | a B-tree database in a file |
| `PRAGMA journal_mode=WAL` | write-ahead log mode |
| `executemany` | batched statements |
| `EXPLAIN QUERY PLAN` | the plan SQLite chose |
