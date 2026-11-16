---
from: javascript
status: reviewed
concept: "Locks you did not ask for"
from_way: "A single-threaded event loop with no locks, and migration tools such as Knex, Prisma Migrate or node-pg-migrate."
native_way: "Every statement takes table and row locks until the end of its transaction, and requests queue per table."
sensei_tip: "prisma migrate deploy has no lock_timeout of its own: add SET lock_timeout to the migration SQL, and write CREATE INDEX CONCURRENTLY in a migration that does not run in a transaction."
---

| JavaScript / TypeScript | PostgreSQL |
| --- | --- |
| no locks in your own code | locks taken for you by each statement |
| Promise.race with a timeout | lock_timeout |
| a Redis lock (Redlock) for one worker | pg_try_advisory_xact_lock(key) |
| node --inspect to see what is stuck | pg_stat_activity, pg_blocking_pids |
| a migration file | DDL that can queue behind a long transaction |
