---
from: csharp
status: reviewed
concept: "Locks you did not ask for"
from_way: "lock, SemaphoreSlim and Monitor.TryEnter, and EF Core migrations applied at startup."
native_way: "Table and row locks taken by every statement until the end of the transaction, with a queue per table."
sensei_tip: "Coming from SQL Server: PostgreSQL never escalates row locks to table locks, and readers never take shared locks on rows. What bites is DDL waiting in the queue."
---

| C# | PostgreSQL |
| --- | --- |
| Monitor.TryEnter(obj, 2000) | SET lock_timeout = '2s' |
| SemaphoreSlim(1) across instances | pg_try_advisory_xact_lock(key) |
| sp_who2 / blocking chains | pg_stat_activity with pg_blocking_pids |
| WITH (ONLINE = ON) index builds | CREATE INDEX CONCURRENTLY |
| lock escalation | does not exist in PostgreSQL |
