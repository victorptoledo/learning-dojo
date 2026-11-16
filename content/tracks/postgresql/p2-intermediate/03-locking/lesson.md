# Locks in Depth: Lock Modes, Waits, Deadlocks and Advisory Locks

The SQL track's lesson
[Transactions, Isolation and Locking](../../../sql/p3-advanced/03-transactions/lesson.md)
covered isolation levels, `SELECT ... FOR UPDATE`, `SKIP LOCKED` and deadlocks from the
application's side. This lesson is the database's side: the eight table lock modes and which
statements take them, the **lock queue** that turns a harmless `ALTER TABLE` into an outage, how
to see who blocks whom, and advisory locks.

## Table locks: eight modes

Every statement takes a lock on each table it touches, held **until the end of the
transaction**. The modes, from weakest to strongest, with what takes them:

| Mode | Taken by | Conflicts with |
| --- | --- | --- |
| `ACCESS SHARE` | `SELECT` | `ACCESS EXCLUSIVE` only |
| `ROW SHARE` | `SELECT ... FOR UPDATE / FOR SHARE` | `EXCLUSIVE`, `ACCESS EXCLUSIVE` |
| `ROW EXCLUSIVE` | `INSERT`, `UPDATE`, `DELETE`, `MERGE` | `SHARE` and stronger |
| `SHARE UPDATE EXCLUSIVE` | `VACUUM`, `ANALYZE`, `CREATE INDEX CONCURRENTLY`, some `ALTER TABLE` (`SET (storage parameter)`, `VALIDATE CONSTRAINT`) | itself and stronger |
| `SHARE` | `CREATE INDEX` (without `CONCURRENTLY`) | `ROW EXCLUSIVE` and stronger: **blocks writes** |
| `SHARE ROW EXCLUSIVE` | `CREATE TRIGGER`, `ADD FOREIGN KEY` (on both tables) | `ROW EXCLUSIVE` and stronger |
| `EXCLUSIVE` | `REFRESH MATERIALIZED VIEW CONCURRENTLY` | everything but `ACCESS SHARE` |
| `ACCESS EXCLUSIVE` | most `ALTER TABLE`, `DROP`, `TRUNCATE`, `VACUUM FULL`, `REINDEX` (without `CONCURRENTLY`), `LOCK TABLE` | **everything, even `SELECT`** |

Two consequences worth remembering:

- Reads and writes do not block each other (`ACCESS SHARE` and `ROW EXCLUSIVE` do not conflict);
  that is MVCC.
- `CREATE INDEX` blocks writes for the whole build; `CREATE INDEX CONCURRENTLY` does not (it is
  slower, cannot run in a transaction block, and leaves an `INVALID` index if it fails).

## The lock queue: how a fast ALTER TABLE causes an outage

Lock requests wait in a **queue** per table, and a new request must wait behind any queued
request it conflicts with, not only behind the holders. Picture this:

```text
10:00:00  report      SELECT ... FROM orders (runs for 5 minutes)     holds ACCESS SHARE
10:00:05  migration   ALTER TABLE orders ADD COLUMN note text          wants ACCESS EXCLUSIVE: waits for report
10:00:06  every API   SELECT ... FROM orders                           wants ACCESS SHARE: waits behind migration
```

The `ALTER TABLE` itself would take 2 ms, but while it waits, **every new query on the table
waits behind it**. Within seconds the connection pool is full and the API is down. The fix is
to make DDL give up quickly and retry:

```sql
SET lock_timeout = '2s';
ALTER TABLE orders ADD COLUMN note text;   -- fails with 55P03 after 2 s instead of queueing forever
```

Run migrations with a short `lock_timeout` and a retry loop, and avoid long transactions on hot
tables.

## Safe schema changes

| Change | Risky form | Safe form |
| --- | --- | --- |
| add a column with a default | (fine since PostgreSQL 11: no rewrite for a constant default) | `ADD COLUMN c text DEFAULT 'x'` with `lock_timeout` |
| add a `NOT NULL` | `ALTER COLUMN c SET NOT NULL` scans the table under `ACCESS EXCLUSIVE` | `ADD CONSTRAINT c_nn CHECK (c IS NOT NULL) NOT VALID`; then `VALIDATE CONSTRAINT c_nn` (weaker lock); then `SET NOT NULL` uses the valid check and skips the scan |
| add a `CHECK` or foreign key | validates every row while blocking writes | `ADD CONSTRAINT ... NOT VALID`, then `VALIDATE CONSTRAINT ...` in a separate transaction |
| add an index | `CREATE INDEX` blocks writes | `CREATE INDEX CONCURRENTLY` (outside a transaction) |
| change a column's type | rewrites the table under `ACCESS EXCLUSIVE` | add a new column, backfill in batches, switch, drop the old one |

`NOT VALID` means "check new and changed rows from now on, but do not scan the existing ones
yet". `VALIDATE CONSTRAINT` then scans with a `SHARE UPDATE EXCLUSIVE` lock, so reads and writes
continue.

## Row locks

Row locks are not kept in memory: they are written into the tuple's `xmax`, so locking a million
rows costs no shared memory (but does write to the pages). Four strengths:

| Clause | Blocks | Taken implicitly by |
| --- | --- | --- |
| `FOR UPDATE` | all other row locks and changes | `DELETE`, an `UPDATE` that changes a key column |
| `FOR NO KEY UPDATE` | the same except `FOR KEY SHARE` | an `UPDATE` that changes no key column |
| `FOR SHARE` | changes | |
| `FOR KEY SHARE` | deletes and key changes | **foreign key checks**: inserting an order locks its customer row this way |

