# Advanced Indexes: GIN, BRIN, Partial and Expression Indexes

The SQL track's lessons [Indexes and Query Plans](../../../sql/p3-advanced/01-indexes-plans/lesson.md)
and [Index Tuning](../../../sql/p3-advanced/02-index-tuning/lesson.md) covered the B-tree: column
order in composite indexes, sargable conditions and covering indexes. PostgreSQL has five more
index types, plus partial and expression indexes that work with all of them. Choosing well is
often the difference between a 2 GB index that is never used and a 20 MB one that serves the
query in a millisecond.

## The index types

| Type | Built for | Operators | Typical use |
| --- | --- | --- | --- |
| **B-tree** (default) | ordered scalar values | `=`, `<`, `>`, `BETWEEN`, `IN`, `IS NULL`, `LIKE 'abc%'` (with `text_pattern_ops` or the C collation), `ORDER BY` | almost everything |
| **Hash** | equality only | `=` | rarely worth it; a B-tree does the same and more |
| **GIN** (generalised inverted) | values that contain many keys | `@>`, `<@`, `&&`, `?`, `@@` | arrays, `jsonb`, `tsvector`, trigrams |
| **GiST** (generalised search tree) | overlapping, nearest, geometric | `&&`, `@>`, `<<`, `<->` (distance) | ranges, exclusion constraints, PostGIS, `pg_trgm` similarity |
| **SP-GiST** | space-partitioned data | `<<`, `>>`, `@>` | `inet`, points, text prefixes |
| **BRIN** (block range) | huge tables whose values follow the physical order | `=`, `<`, `>` | append-only logs, time series |

```sql
SELECT amname FROM pg_am WHERE amtype = 'i';                   -- the index types installed
SELECT indexrelid::regclass, am.amname
FROM pg_index JOIN pg_class c ON c.oid = indexrelid JOIN pg_am am ON am.oid = c.relam
WHERE indrelid = 'events'::regclass;                             -- the types of a table's indexes
```

## B-tree details worth knowing

- **INCLUDE** adds columns to the leaf pages only, to allow index-only scans without making
  them part of the key: `CREATE INDEX ON orders (customer_id) INCLUDE (total, status);`.
  A `UNIQUE` index with `INCLUDE` keeps uniqueness on the key columns only.
- **Deduplication** (PostgreSQL 13+) stores repeated keys once, so indexes on low-cardinality
  columns are much smaller than they used to be.
- **Sort direction**: `(created_at DESC, id DESC)` serves `ORDER BY created_at DESC, id DESC`
  and its exact reverse, not mixed directions such as `created_at DESC, id ASC`.
- `LIKE 'abc%'` needs the `C` collation or `text_pattern_ops` on a B-tree; `LIKE '%abc%'` needs
  a trigram index.

## GIN: many keys per row

A GIN index maps every **key** inside a value (an array element, a jsonb key or path, a lexeme,
a trigram) to the rows that contain it, like the index at the back of a book.

```sql
CREATE INDEX articles_tags ON articles USING gin (tags);                  -- tags @> ARRAY['postgres']
CREATE INDEX products_attrs ON products USING gin (attrs jsonb_path_ops); -- attrs @> '{"brand": "x"}'
CREATE INDEX tickets_doc ON tickets USING gin (doc);                      -- doc @@ to_tsquery(...)
CREATE INDEX customers_name_trgm ON customers USING gin (name gin_trgm_ops); -- name ILIKE '%smi%'
```

GIN is fast to search and slow to update: each new row touches many keys. With `fastupdate`
(on by default), new entries go to a **pending list** first and are merged later by VACUUM or
when the list exceeds `gin_pending_list_limit`, so writes stay fast but searches also scan the
pending list.

## GiST: overlap and distance

GiST indexes store bounding "envelopes" in a tree, so they answer overlap, containment and
nearest-neighbour questions:

```sql
CREATE INDEX bookings_during ON bookings USING gist (during);       -- during && tstzrange(...)
SELECT * FROM places ORDER BY location <-> point '(38.7, -9.1)' LIMIT 5;   -- nearest 5, with a GiST index
```

Exclusion constraints (lesson [PostgreSQL Types](../../p1-fundamentals/01-data-types/lesson.md))
are GiST indexes underneath. For pure "contains this word" searches GIN is usually faster; GiST
is smaller and faster to update.

## BRIN: tiny indexes for naturally ordered data

A BRIN index stores, for each range of table pages (128 by default), the **minimum and maximum**
value found there. A query for one day reads the summaries, skips every block range whose
min–max cannot match, and scans the rest.

