---
from: python
status: reviewed
concept: "Locks you did not ask for"
from_way: "threading.Lock and RLock, and Django or Alembic migrations that are just a series of ALTER TABLEs."
native_way: "Table and row locks taken by every statement until the end of the transaction, with a queue per table."
sensei_tip: "Alembic and Django run migrations in a transaction. Execute SET LOCAL lock_timeout = '2s' first, and split NOT NULL additions into a NOT VALID check plus a VALIDATE in a second migration."
---

| Python | PostgreSQL |
| --- | --- |
| lock.acquire(timeout=2) | SET lock_timeout = '2s' |
| a lock held across an await | a session idle in transaction holding row locks |
| py-spy dump | pg_stat_activity + pg_blocking_pids |
| a file lock so one cron job runs | pg_try_advisory_xact_lock(key) |
| migrations.AddIndexConcurrently (Django) | CREATE INDEX CONCURRENTLY |