The last line explains surprising waits: inserting into `orders` takes `FOR KEY SHARE` on the
referenced `customers` row, so it waits for a transaction that is deleting that customer.

A session that waits for a row lock waits on the **transaction** holding it
(`locktype = 'transactionid'` in `pg_locks`), because the lock lives in the tuple.

## Who is blocking whom

```sql
SELECT w.pid AS waiting_pid, w.application_name AS waiting, w.wait_event_type, left(w.query, 50) AS waiting_query,
       b.pid AS blocking_pid, b.application_name AS blocking, b.state AS blocking_state,
       now() - b.xact_start AS blocking_for, left(b.query, 50) AS blocking_query
FROM pg_stat_activity AS w
CROSS JOIN LATERAL unnest(pg_blocking_pids(w.pid)) AS bp(pid)
JOIN pg_stat_activity AS b ON b.pid = bp.pid
ORDER BY blocking_for DESC;
```

- `pg_blocking_pids(pid)` returns the processes that block `pid`: the holders of conflicting
  locks **and** the queued requests ahead of it that conflict with it.
- `wait_event_type = 'Lock'` (with `wait_event` such as `relation`, `transactionid`, `tuple`)
  means a heavyweight lock wait.
- A blocker in state **idle in transaction** is the classic culprit: a session that changed a
  row and then stopped talking. Its `query` column shows its **last** statement.
- `pg_locks` has the raw data: `locktype`, `relation`, `mode`, `granted`, `pid`.
- `log_lock_waits = on` logs every wait longer than `deadlock_timeout` (1 s).

To stop a blocker: `SELECT pg_cancel_backend(pid)` cancels its current statement;
`SELECT pg_terminate_backend(pid)` ends its session (and rolls back its transaction). For an idle
in transaction session only the second helps.

## Deadlocks

When a session has waited `deadlock_timeout` (1 s), PostgreSQL checks the wait graph for a cycle
and, if it finds one, aborts one of the transactions with SQLSTATE `40P01`. The log shows both
statements. Prevent them by touching rows (and tables) in a consistent order, and retry.

## Advisory locks

Advisory locks are locks on **numbers that you choose**, with no table behind them. They
coordinate application work: "only one instance may run the nightly report".

| Function | Waits? | Released |
| --- | --- | --- |
| `pg_advisory_lock(key)` | yes | by `pg_advisory_unlock(key)` or when the **session** ends |
| `pg_try_advisory_lock(key)` | no: returns `false` | same |
| `pg_advisory_xact_lock(key)` | yes | at the end of the **transaction** |
| `pg_try_advisory_xact_lock(key)` | no | at the end of the transaction |

The key is one `bigint` or two `integer`s; `hashtext('nightly-report')` turns a name into one.

```sql
BEGIN;
SELECT pg_try_advisory_xact_lock(4242) AS got_lock;   -- false: another instance is running
-- ... do the work only if got_lock ...
COMMIT;                                                -- released here
```

Prefer the **transaction-level** forms. Session-level locks survive a `COMMIT` and are tied to
the connection: with a transaction-mode pooler (PgBouncer), the next transaction may run on a
different server connection, so a session lock may be held by a connection you no longer have.

## Locks under SERIALIZABLE

SERIALIZABLE (SSI) adds **predicate locks** (`SIReadLock` in `pg_locks`) that never block
anyone; they only record what each transaction read, so PostgreSQL can detect a dangerous
pattern and abort one transaction with `40001`. Their cost is memory and retries, not waits.

## Summary

- Each statement takes a table lock until the end of its transaction; only `ACCESS EXCLUSIVE`
  (most `ALTER TABLE`, `DROP`, `TRUNCATE`, `VACUUM FULL`) blocks reads.
- A queued `ACCESS EXCLUSIVE` request blocks every later query on the table: give DDL a short
  `lock_timeout` and retry.
- Add constraints `NOT VALID` and validate them separately; build indexes `CONCURRENTLY`.
- Row locks live in the tuple; foreign key checks take `FOR KEY SHARE`.
- `pg_blocking_pids` and `pg_stat_activity` show who blocks whom; idle-in-transaction sessions
  are the usual blockers; `pg_terminate_backend` ends them.
- Use transaction-level advisory locks for application-level mutual exclusion.

## Further reading

- PostgreSQL 17, "Explicit Locking": <https://www.postgresql.org/docs/17/explicit-locking.html>
- PostgreSQL 17, "pg_locks": <https://www.postgresql.org/docs/17/view-pg-locks.html>
- PostgreSQL 17, "System Information Functions" (pg_blocking_pids): <https://www.postgresql.org/docs/17/functions-info.html>
- PostgreSQL 17, "Advisory Lock Functions": <https://www.postgresql.org/docs/17/functions-admin.html#FUNCTIONS-ADVISORY-LOCKS>
- PostgreSQL 17, "ALTER TABLE" (lock levels and NOT VALID): <https://www.postgresql.org/docs/17/sql-altertable.html>
- PostgreSQL 17, "Serializable Isolation Level": <https://www.postgresql.org/docs/17/transaction-iso.html#XACT-SERIALIZABLE>
- PostgreSQL wiki, "Lock Monitoring": <https://wiki.postgresql.org/wiki/Lock_Monitoring>
