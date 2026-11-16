# Declarative Partitioning

A **partitioned table** is one logical table stored as many physical tables, its
**partitions**, each holding the rows of one range, list or hash bucket of a **partition key**.
Done for the right reasons, partitioning makes deleting old data instant and keeps maintenance
and some queries small. Done for the wrong reasons, it makes every query plan bigger and buys
nothing. This lesson covers how it works, the rules it imposes, and when to use it.

## Range, list and hash

```sql
CREATE TABLE events (
    id      bigint GENERATED ALWAYS AS IDENTITY,
    at      timestamptz NOT NULL,
    tenant  integer NOT NULL,
    payload jsonb,
    PRIMARY KEY (id, at)                  -- must include the partition key
) PARTITION BY RANGE (at);

CREATE TABLE events_2026_11 PARTITION OF events
    FOR VALUES FROM ('2026-11-01') TO ('2026-12-01');      -- lower bound included, upper excluded
CREATE TABLE events_2026_12 PARTITION OF events
    FOR VALUES FROM ('2026-12-01') TO ('2027-01-01');
CREATE TABLE events_default PARTITION OF events DEFAULT;   -- rows that fit no other partition
```

| Strategy | Partitions by | Example |
| --- | --- | --- |
| `RANGE` | consecutive ranges | months of `at`, id ranges |
| `LIST` | explicit values | `FOR VALUES IN ('eu', 'uk')` for regions |
| `HASH` | `MODULUS` and `REMAINDER` of a hash | `FOR VALUES WITH (MODULUS 8, REMAINDER 3)`: spreads rows evenly |

An insert into `events` is routed to the right partition; a row that fits none fails (SQLSTATE
`23514`) unless there is a `DEFAULT` partition. Updating the key moves the row to another
partition. Partitions can themselves be partitioned (sub-partitioning), and `\d+ events` lists
them.

## Partition pruning

The main performance gain: when a query's `WHERE` restricts the key, the planner (or the
executor, for parameters and `now()`) skips the partitions that cannot match:

```sql
EXPLAIN SELECT count(*) FROM events WHERE at >= '2026-11-10' AND at < '2026-11-11';
--  Aggregate
--    ->  Seq Scan on events_2026_11 events      ← only one partition
--          Filter: ...
```

A query without the key reads **every** partition, so choose the key that almost every query
filters on (usually time). `enable_partition_pruning` is on by default; a plan that shows
`Subplans Removed: 11` pruned at execution time.

## The rules partitioning imposes

- **Primary keys and unique constraints must include the partition key**: uniqueness is enforced
  per partition, so `UNIQUE (id)` alone is impossible. That is why the example has
  `PRIMARY KEY (id, at)`.
- Indexes created on the parent are created on every partition (and on partitions added later);
  `CREATE INDEX` on the parent cannot be `CONCURRENTLY`, but you can create the index
  concurrently on each partition and then `ALTER INDEX parent_idx ATTACH PARTITION ...`.
- Foreign keys **from** a partitioned table work; foreign keys **to** one must reference a unique
  key that includes the partition key.
- Row-level triggers and RLS policies on the parent apply to all partitions.
- Rows in the `DEFAULT` partition block creating a new partition whose range they belong to
  until you move them: keep the default empty, and alert when it is not.

## Retention: why people really partition

Deleting a month of a 2-billion-row table with `DELETE` writes WAL for every row, leaves
millions of dead tuples for VACUUM and takes hours. With monthly partitions it is a catalog
operation:

```sql
ALTER TABLE events DETACH PARTITION events_2025_11;            -- now an ordinary table
ALTER TABLE events DETACH PARTITION events_2025_11 CONCURRENTLY; -- 14+: without blocking queries
DROP TABLE events_2025_11;                                       -- or archive it first
```

The same works in reverse for bulk loads: load and index a standalone table, add a `CHECK`
constraint that matches the range (so attaching skips the validation scan), then
`ALTER TABLE events ATTACH PARTITION events_2027_01 FOR VALUES FROM (...) TO (...)`.

Something must **create the future partitions** before data arrives: a scheduled job, the
`pg_partman` extension, or the application's migrations. A missing partition with no default
means failed inserts at midnight on the first of the month.

## When to partition

| Good reasons | Bad reasons |
| --- | --- |
| retention by time: drop old data instantly | "the table is big" (an index scan on 1 billion rows is fine) |
| almost every query filters on the key, and the hot partition fits in memory | queries that do not filter on the key |
| VACUUM, `REINDEX` and backups per partition stay manageable | thousands of tiny partitions (planning time grows with their number) |
| bulk loading a period at once | to replace a missing index |

Rule of thumb: consider it beyond a few hundred GB or when you need time-based retention, keep
partitions in the tens or low hundreds, and test planning time with `EXPLAIN (SUMMARY)`.

## Summary

- `PARTITION BY RANGE`, `LIST` or `HASH`; partitions are created with `PARTITION OF ... FOR VALUES`;
  a `DEFAULT` partition catches the rest.
- Pruning skips partitions only when the query filters on the key.
- Unique constraints must include the key; indexes on the parent cascade to the partitions.
- `DETACH PARTITION` (`CONCURRENTLY`) plus `DROP` is instant retention; `ATTACH` with a matching
  `CHECK` is a fast bulk load.
- Automate the creation of future partitions; partition for retention and maintenance, not
  because a table is merely large.

## Further reading

- PostgreSQL 17, "Table Partitioning": <https://www.postgresql.org/docs/17/ddl-partitioning.html>
- PostgreSQL 17, "Partition Pruning": <https://www.postgresql.org/docs/17/ddl-partitioning.html#DDL-PARTITION-PRUNING>
- PostgreSQL 17, "CREATE TABLE" (PARTITION BY, PARTITION OF): <https://www.postgresql.org/docs/17/sql-createtable.html>
- PostgreSQL 17, "ALTER TABLE" (ATTACH and DETACH PARTITION): <https://www.postgresql.org/docs/17/sql-altertable.html>
- pg_partman, partition management extension: <https://github.com/pgpartman/pg_partman>
