# EXPLAIN (ANALYZE, BUFFERS): Reading the Query Planner

The SQL track's lesson [Indexes and Query Plans](../../../sql/p3-advanced/01-indexes-plans/lesson.md)
showed what an index is and how to spot a sequential scan in `EXPLAIN`. This lesson goes into the
planner itself: how it estimates, how it chooses scans and joins, where it gets its numbers, and
how to read an `EXPLAIN ANALYZE` well enough to know **why** a plan is bad, not only that it is.

## The planner in one paragraph

For each query, the planner enumerates ways to run it (which index, which join order, which join
method), estimates the **cost** of each from statistics about the tables, and keeps the cheapest.
A cost is an abstract number: reading a page sequentially costs `seq_page_cost` (1.0), a random
page `random_page_cost` (4.0, lower it to about 1.1 on SSDs), processing a row `cpu_tuple_cost`
(0.01). The planner is only as good as its **row estimates**: almost every bad plan is a plan
built on a wrong guess of how many rows a step returns.

## The options of EXPLAIN

| Option | Adds |
| --- | --- |
| (none) | the chosen plan with estimated costs and rows; the query is **not** run |
| `ANALYZE` | **runs** the query and adds actual time, rows and loops for every node |
| `BUFFERS` | pages found in shared buffers (`hit`), read from disk or the OS cache (`read`), dirtied and written |
| `VERBOSE` | output columns, schema-qualified names, worker details |
| `SETTINGS` | planner settings that differ from the defaults |
| `WAL` | WAL records and bytes generated (for writes) |
| `SERIALIZE` (17+) | the cost of converting the result to text for the client |
| `MEMORY` (17+) | memory used by the planner |
| `FORMAT JSON` | the same tree as JSON, for tools such as explain.dalibo.com |

`EXPLAIN ANALYZE` **executes** the statement. For an `UPDATE` or `DELETE`, wrap it:
`BEGIN; EXPLAIN ANALYZE DELETE ...; ROLLBACK;`.

## Reading a plan

```text
EXPLAIN (ANALYZE, BUFFERS)
SELECT c.name, sum(o.total)
FROM customers AS c JOIN orders AS o ON o.customer_id = c.id
WHERE c.country = 'PT'
GROUP BY c.name;

HashAggregate  (cost=2817.62..2830.12 rows=1000 width=40) (actual time=31.2..31.5 rows=987 loops=1)
  Group Key: c.name
  Buffers: shared hit=1205
  ->  Hash Join  (cost=289.00..2717.62 rows=20000 width=14) (actual time=2.9..25.1 rows=19650 loops=1)
        Hash Cond: (o.customer_id = c.id)
        ->  Seq Scan on orders o  (cost=0.00..1935.00 rows=100000 width=14) (actual time=0.01..8.1 rows=100000 loops=1)
        ->  Hash  (cost=264.00..264.00 rows=2000 width=18) (actual time=2.8..2.8 rows=1965 loops=1)
              Buckets: 2048  Batches: 1  Memory Usage: 115kB
              ->  Seq Scan on customers c  (cost=0.00..264.00 rows=2000 width=18) (actual time=0.01..2.3 rows=1965 loops=1)
                    Filter: (country = 'PT'::text)
                    Rows Removed by Filter: 8035
Planning Time: 0.4 ms
Execution Time: 31.9 ms
```

- Read it **inside out**: the most indented nodes run first and feed their parents.
- `cost=startup..total`: the estimated cost to return the first row and all rows.
- `rows` (estimated) next to `actual ... rows`: **compare them first**. Here 2000 versus 1965
  is excellent. A factor of 10 or more is where bad plans come from.
- `loops`: how many times the node ran. `actual rows` and `actual time` are **per loop**, so a
  node with `rows=5 loops=20000` produced 100,000 rows in total.
- `Rows Removed by Filter`: rows read and thrown away; a large number next to a small output is
  a missing or unusable index.
- `Buffers: shared hit=1205 read=0`: everything came from the cache. `read` means I/O.

## Scan nodes

| Node | Reads | Good when |
| --- | --- | --- |
| **Seq Scan** | every page, in order | a large share of the table is needed, or the table is small |
| **Index Scan** | the index, then the table row for each match | few rows, or rows needed in index order |
| **Index Only Scan** | only the index, plus the table for pages not all-visible (`Heap Fetches`) | every needed column is in the index and the table is well vacuumed |
| **Bitmap Index Scan** + **Bitmap Heap Scan** | collects matching row addresses from one or more indexes, sorts them by page, then reads each page once | a medium number of rows, or combining indexes with `BitmapAnd`/`BitmapOr` |

A Bitmap Heap Scan may show `Recheck Cond` and `Heap Blocks: exact=... lossy=...`: when the
bitmap does not fit in `work_mem`, it remembers whole pages and must recheck every row on them.

## Join nodes

| Node | How | Good when |
| --- | --- | --- |
| **Nested Loop** | for each outer row, look up the inner side (ideally with an index) | the outer side is small |
| **Hash Join** | build a hash table of the smaller input, then probe it with the other | large unsorted inputs and equality conditions |
| **Merge Join** | walk two inputs sorted on the join key | both inputs already sorted (from indexes) or large |

