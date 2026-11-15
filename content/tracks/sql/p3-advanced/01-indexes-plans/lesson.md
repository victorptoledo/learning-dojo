# Indexes and Query Plans

SQL is declarative, so speed is the planner's job, and the planner can only be as good as the
access paths you give it. Indexes are those paths. This lesson is about the B-tree, the index
behind nearly every primary key, and about reading the plan the planner chose.

## What a B-tree index is

A B-tree index keeps the indexed values **sorted**, in a balanced tree of pages, each value
with a pointer to its row in the table (the heap). To find `customer_id = 42`, the engine
walks from the root to a leaf in 3 or 4 page reads, then follows the leaf chain while the
value still matches. That is an **index scan**. Without an index it reads every page of the
table: a **sequential scan**.

Because the values are sorted, one index serves:

- equality: `customer_id = 42`, `IN (1, 2, 3)`;
- ranges: `placed_at >= '2026-03-01' AND placed_at < '2026-04-01'`, `BETWEEN`, `LIKE 'abc%'`
  (prefix only, and in PostgreSQL with the `C` collation or a `text_pattern_ops` index);
- sorting: `ORDER BY placed_at DESC LIMIT 20` can read the index backwards and stop after 20;
- `min` and `max`, and joins on the column.

An index is a **copy** of the indexed columns, kept up to date by every `INSERT`, `UPDATE`
of those columns and `DELETE`. Each index costs disk, memory and write time. Index what your
queries filter, join and sort on, not every column.

## Sargable conditions

A condition is **sargable** (from "search argument") when the index can be searched with it:
the column stands alone on one side.

| Not sargable | Sargable rewrite |
| --- | --- |
| `WHERE date(placed_at) = '2026-03-01'` | `WHERE placed_at >= '2026-03-01' AND placed_at < '2026-03-02'` |
| `WHERE extract(year FROM placed_at) = 2026` | `WHERE placed_at >= '2026-01-01' AND placed_at < '2027-01-01'` |
| `WHERE price * 1.23 > 100` | `WHERE price > 100 / 1.23` |
| `WHERE lower(email) = 'ada@x.com'` | an **expression index** on `lower(email)` |
| `WHERE name LIKE '%son'` | a trigram index (`pg_trgm`), or full-text search |
| `WHERE customer_id::text = '42'` | compare with the column's own type |

Half-open ranges (`>=` start `AND <` next start) are correct for timestamps of any precision,
while `BETWEEN ... AND '2026-03-31 23:59:59'` misses the last second's fractions.

## Composite indexes: order matters

An index on `(customer_id, placed_at)` is sorted by `customer_id`, and by `placed_at` within
each customer. Like a phone book sorted by surname and then first name:

| Query | Uses the index? |
| --- | --- |
| `WHERE customer_id = 42` | yes (leftmost column) |
| `WHERE customer_id = 42 AND placed_at >= '2026-01-01'` | yes, both columns |
| `WHERE customer_id = 42 ORDER BY placed_at DESC LIMIT 10` | yes, and no sort needed |
| `WHERE placed_at >= '2026-01-01'` | not efficiently (the leading column is missing) |

Rule of thumb: **equality columns first, then the range or sort column**. A range on the
first column stops the second from narrowing the search. One well-designed composite index
often replaces several single-column ones.

## Covering, partial and expression indexes

```sql
-- covering: the index alone answers the query (an index-only scan)
CREATE INDEX orders_customer_placed ON orders (customer_id, placed_at DESC) INCLUDE (total);

-- partial: only the rows a hot query looks at; small and cheap to maintain
CREATE INDEX orders_pending ON orders (placed_at) WHERE status = 'pending';

-- expression: for queries that filter on a function of a column
CREATE INDEX customers_email_lower ON customers (lower(email));

-- unique: a constraint and an index at once
CREATE UNIQUE INDEX users_email ON users (lower(email));
```

A partial index is used only when the query's `WHERE` implies the index's condition
(`status = 'pending'` written the same way). Index-only scans in PostgreSQL also need the
table's visibility map to be current, which `VACUUM` maintains.

