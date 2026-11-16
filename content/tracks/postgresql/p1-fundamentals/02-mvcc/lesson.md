# MVCC Inside: Row Versions, Snapshots and HOT Updates

The SQL track's lesson
[Transactions, Isolation and Locking](../../../sql/p3-advanced/03-transactions/lesson.md)
explains what isolation levels promise and that PostgreSQL keeps "old versions" of rows. This
lesson opens the box: what a row version is, how a transaction decides which versions it sees,
why an `UPDATE` is really an insert, and the trick (HOT) that makes updates cheap. Everything
in the next lesson, VACUUM, follows from this.

## A table is a heap of tuples

A table is a file of 8 kB **pages**. Each page holds **tuples**, one per row *version*, plus an
array of line pointers that point to them. A tuple's physical address is its **ctid**:
`(page, line pointer)`.

Every tuple carries a header with, among others:

| Field | Meaning |
| --- | --- |
| `xmin` | the id of the transaction that **created** this version (insert or update) |
| `xmax` | the id of the transaction that **deleted** or replaced it (0 if none; also set by row locks) |
| `ctid` | for an updated tuple, where its newer version is |

You can see them as hidden system columns:

```sql
CREATE TABLE accounts (id int PRIMARY KEY, balance numeric);
INSERT INTO accounts VALUES (1, 100);
SELECT ctid, xmin, xmax, * FROM accounts;
--  ctid  | xmin | xmax | id | balance
--  (0,1) |  750 |    0 |  1 |     100

UPDATE accounts SET balance = 80 WHERE id = 1;
SELECT ctid, xmin, xmax, * FROM accounts;
--  (0,2) |  751 |    0 |  1 |      80        ← a new tuple at a new address
```

The `UPDATE` did not change the tuple at `(0,1)`. It set that tuple's `xmax` to 751 and wrote a
**new** tuple at `(0,2)` with `xmin` 751. A `DELETE` only sets `xmax`. Nothing is removed: the
old version stays in the page as long as some transaction might still need it.

## Transaction ids and snapshots

Each transaction that writes gets a 32-bit **transaction id** (`pg_current_xact_id()`; read-only
transactions do not use one). Whether a transaction committed or aborted is recorded in the
commit log (`pg_xact`).

When a statement (READ COMMITTED) or a transaction (REPEATABLE READ and SERIALIZABLE) starts, it
takes a **snapshot**:

```sql
SELECT pg_current_snapshot();      -- 751:755:752,754
--  xmin 751: every transaction below this one has finished
--  xmax 755: every transaction from this one on had not started yet
--  in progress at the time: 752 and 754
```

A tuple is **visible** to a snapshot when:

1. its `xmin` committed and is "in the past" of the snapshot (below `xmax` and not in the
   in-progress list), and
2. its `xmax` is 0, aborted, or not yet in the past of the snapshot.

That is the whole trick. A reader never waits for a writer: it simply sees the versions that
were committed when its snapshot was taken. Two transactions that update the **same row** do
conflict: the second one waits on the first one's row lock (`xmax`) and then, under READ
COMMITTED, re-reads the newest version; under REPEATABLE READ it fails with a serialization
error.

Hint bits: the first reader that looks up a tuple's `xmin` in the commit log stores the answer
("committed") in the tuple header, so later readers skip the lookup. That is why a `SELECT`
right after a big load can write to the disk.

## The cost: dead tuples

A tuple whose `xmax` committed and that no snapshot can still see is **dead**. It still takes
space in the page and in every index that points to it. Something must reclaim it: that is
`VACUUM` (next lesson). Three habits make dead tuples pile up:

- **Updating many rows** (every update leaves one dead version).
- **Long-running transactions**: a transaction open for an hour holds a snapshot that might
  need versions from an hour ago, so nothing deleted since then can be removed, anywhere in the
  database.
- **Idle in transaction** sessions: an application that ran `BEGIN` and is waiting for a user or
  an HTTP call is a long-running transaction too.

