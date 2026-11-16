# VACUUM, Autovacuum, Bloat and Wraparound

MVCC leaves dead tuples behind (previous lesson). **VACUUM** is the process that cleans up after
it, and **autovacuum** runs it for you. Most PostgreSQL performance incidents that are not a
missing index are a vacuum problem: tables bloated to ten times their size, plans gone bad
because statistics are stale, or, at worst, a database that stops accepting writes to protect
itself from transaction id wraparound.

## What VACUUM does

A plain `VACUUM orders`:

1. scans the pages that may have dead tuples (the visibility map tells it which to skip),
2. removes dead tuples from the **indexes** first, then from the heap, and records the free
   space in the **free space map** so new rows reuse it,
3. sets the **all-visible** bit of pages where every tuple is visible to everyone, which
   speeds up index-only scans,
4. **freezes** old tuples (below), and
5. may truncate empty pages at the end of the file.

It runs alongside normal reads and writes (it only takes a `SHARE UPDATE EXCLUSIVE` lock). It
does **not** give space back to the operating system, except for empty pages at the end: the
file keeps its size, and the freed space is reused by later inserts and updates.

| Command | Does | Locks |
| --- | --- | --- |
| `VACUUM t` | removes dead tuples, keeps the file size | lets reads and writes run |
| `VACUUM (VERBOSE, ANALYZE) t` | the same, reports what it did, and refreshes statistics | same |
| `ANALYZE t` | samples rows to refresh the planner's statistics (`pg_stats`) | same |
| `VACUUM FULL t` | rewrites the whole table and its indexes compactly, giving space back | `ACCESS EXCLUSIVE`: blocks everything, even `SELECT` |
| `pg_repack` (extension) | rewrites online with a short lock at the end | brief exclusive lock |

`VACUUM FULL` is a maintenance-window tool. If a table is bloated because vacuum was blocked,
fix the cause first; otherwise it will bloat again.