Other index types in PostgreSQL: **hash** (equality only), **GIN** (arrays, `jsonb`, full-text),
**GiST** (ranges, geometry, exclusion constraints), **BRIN** (huge tables whose rows arrive in
order, such as logs). Their tuning belongs to the PostgreSQL track.

## Selectivity: when an index is not worth it

An index scan reads the index **and then** jumps to the table for each match, in random
order. For a condition that matches a large share of the table (`status = 'paid'` when 90% of
orders are paid), a sequential scan is cheaper, and the planner will choose it. That is not a
bug. PostgreSQL also has the **bitmap scan**: collect matching row locations from the index,
sort them by page, then read each page once, which suits medium selectivity and combining two
indexes with AND or OR.

The planner decides from **statistics** (row counts, common values, histograms) gathered by
`ANALYZE`, which autovacuum runs for you. After a bulk load, run `ANALYZE` yourself.

## Reading a plan

`EXPLAIN` shows the plan and its estimates; `EXPLAIN ANALYZE` **runs** the query and adds what
really happened (careful with `UPDATE` and `DELETE`: wrap them in a transaction you roll back).

```text
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, total FROM orders WHERE customer_id = 42 ORDER BY placed_at DESC LIMIT 20;

Limit  (cost=0.43..18.90 rows=20 width=14) (actual time=0.031..0.052 rows=20 loops=1)
  Buffers: shared hit=24
  ->  Index Scan Backward using orders_customer_placed on orders
        (cost=0.43..921.40 rows=998 width=14) (actual time=0.030..0.048 rows=20 loops=1)
        Index Cond: (customer_id = 42)
        Buffers: shared hit=24
Planning Time: 0.120 ms
Execution Time: 0.071 ms
```

How to read it:

- It is a **tree**: the innermost (most indented) nodes run first and feed their parents.
- `cost=startup..total` is in arbitrary planner units; compare plans with it, do not read it
  as milliseconds.
- `rows=` estimated versus `actual ... rows=`: a large gap (estimated 10, actual 100000) means
  bad statistics or a condition the planner cannot estimate, and it is the usual cause of bad
  plans.
- `loops=` multiplies: an inner node with `loops=10000` ran ten thousand times.
- `Rows Removed by Filter` shows work wasted reading rows that the index could have skipped.
- `Buffers: shared hit` pages came from memory, `read` from disk.

Node names you will meet: `Seq Scan`, `Index Scan`, `Index Only Scan`, `Bitmap Heap Scan`,
`Nested Loop`, `Hash Join`, `Merge Join`, `Sort` (with `external merge` when it spills to
disk), `HashAggregate`, `Limit`.

MySQL has `EXPLAIN` and `EXPLAIN ANALYZE` (8.0.18+) with a different format; SQL Server shows
graphical or XML plans (`SET STATISTICS PROFILE ON`); SQLite has `EXPLAIN QUERY PLAN`. The
ideas are the same.

## Summary

- A B-tree keeps values sorted: it serves equality, ranges, prefix LIKE, ORDER BY and LIMIT.
- Keep the column alone in the condition (sargable); use half-open ranges for times.
- Composite indexes: equality columns first, then the range or sort column.
- Covering (INCLUDE), partial (WHERE) and expression indexes target specific queries.
- Every index slows writes. Low selectivity favours a sequential scan.
- EXPLAIN ANALYZE: read the tree inside out, compare estimated and actual rows, watch loops.

## Further reading

- Use The Index, Luke (the whole book is free online): <https://use-the-index-luke.com/>
- Use The Index, Luke, "Anatomy of an Index": <https://use-the-index-luke.com/sql/anatomy>
- Use The Index, Luke, "Concatenated Indexes": <https://use-the-index-luke.com/sql/where-clause/the-equals-operator/concatenated-keys>
- Use The Index, Luke, functions in WHERE: <https://use-the-index-luke.com/sql/where-clause/functions>
- PostgreSQL 17, "Indexes": <https://www.postgresql.org/docs/17/indexes.html>
- PostgreSQL 17, "Using EXPLAIN": <https://www.postgresql.org/docs/17/using-explain.html>