```sql
SELECT pid, state, now() - xact_start AS age, backend_xmin
FROM pg_stat_activity
WHERE backend_xmin IS NOT NULL
ORDER BY age(backend_xmin) DESC;
```

## Indexes and updates

An index entry points to a tuple's ctid. When an update writes a new tuple at a new address,
**every index of the table** needs a new entry for it, even indexes on columns that did not
change. On a table with eight indexes, updating a `last_seen_at` column writes nine tuples.

## HOT updates

A **heap-only tuple** (HOT) update avoids that. When:

1. **no indexed column changes** (columns only in BRIN indexes do not count from PostgreSQL 16),
   and
2. the new version **fits on the same page**,

PostgreSQL writes the new tuple on the same page and links it from the old one. The indexes keep
pointing to the old line pointer and follow the chain; no index is touched. Later, any access to
the page can **prune** the chain and reuse the space, without waiting for VACUUM.

To make an update-heavy table HOT-friendly:

- **Do not index columns you update constantly** (`last_seen_at`, counters, `status` columns are
  common culprits). Every such index turns HOT off for those updates.
- **Leave room on each page** with a lower **fillfactor**. The default is 100 (pages are filled
  completely on insert, leaving no room for new versions); 70–90 is common for hot tables:

```sql
CREATE TABLE sessions (
    id        bigint PRIMARY KEY,
    user_id   bigint NOT NULL,
    last_seen timestamptz NOT NULL
) WITH (fillfactor = 80);

ALTER TABLE big_table SET (fillfactor = 80);   -- applies to pages written from now on
```

Check how you are doing in `pg_stat_user_tables`:

```sql
SELECT relname, n_tup_upd, n_tup_hot_upd,
       round(100.0 * n_tup_hot_upd / nullif(n_tup_upd, 0), 1) AS hot_percent
FROM pg_stat_user_tables
ORDER BY n_tup_upd DESC;
```

The same counters for the **current transaction only** are in `pg_stat_xact_user_tables`, which
is handy in tests.

## Index-only scans and the visibility map

An index does not store `xmin`/`xmax`, so a scan that finds a row in an index must normally visit
the table to check visibility. The **visibility map** has one bit per page saying "every tuple
here is visible to everyone". VACUUM sets the bit; any change on the page clears it. An
**index-only scan** skips the table for pages with the bit set; that is why a recently vacuumed
table gets faster index-only scans (`Heap Fetches: 0` in `EXPLAIN ANALYZE`).

## Summary

- Every row version is a tuple with `xmin` (creator) and `xmax` (deleter); `ctid` is its address.
- An `UPDATE` writes a new tuple and marks the old one; a `DELETE` only marks.
- A snapshot decides visibility from transaction ids, so readers never block writers.
- Dead tuples stay until VACUUM; long and idle-in-transaction sessions stop their cleanup.
- A normal update writes into every index; a HOT update (no indexed column changed, room on the
  page) writes none. Keep hot columns out of indexes and lower the fillfactor of update-heavy
  tables.
- The visibility map lets index-only scans skip the table.

## Further reading

- PostgreSQL 17, "Concurrency Control" and "Introduction to MVCC": <https://www.postgresql.org/docs/17/mvcc-intro.html>
- PostgreSQL 17, "System Columns": <https://www.postgresql.org/docs/17/ddl-system-columns.html>
- PostgreSQL 17, "Database Page Layout": <https://www.postgresql.org/docs/17/storage-page-layout.html>
- PostgreSQL 17, "Heap-Only Tuples (HOT)": <https://www.postgresql.org/docs/17/storage-hot.html>
- PostgreSQL 17, "Visibility Map": <https://www.postgresql.org/docs/17/storage-vm.html>
- PostgreSQL 17, transaction id and snapshot functions: <https://www.postgresql.org/docs/17/functions-info.html#FUNCTIONS-PG-SNAPSHOT>
- Egor Rogov, "PostgreSQL 14 Internals", part I (Isolation and MVCC): <https://postgrespro.com/community/books/internals>