`VACUUM` cannot run inside a transaction block (`BEGIN ... VACUUM` fails with "VACUUM cannot run
inside a transaction block").

## Autovacuum: when does it run?

The autovacuum launcher wakes up every `autovacuum_naptime` (1 minute) and starts workers (at
most `autovacuum_max_workers`, 3 by default) for tables that crossed a threshold. A table is
vacuumed when

```text
dead tuples > autovacuum_vacuum_threshold + autovacuum_vacuum_scale_factor × reltuples
                       50                  +          0.2                  × rows
```

and analyzed when the rows changed since the last analyze exceed
`autovacuum_analyze_threshold (50) + autovacuum_analyze_scale_factor (0.1) × reltuples`. Since
PostgreSQL 13, inserts alone also trigger a vacuum
(`autovacuum_vacuum_insert_threshold` 1000 + `autovacuum_vacuum_insert_scale_factor` 0.2), so
append-only tables get their visibility map set and their tuples frozen.

The default 20% is fine for small tables and **far too lazy for big ones**: a table with 500
million rows waits for 100 million dead tuples. Set it per table, as a storage parameter:

```sql
ALTER TABLE events SET (autovacuum_vacuum_scale_factor = 0.01,
                        autovacuum_vacuum_threshold = 10000,
                        autovacuum_analyze_scale_factor = 0.005);

SELECT relname, reloptions FROM pg_class WHERE relname = 'events';
--  events | {autovacuum_vacuum_scale_factor=0.01,autovacuum_vacuum_threshold=10000,...}

ALTER TABLE events RESET (autovacuum_vacuum_threshold);   -- back to the global value
```

### Speed: the cost limit

To avoid hurting the workload, vacuum counts the pages it touches and sleeps after spending its
budget: `vacuum_cost_limit` (200 points, shared by all autovacuum workers) and
`autovacuum_vacuum_cost_delay` (2 ms). On modern SSDs, autovacuum is usually **too slow**, not
too aggressive: raise `autovacuum_vacuum_cost_limit` (for example to 1000–2000) before you
consider turning it off. **Never turn autovacuum off.**

### Watching it

```sql
SELECT relname, n_live_tup, n_dead_tup, last_autovacuum, last_autoanalyze,
       autovacuum_count
FROM pg_stat_user_tables
ORDER BY n_dead_tup DESC LIMIT 10;

SELECT * FROM pg_stat_progress_vacuum;       -- what running vacuums are doing now
```

`log_autovacuum_min_duration = '10s'` logs every autovacuum run that takes longer than that.

## What stops VACUUM from removing tuples

VACUUM can only remove tuples that are dead to **every** snapshot in the cluster. The oldest
snapshot is the "xmin horizon", and four things hold it back:

| Cause | Where you see it | Fix |
| --- | --- | --- |
| a long transaction, or a session **idle in transaction** | `pg_stat_activity.backend_xmin`, `xact_start`, `state` | end it; set `idle_in_transaction_session_timeout` |
| a replica with `hot_standby_feedback = on` running a long query | `pg_stat_replication.backend_xmin` | shorter queries on the replica, `max_standby_streaming_delay` |
| an abandoned **replication slot** | `pg_replication_slots.xmin`, `catalog_xmin` | drop the slot |
| a forgotten **prepared transaction** | `pg_prepared_xacts` | `COMMIT PREPARED` or `ROLLBACK PREPARED` |

`VACUUM (VERBOSE)` says it plainly: "tuples: 0 removed, 1000000 remain, 1000000 are dead but not
yet removable".

## Bloat

**Bloat** is the space taken by dead tuples and free space that is not being reused well. A
table that is three times larger than its live data reads three times more pages. Ways to
measure it:

- `pgstattuple('orders')` (extension) scans the table and reports exact live, dead and free
  space; `pgstattuple_approx` is cheaper.
- Compare `pg_relation_size` with the live rows × row width, as the well-known "bloat estimate"
  queries do.
- Indexes bloat too; `REINDEX INDEX CONCURRENTLY` rebuilds one without blocking writes.

## Transaction id wraparound and freezing

Transaction ids are **32 bits**: about 4 billion values used in a circle, where half of them are
"in the past" and half "in the future" of any id. A tuple whose `xmin` is more than ~2 billion
transactions old would suddenly look like it was created in the future, and disappear.

To prevent that, VACUUM **freezes** old tuples: it marks them as "visible to everyone, forever",
so their `xmin` no longer matters. Each table records the oldest unfrozen `xmin` in
`pg_class.relfrozenxid`, and each database in `pg_database.datfrozenxid`:

```sql
SELECT datname, age(datfrozenxid) AS xid_age,
       round(100.0 * age(datfrozenxid) / current_setting('autovacuum_freeze_max_age')::int, 1)
           AS percent_of_freeze_max_age
FROM pg_database ORDER BY xid_age DESC;

SELECT c.oid::regclass, age(c.relfrozenxid) AS xid_age
FROM pg_class AS c WHERE c.relkind IN ('r', 'm', 't')
ORDER BY xid_age DESC LIMIT 10;
```

When a table's age passes `autovacuum_freeze_max_age` (200 million), autovacuum starts an
**aggressive, anti-wraparound vacuum** on it, even if autovacuum is disabled for the table, and
it will not cancel itself to let a lock through. If the age keeps growing (because something
holds back the xmin horizon), PostgreSQL warns in the log from 40 million transactions before the
limit, and at 3 million it **refuses to assign new transaction ids**: the database becomes
read-only until someone runs a manual VACUUM. Monitor `age(datfrozenxid)` and alert well before
50% of 2 billion.

## Summary

- VACUUM removes dead tuples, fills the free space and visibility maps, and freezes old tuples; it
  does not shrink the file. VACUUM FULL does, with an exclusive lock.
- Autovacuum triggers at `threshold + scale_factor × reltuples` dead tuples; lower the scale
  factor per table for big tables and raise the cost limit rather than disabling it.
- Long or idle-in-transaction sessions, replica feedback, abandoned slots and prepared
  transactions hold back the xmin horizon and cause bloat.
- Watch `n_dead_tup`, `last_autovacuum` and `age(datfrozenxid)`; anti-wraparound vacuums start
  at 200 million, and PostgreSQL stops writes near 2 billion.

## Further reading

- PostgreSQL 17, "Routine Vacuuming" (with wraparound): <https://www.postgresql.org/docs/17/routine-vacuuming.html>
- PostgreSQL 17, "The Autovacuum Daemon": <https://www.postgresql.org/docs/17/routine-vacuuming.html#AUTOVACUUM>
- PostgreSQL 17, "Automatic Vacuuming" settings: <https://www.postgresql.org/docs/17/runtime-config-autovacuum.html>
- PostgreSQL 17, "VACUUM": <https://www.postgresql.org/docs/17/sql-vacuum.html>
- PostgreSQL 17, storage parameters for tables (per-table autovacuum): <https://www.postgresql.org/docs/17/sql-createtable.html#SQL-CREATETABLE-STORAGE-PARAMETERS>
- PostgreSQL 17, "pgstattuple": <https://www.postgresql.org/docs/17/pgstattuple.html>
- PostgreSQL 17, "VACUUM Progress Reporting": <https://www.postgresql.org/docs/17/progress-reporting.html#VACUUM-PROGRESS-REPORTING>
