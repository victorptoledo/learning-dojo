# Transactions, Isolation and Locking

A database serves many sessions at once. Transactions are how it keeps each one's work all or
nothing, and isolation is how much they see of each other while they run. Most production data
bugs that "cannot happen" are races that this lesson explains.

## ACID

| Letter | Promise | In practice |
| --- | --- | --- |
| **Atomicity** | all of a transaction's changes happen, or none | a crash mid-transfer leaves both accounts untouched |
| **Consistency** | constraints hold at commit | a transfer cannot leave a negative balance if a CHECK forbids it |
| **Isolation** | concurrent transactions do not see each other's half-done work | how much exactly is the isolation level |
| **Durability** | after COMMIT returns, the change survives a crash | the write-ahead log is flushed to disk |

```sql
BEGIN;                                  -- or START TRANSACTION (standard)
UPDATE accounts SET balance = balance - 50 WHERE id = 1;
UPDATE accounts SET balance = balance + 50 WHERE id = 2;
COMMIT;                                 -- or ROLLBACK to undo both
```

Without `BEGIN`, each statement is its own transaction (autocommit). Inside a transaction,
`SAVEPOINT s1` marks a point and `ROLLBACK TO SAVEPOINT s1` undoes only what came after it. In
PostgreSQL an error aborts the whole transaction ("current transaction is aborted, commands
ignored until end of transaction block") unless you roll back to a savepoint.

Keep transactions **short**: they hold locks and, in PostgreSQL, keep old row versions alive
for everyone. Never wait for a user or a slow HTTP call inside one.

## The anomalies

| Anomaly | What happens |
| --- | --- |
| **dirty read** | you read another transaction's uncommitted change, which it then rolls back |
| **non-repeatable read** | you read a row twice and get two values, because someone committed in between |
| **phantom read** | you run the same query twice and new rows appear |
| **lost update** | two transactions read a value, both compute from it and write; one write is lost |
| **write skew** | two transactions each check a rule over rows the other changes; both commit and the rule breaks (two doctors both go off call because "the other one is on call") |

## Isolation levels

The standard defines four levels by the anomalies they must prevent. Engines are allowed to do
better, and they differ:

| Level | Standard forbids | PostgreSQL | MySQL InnoDB | SQL Server |
| --- | --- | --- | --- | --- |
| READ UNCOMMITTED | nothing | behaves as READ COMMITTED | dirty reads possible | dirty reads possible |
| READ COMMITTED | dirty reads | **default**; each statement sees a fresh snapshot | | **default** (locking, or snapshots with RCSI) |
| REPEATABLE READ | + non-repeatable reads | one snapshot per transaction; no phantoms either; concurrent updates of the same row fail (40001) | **default**; snapshot reads, but locking reads and updates see newer rows | locks rows read |
| SERIALIZABLE | + phantoms (and every other anomaly) | SSI: detects dangerous patterns and aborts one transaction (40001) | locking reads | range locks |

```sql
BEGIN ISOLATION LEVEL SERIALIZABLE;
-- ...
COMMIT;
```

Two lessons from the table:

- **READ COMMITTED does not prevent lost updates or write skew.** It is the default almost
  everywhere, so your code must handle them.
- **Higher levels fail instead of waiting.** Under REPEATABLE READ and SERIALIZABLE,
  PostgreSQL aborts one of the conflicting transactions with SQLSTATE `40001`
  (serialization_failure). The application must **retry the whole transaction**, not just the
  last statement.

## MVCC: readers do not block writers

PostgreSQL (like Oracle and MySQL InnoDB) uses **multi-version concurrency control**: an
`UPDATE` writes a new version of the row and keeps the old one for transactions that started
earlier. Each transaction reads from a snapshot of committed versions, so plain `SELECT`s never
wait for writers and writers never wait for readers. Writers of the **same row** do wait for
each other. The old versions are cleaned up later by `VACUUM` (a topic of the PostgreSQL track).

## Fixing the lost update

The race: two requests read a balance of 100, both decide 80 can be withdrawn, both write 20.

**1. One atomic statement** (the best fix when the rule fits in one row):

```sql
UPDATE accounts SET balance = balance - 80
WHERE id = 7 AND balance >= 80;
-- UPDATE 1: done.  UPDATE 0: insufficient funds (or no such account).
```

The second request waits for the first one's row lock, then re-checks `balance >= 80` against
the new value (20) and updates nothing.

**2. Lock the row first** (pessimistic), when you must read before deciding:

```sql
BEGIN;
SELECT balance FROM accounts WHERE id = 7 FOR UPDATE;   -- others wait here
-- decide in the application
UPDATE accounts SET balance = 20 WHERE id = 7;
COMMIT;
```

`FOR UPDATE` locks the selected rows until the transaction ends. Variants: `FOR SHARE`
(others may read-lock but not change), `NOWAIT` (fail at once if locked), `SKIP LOCKED` (skip
rows that are locked: the basis of job queues, next kata).

**3. Optimistic concurrency** with a version column, when conflicts are rare and you do not
want to hold locks across a user's think time:

```sql
UPDATE documents SET body = :body, version = version + 1
WHERE id = :id AND version = :version_read;
-- 0 rows: someone saved first; reload and show the conflict
```

**4. SERIALIZABLE**, for rules that span rows (write skew), with a retry loop around the
transaction.

## Deadlocks

Transaction A locks row 1 and waits for row 2; B locks row 2 and waits for row 1. Neither can
continue. The database detects the cycle and aborts one of them (PostgreSQL SQLSTATE `40P01`).
Prevent most deadlocks by locking rows in a **consistent order** (for example by id), and
retry when one happens anyway.

## Summary

- A transaction is all or nothing; keep it short and never wait on a user inside it.
- READ COMMITTED (the usual default) allows non-repeatable reads, lost updates and write skew.
- REPEATABLE READ and SERIALIZABLE abort conflicts with 40001: retry the whole transaction.
- MVCC: readers and writers do not block each other; writers of one row do.
- Fix lost updates with atomic updates, FOR UPDATE, version columns or SERIALIZABLE.
- Lock in a consistent order to avoid deadlocks; retry on 40P01.

## Further reading

- PostgreSQL 17, "Transaction Isolation": <https://www.postgresql.org/docs/17/transaction-iso.html>
- PostgreSQL 17, "Explicit Locking": <https://www.postgresql.org/docs/17/explicit-locking.html>
- PostgreSQL 17, "Introduction to MVCC": <https://www.postgresql.org/docs/17/mvcc-intro.html>
- PostgreSQL 17, the locking clause (FOR UPDATE, SKIP LOCKED): <https://www.postgresql.org/docs/17/sql-select.html#SQL-FOR-UPDATE-SHARE>
- SQL Server, "SET TRANSACTION ISOLATION LEVEL": <https://learn.microsoft.com/en-us/sql/t-sql/statements/set-transaction-isolation-level-transact-sql>
- Berenson et al., "A Critique of ANSI SQL Isolation Levels": <https://www.microsoft.com/en-us/research/publication/a-critique-of-ansi-sql-isolation-levels/>
