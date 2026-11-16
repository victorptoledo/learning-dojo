# Monitoring and Tuning in Production

A production database needs two habits: **watching** the few numbers that predict trouble, and
**tuning** from evidence rather than from folklore. This lesson collects what the track taught
into an operating practice: `pg_stat_statements` to find the queries that cost the most, the
statistics views to watch, the settings that matter for throughput, and a method for working
an incident.

## pg_stat_statements: where the time goes

```text
shared_preload_libraries = 'pg_stat_statements'     # restart
pg_stat_statements.track = top
```

```sql
CREATE EXTENSION pg_stat_statements;

SELECT queryid, calls,
       round(total_exec_time) AS total_ms,
       round(mean_exec_time, 2) AS mean_ms,
       rows,
       shared_blks_hit, shared_blks_read,
       round(100.0 * total_exec_time / sum(total_exec_time) OVER (), 1) AS percent_of_total,
       left(query, 80) AS query
FROM pg_stat_statements
ORDER BY total_exec_time DESC
LIMIT 10;
```

Queries are **normalised** (`WHERE id = $1`), so a query run a million times is one row. Sort by
`total_exec_time` to find what costs the server most (often a fast query called very often), by
`mean_exec_time` for what users wait on, and by `shared_blks_read` for I/O. Reset with
`pg_stat_statements_reset()` after a change to compare. Then take the top query to
`EXPLAIN (ANALYZE, BUFFERS)` (lesson
[Reading the Query Planner](../../p2-intermediate/01-query-planner/lesson.md)).

`auto_explain` logs the plan of every statement slower than `auto_explain.log_min_duration`,
which catches the slow plan that only happens at 3 a.m. with certain parameters.

## What to watch

| Signal | Where | Trouble when |
| --- | --- | --- |
| connections by state | `pg_stat_activity` | near `max_connections`; many `idle in transaction` |
| long transactions | `pg_stat_activity.xact_start`, `backend_xmin` | open for minutes: they block VACUUM and may hold locks |
| lock waits | `pg_stat_activity.wait_event_type = 'Lock'`, `log_lock_waits` | waits longer than a second, chains |
| cache hit ratio | `pg_stat_database` (`blks_hit` / (`blks_hit` + `blks_read`)) | dropping below ~99% for an OLTP workload |
| dead tuples, last autovacuum | `pg_stat_user_tables` | `n_dead_tup` growing, `last_autovacuum` old on busy tables |
| transaction id age | `age(datfrozenxid)` in `pg_database` | above ~40% of 2 billion |
| replication lag, slots | `pg_stat_replication`, `pg_replication_slots` | lag growing; retained WAL growing |
| WAL archiving | `pg_stat_archiver` | `failed_count` rising, `last_archived_time` old |
| checkpoints | `pg_stat_checkpointer` (17+), `log_checkpoints` | many requested (WAL-triggered) checkpoints |
| temp files | `pg_stat_database.temp_bytes`, `log_temp_files` | large spills: `work_mem` too small for some queries |
| deadlocks, conflicts | `pg_stat_database.deadlocks`, `conflicts` | any increase |
| disk | the OS | `pg_wal` or data volume filling up |
| sequences | `pg_sequences.last_value` against the type's maximum | an `integer` id above 75% of 2,147,483,647 |

Tools that collect these for you: `postgres_exporter` with Prometheus and Grafana, pganalyze,
pgwatch, Datadog, the managed services' dashboards. They all read the same views. Grant the
monitoring user `pg_monitor`, never superuser.

## Tuning, in order of payoff

1. **Fix the queries.** The top of `pg_stat_statements` is usually a missing index, a bad
   estimate, an N+1 pattern or a query that reads far more than it returns. Nothing in
   `postgresql.conf` beats it.
2. **Keep autovacuum ahead** (lesson [VACUUM](../../p1-fundamentals/03-vacuum/lesson.md)): per-table
   scale factors for big tables, a higher `autovacuum_vacuum_cost_limit`, enough
   `autovacuum_max_workers`.
