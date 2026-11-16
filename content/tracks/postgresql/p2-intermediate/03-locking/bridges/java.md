---
from: java
status: reviewed
concept: "Locks you did not ask for"
from_way: "synchronized, ReentrantLock and ReadWriteLock, with jstack thread dumps to find who holds a monitor."
native_way: "Table and row locks taken by every statement until the end of the transaction, a lock queue per table, and pg_blocking_pids as the thread dump."
sensei_tip: "Flyway and Liquibase run each migration in a transaction. Put SET lock_timeout = '2s' at the top of risky migrations and let the deployment retry, or one long report turns a two-millisecond ALTER into an outage."
---

| Java | PostgreSQL |
| --- | --- |
| ReadWriteLock read lock | ACCESS SHARE (SELECT) |
| a writer waiting starves new readers (fair lock) | the lock queue behind a pending ACCESS EXCLUSIVE |
| lock.tryLock(2, SECONDS) | SET lock_timeout = '2s' |
| jstack "waiting to lock" | pg_stat_activity with pg_blocking_pids |
| a JVM-wide named lock | pg_try_advisory_xact_lock(key) |
| ShedLock for scheduled jobs | advisory locks |
