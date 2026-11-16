---
from: go
status: reviewed
concept: "Locks you did not ask for"
from_way: "sync.Mutex and sync.RWMutex, with goroutine dumps to find a deadlock."
native_way: "Table and row locks taken by every statement until the end of the transaction, a queue per table, and deadlock detection by the server."
sensei_tip: "sync.RWMutex blocks new readers once a writer waits, exactly like PostgreSQL's lock queue. An ALTER TABLE is that writer: give it a lock_timeout."
---

| Go | PostgreSQL |
| --- | --- |
| RWMutex.RLock | ACCESS SHARE |
| a waiting Lock() blocking new RLock()s | the lock queue behind ACCESS EXCLUSIVE |
| TryLock / context deadline | lock_timeout |
| "all goroutines are asleep - deadlock!" | deadlock detected, SQLSTATE 40P01 |
| a leader-election lock | pg_try_advisory_xact_lock(key) |
