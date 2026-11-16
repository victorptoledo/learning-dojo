# Incident Clinic

Four tickets from a bad week on call. `fixture/01_incident.sql` creates the shop's tables
(`orders` with an old `serial` key, `order_items`, `tickets`, `events`, `invoices`), sets their
sequences where production has them, and saves this morning's `pg_stat_statements` as a table:

```text
statements_snapshot (queryid, query, calls, total_exec_time (ms), rows, shared_blks_hit, shared_blks_read)
```

| File | Kind | Ticket |
| --- | --- | --- |
| `vacuum_blockers.sql` | one query | "Dead tuples keep growing and VACUUM says they are not yet removable." List the **other** sessions that hold back the cleanup: those that keep a snapshot or a transaction id open. Return `application_name`, `state`. |
| `sequences.sql` | one query | "Will we run out of ids?" Every sequence, in any schema, that has used **more than 75%** of its range: `schema_name`, `sequence_name`, `data_type`, `percent_used` (of its own maximum, rounded to 1 decimal), most used first, then by name. Sequences never used yet have no last value. |
| `top_queries.sql` | one query | "The CPU is at 100%: what should we fix first?" The three statements with the most total execution time: `queryid`, `total_seconds` (rounded to a whole number), `percent_of_total` (of all statements' time, 1 decimal), `mean_ms` (2 decimals), `hit_percent` (shared hits as a percentage of hits plus reads, 1 decimal; NULL when the statement touched no page). |
| `fix_orders_id.sql` | statements | "`orders.id` is at 95% of the `integer` limit." Make sure new orders keep getting ids after 2,147,483,647, and that `order_items` can still reference them. Keep every row. |

About the first ticket: the hidden tests open real sessions. One runs a `REPEATABLE READ` report
and goes quiet, one updates a row and goes quiet, one is connected without a transaction, and one
ran `BEGIN` but has not read anything yet. Look at the `backend_xmin` and `backend_xid` columns of
`pg_stat_activity`, and remember that your own session holds a snapshot while it runs your query.

About the last ticket: a `serial` column is an `integer` column with a separate sequence whose
own data type is `integer` too.

Try it with `docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play`. Open more
sessions with `docker exec -it <container> psql -d dojo` to see `backend_xmin` and
`backend_xid` change as they begin, read and write.

There are 11 tests: 1 in `tests/visible.sql` and 10 hidden ones.