3. **Memory**: `shared_buffers` ~25% of RAM, `effective_cache_size` 50–75%, `work_mem` raised
   per role for reports, `maintenance_work_mem` 512 MB–2 GB (lesson
   [Configuration](../../p1-fundamentals/05-configuration/lesson.md)).
4. **Storage costs**: `random_page_cost = 1.1` and `effective_io_concurrency = 200` on SSDs, so
   the planner stops avoiding index scans.
5. **Checkpoints and WAL**: `max_wal_size` large enough that checkpoints are time-triggered,
   `checkpoint_completion_target = 0.9`, `wal_compression = on` (lz4 or zstd) for write-heavy
   servers.
6. **Connections**: a pooler and small pools (lesson
   [Extensions and Connection Pooling](../../p3-advanced/06-extensions-pooling/lesson.md)).
7. **Parallelism**: `max_parallel_workers_per_gather` (2–4) for analytic queries; less for pure
   OLTP.

Change **one thing at a time**, measure before and after with the same workload, and record why
you changed it. Tools such as PGTune give a reasonable starting configuration from the hardware;
they do not replace measuring.

## Sequence exhaustion: a slow-motion incident

An `integer` (int4) identity or `serial` column stops at 2,147,483,647. When it is reached, every
insert fails with "nextval: reached maximum value of sequence". On a busy table that takes a few
years, and it always comes as a surprise:

```sql
SELECT schemaname, sequencename, data_type, last_value, max_value,
       round(100.0 * last_value / max_value, 1) AS percent_used
FROM pg_sequences
WHERE last_value IS NOT NULL
ORDER BY percent_used DESC;
```

Use `bigint` for new keys. To fix an existing `integer` key, add a `bigint` column, backfill it in
batches, and switch with a short lock; or, as a stopgap, restart the sequence at
−2,147,483,648 and use the negative half.

## Working an incident

```text
1. Is the database up and accepting connections?       psql, pg_isready, connection errors in the app
2. What is running right now, and what waits?           pg_stat_activity by state, wait_event, xact_start
3. Who blocks whom?                                     pg_blocking_pids
4. What changed?                                        deploys, migrations, traffic, settings, plan changes
5. What do the logs say?                                errors, slow statements, lock waits, checkpoints, autovacuum
6. Stop the bleeding                                    cancel or terminate, pause the job, add the missing index CONCURRENTLY
7. Fix the cause, then write it down                    timeouts, monitoring, a test, a runbook entry
```

The usual suspects, in rough order: a long or idle-in-transaction session, a migration queued on a
lock, a plan that flipped after statistics changed, a missing index on a new query, connection
exhaustion, a full disk (WAL from a slot or failed archiving), autovacuum falling behind, and
sequence or transaction id exhaustion.

## Summary

- Install `pg_stat_statements` everywhere; sort by total time, then explain the top queries;
  use `auto_explain` for plans you cannot reproduce.
- Watch connections and long transactions, locks, cache hits, dead tuples, xid age, replication
  lag and slots, archiving, checkpoints, temp files, disk and sequences, with a `pg_monitor` role.
- Tune queries first, then autovacuum, memory, storage costs, checkpoints, connections and
  parallelism, one change at a time.
- Integer keys run out: monitor `pg_sequences` and use `bigint`.
- In an incident: what runs, who waits, what changed, stop the bleeding, fix the cause.

## Further reading

- PostgreSQL 17, "pg_stat_statements": <https://www.postgresql.org/docs/17/pgstatstatements.html>
- PostgreSQL 17, "auto_explain": <https://www.postgresql.org/docs/17/auto-explain.html>
- PostgreSQL 17, "The Cumulative Statistics System" (every pg_stat view): <https://www.postgresql.org/docs/17/monitoring-stats.html>
- PostgreSQL 17, "Monitoring Disk Usage": <https://www.postgresql.org/docs/17/diskusage.html>
- PostgreSQL 17, "pg_sequences": <https://www.postgresql.org/docs/17/view-pg-sequences.html>
- PostgreSQL wiki, "Tuning Your PostgreSQL Server": <https://wiki.postgresql.org/wiki/Tuning_Your_PostgreSQL_Server>
- postgres_exporter for Prometheus: <https://github.com/prometheus-community/postgres_exporter>