```sql
CREATE INDEX events_at_brin ON events USING brin (at) WITH (pages_per_range = 32);
```

| | B-tree on `at` | BRIN on `at` |
| --- | --- | --- |
| size for 1 billion rows | tens of GB | a few MB |
| works when | always | values follow the physical order (`pg_stats.correlation` near 1 or −1) |
| finds | exact rows | candidate block ranges, then rechecks rows (lossy) |

BRIN is ideal for append-only tables where rows arrive in time order (logs, events, metrics). It
is useless when the column is random relative to the storage, and updates or a `CLUSTER` on
another column destroy the correlation. Check with
`SELECT correlation FROM pg_stats WHERE tablename = 'events' AND attname = 'at'`.

## Partial indexes

A partial index covers only the rows matching its `WHERE`:

```sql
-- the queue only ever looks at pending jobs, 0.1% of the table
CREATE INDEX jobs_pending ON jobs (run_at) WHERE status = 'pending';

-- one active subscription per user, any number of cancelled ones
CREATE UNIQUE INDEX one_active_subscription ON subscriptions (user_id) WHERE status = 'active';
```

The planner uses it only when the query's `WHERE` **implies** the index's condition: `WHERE
status = 'pending' AND run_at < now()` can use `jobs_pending`; a query with `status = $1` (a
parameter) cannot know that, unless the plan is custom for that value.

## Expression indexes

An index on an expression serves queries that use **exactly that expression**:

```sql
CREATE UNIQUE INDEX users_email_lower ON users (lower(email));
SELECT * FROM users WHERE lower(email) = lower('Ana@Example.com');   -- uses it
SELECT * FROM users WHERE email = 'ana@example.com';                 -- does not

CREATE INDEX orders_day ON orders ((placed_at AT TIME ZONE 'UTC')::date);   -- note the extra parentheses
```

The expression must be immutable. ANALYZE collects statistics on indexed expressions, which also
improves estimates. Alternatives: a stored generated column with a plain index, or the
`citext` type for case-insensitive text.

## Every index has a price

Each index makes every `INSERT`, many `UPDATE`s (no HOT) and every `DELETE` slower, takes disk
and cache, and must be vacuumed. Look for the ones that do not pay:

```sql
-- never used since the statistics were reset
SELECT s.relname AS table_name, s.indexrelname AS index_name, s.idx_scan,
       pg_size_pretty(pg_relation_size(s.indexrelid)) AS size
FROM pg_stat_user_indexes AS s
JOIN pg_index AS i ON i.indexrelid = s.indexrelid
WHERE s.idx_scan = 0 AND NOT i.indisunique AND NOT i.indisprimary
ORDER BY pg_relation_size(s.indexrelid) DESC;

-- left behind by a failed CREATE INDEX CONCURRENTLY
SELECT indexrelid::regclass FROM pg_index WHERE NOT indisvalid;
```

Check replicas too (each server counts its own `idx_scan`), and `REINDEX INDEX CONCURRENTLY`
rebuilds a bloated index without blocking writes.

## Summary

- B-tree for ordered scalars (with `INCLUDE` for index-only scans); GIN for containment in
  arrays, jsonb, text search and trigrams; GiST for ranges, exclusion and nearest neighbour;
  BRIN for huge naturally ordered tables; SP-GiST for `inet` and points.
- GIN is slow to update (pending list); BRIN needs physical order; GiST is the flexible middle.
- Partial indexes cover the rows you query (and give partial uniqueness); expression indexes
  serve queries that use the same expression.
- Drop indexes nobody uses, find invalid ones, and rebuild bloated ones concurrently.

## Further reading

- PostgreSQL 17, "Index Types": <https://www.postgresql.org/docs/17/indexes-types.html>
- PostgreSQL 17, "Partial Indexes": <https://www.postgresql.org/docs/17/indexes-partial.html>
- PostgreSQL 17, "Indexes on Expressions": <https://www.postgresql.org/docs/17/indexes-expressional.html>
- PostgreSQL 17, "Index-Only Scans and Covering Indexes": <https://www.postgresql.org/docs/17/indexes-index-only-scans.html>
- PostgreSQL 17, "GIN Indexes": <https://www.postgresql.org/docs/17/gin.html>
- PostgreSQL 17, "BRIN Indexes": <https://www.postgresql.org/docs/17/brin.html>
- PostgreSQL 17, "GiST Indexes": <https://www.postgresql.org/docs/17/gist.html>
- Markus Winand, "Use The Index, Luke": <https://use-the-index-luke.com/>