The classic disaster is a **Nested Loop chosen on an estimate of 1 row** that actually returns
50,000: the inner side then runs 50,000 times (`loops=50000`). The fix is almost never to turn
nested loops off; it is to fix the estimate.

Other nodes you will meet: `Sort` (look for `Sort Method: external merge Disk: ...` = it spilled,
`work_mem` too small), `Incremental Sort` (input already partly sorted), `HashAggregate` and
`GroupAggregate`, `Memoize` (caches inner lookups of a nested loop), `Limit`, and `Gather` with
`Workers Launched` for parallel plans.

## Where the estimates come from

`ANALYZE` (run by autovacuum, or by hand) samples up to `300 × default_statistics_target` rows
(30,000 by default) per table and stores per-column statistics in `pg_statistic`, readable
through the `pg_stats` view:

```sql
SELECT attname, null_frac, n_distinct, most_common_vals, most_common_freqs,
       histogram_bounds, correlation
FROM pg_stats WHERE tablename = 'orders' AND attname = 'status';
```

| Column | Means |
| --- | --- |
| `null_frac` | fraction of NULLs |
| `n_distinct` | number of distinct values (negative: a fraction of the rows, -1 = unique) |
| `most_common_vals`, `most_common_freqs` | the MCV list: the most frequent values and their frequencies |
| `histogram_bounds` | buckets with equal numbers of rows for the other values, for ranges |
| `correlation` | how well the physical order follows the column's order (1 = sorted); drives index scan costs |

`pg_class.reltuples` and `relpages` hold the table's size estimate (also from ANALYZE and
VACUUM); `SELECT reltuples::bigint FROM pg_class WHERE oid = 'orders'::regclass` is the
classic fast approximate count.

For `WHERE status = 'refunded'`, the planner looks the value up in the MCV list; for values
outside it, it spreads the remaining frequency over the remaining distinct values; for ranges it
uses the histogram.

### Why estimates go wrong

| Cause | Symptom | Fix |
| --- | --- | --- |
| stale statistics after a big load or delete | estimates far from reality on every query of the table | `ANALYZE t`; make autovacuum analyze it more often |
| **correlated columns**: `city = 'Porto' AND country = 'PT'` | the planner multiplies the selectivities as if independent, and underestimates | `CREATE STATISTICS ... (dependencies, mcv) ON city, country FROM addresses; ANALYZE addresses;` |
| a skewed column with many distinct values | rare and frequent values estimated alike | `ALTER TABLE t ALTER COLUMN c SET STATISTICS 1000; ANALYZE t;` |
| a function or expression on the column, `WHERE lower(email) = ...` | a fixed default guess (0.5% for equality) | an expression index (it gets its own statistics) or extended statistics on the expression |
| `GROUP BY a, b` on related columns | wrong number of groups, wrong aggregate choice | `CREATE STATISTICS (ndistinct)` |

```sql
CREATE STATISTICS addresses_city_country (dependencies, mcv) ON city, country FROM addresses;
ANALYZE addresses;
SELECT stxname, stxkind FROM pg_statistic_ext;
```

Extended statistics do nothing until the next `ANALYZE`.

## Prepared statements: custom and generic plans

A prepared statement (most drivers use them) is planned with the actual parameter values for its
first five executions (**custom plans**). After that, PostgreSQL may switch to a **generic plan**
that ignores the values, if it is not much more expensive on average. With skewed data that can
be wrong for some values. `plan_cache_mode = force_custom_plan` (per role or session) forces
custom plans; `EXPLAIN (GENERIC_PLAN)` (16+) shows the generic one.

## Diagnosing, not forcing

The `enable_*` settings (`enable_seqscan`, `enable_nestloop`, ...) make a plan type look very
expensive. Use them **in a session to experiment** ("would an index scan be faster?"), never as
a fix in production. PostgreSQL has no query hints by design; the fix is better statistics,
better indexes or a better query. `pg_hint_plan` exists as an extension for emergencies.

## Summary

- Compare estimated and actual rows first; bad plans come from bad estimates.
- Actual rows and time are per loop; multiply by `loops`.
- `BUFFERS` separates cache hits from reads; `Rows Removed by Filter` and spills point at fixes.
- Know the scans (seq, index, index-only, bitmap) and joins (nested loop, hash, merge) and when
  each wins.
- Statistics live in `pg_stats`; fix stale ones with `ANALYZE`, correlated columns with
  `CREATE STATISTICS`, skew with a higher statistics target.
- Use `enable_*` to experiment, not to fix.

## Further reading

- PostgreSQL 17, "Using EXPLAIN": <https://www.postgresql.org/docs/17/using-explain.html>
- PostgreSQL 17, "EXPLAIN": <https://www.postgresql.org/docs/17/sql-explain.html>
- PostgreSQL 17, "Statistics Used by the Planner" (with extended statistics): <https://www.postgresql.org/docs/17/planner-stats.html>
- PostgreSQL 17, "How the Planner Uses Statistics": <https://www.postgresql.org/docs/17/planner-stats-details.html>
- PostgreSQL 17, "Query Planning" settings: <https://www.postgresql.org/docs/17/runtime-config-query.html>
- PostgreSQL 17, "pg_stats": <https://www.postgresql.org/docs/17/view-pg-stats.html>
- explain.dalibo.com, a visualiser for plans: <https://explain.dalibo.com/>
