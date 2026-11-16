# Incremental Loads, Watermarks and Idempotency

The simplest load copies everything every time. It is correct and easy to rerun, and it
stops working when the table has a billion rows. Incremental loads copy **only what
changed**, which is where most pipeline bugs live: duplicates, missed rows and loads that
break when they run twice.

## Full and incremental

| | Full reload | Incremental |
| --- | --- | --- |
| How | truncate and reload (or build a new table and swap) | read what changed since the last run, merge it in |
| Pros | simple, always consistent, sees deletes | fast, cheap, light on the source |
| Cons | slow and expensive for large tables | needs a reliable change marker; misses deletes |
| Use for | small dimensions, reconciliation runs | large or fast-growing tables |

A common setup: incremental every hour, plus a full reconciliation every week to catch what
the incremental logic cannot see.

## Watermarks

A **watermark** records how far the last run got: "everything up to `2026-11-15 06:00`".
The next run reads rows after it. What it measures matters:

- **Source update time** (`updated_at`): needs the source to set it on every change, with
  an index. Beware rows that commit **after** their `updated_at` (a long transaction stamps
  `05:59`, commits at `06:03`, after your `06:00` run already read past it).
- **Ingestion time** (`_ingested_at`, set when the row lands in raw): monotonic in your own
  system, so nothing is skipped between raw and staging.
- **An auto-increment id**: only sees inserts, never updates.

Store the watermark as the **largest value you actually read**, not the clock time of the
run, and only after the load succeeded. To cover late-committing rows, read with a
**lookback window** (`updated_at > watermark - interval '1 hour'`) and rely on idempotent
merging to absorb the overlap.

> [!WARNING]
> A timestamp watermark never sees **deletes**: a deleted row simply stops appearing. Use
> soft deletes (`deleted_at`) in the source, change data capture (phase 2), or a periodic
> full comparison of keys.

## Idempotency

A load is **idempotent** if running it twice with the same input leaves the same result
as running it once. Orchestrators retry failed tasks and people rerun days by hand, so
every load **will** run twice. Three ways to get there:

1. **Upsert** on the natural key:
   ```sql
   INSERT INTO orders (order_id, status, amount, updated_at)
   SELECT order_id, status, amount, updated_at FROM batch
   ON CONFLICT (order_id) DO UPDATE
   SET status = excluded.status, amount = excluded.amount, updated_at = excluded.updated_at
   WHERE excluded.updated_at > orders.updated_at;   -- never go back in time
   ```
2. **MERGE** (SQL standard, PostgreSQL 15+, every warehouse): `WHEN MATCHED THEN UPDATE`,
   `WHEN NOT MATCHED THEN INSERT`, and even `WHEN NOT MATCHED BY SOURCE THEN DELETE` for a
   full sync.
3. **Partition overwrite**: delete (or replace) the whole partition being loaded, such as
   one day, then insert it again, in one transaction. Simple and robust for event data
   keyed by date.

Plain `INSERT` of a batch is **not** idempotent: a retry duplicates it.

## Deduplication

Landing zones contain duplicates: retried extracts, the overlap of a lookback window,
several versions of the same row. Deduplicate **before** merging, deterministically:

```sql
SELECT *
FROM (
    SELECT r.*, row_number() OVER (
               PARTITION BY order_id
               ORDER BY updated_at DESC, _ingested_at DESC) AS rn
    FROM batch AS r
) AS ranked
WHERE rn = 1;
```

The second `ORDER BY` column is the **tie-break**: without it, two rows with the same
`updated_at` are picked at random, and two runs can disagree. In PostgreSQL an upsert even
refuses a batch that touches the same key twice (`cannot affect row a second time`).

## Summary

- Full reloads are simple; incremental loads are fast but need a trustworthy watermark.
- Store the largest value read, after success; use a lookback window for late rows.
- Every load must be idempotent: upsert, MERGE or overwrite a partition.
- Deduplicate with `row_number()` and a deterministic tie-break before merging.
- Timestamp watermarks miss deletes: plan for them.

## Further reading

- PostgreSQL documentation, `INSERT ... ON CONFLICT`: <https://www.postgresql.org/docs/17/sql-insert.html#SQL-ON-CONFLICT>
- PostgreSQL documentation, `MERGE`: <https://www.postgresql.org/docs/17/sql-merge.html>
- dbt documentation, incremental models and strategies: <https://docs.getdbt.com/docs/build/incremental-models>
- Maxime Beauchemin, "Functional Data Engineering: a modern paradigm for batch data processing": <https://maximebeauchemin.medium.com/functional-data-engineering-a-modern-paradigm-for-batch-data-processing-2327ec32c42a>
