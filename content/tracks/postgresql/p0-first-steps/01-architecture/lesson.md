# Inside a PostgreSQL Server: Clusters, Processes, psql and the Catalogs

The SQL track taught you the language, using PostgreSQL 17 as the engine. This track is about
the engine itself: how it stores rows, why it needs VACUUM, how it plans, locks, replicates and
recovers, and how you run it in production. If you have not done the SQL track, its lesson
[Relational Databases, SQL and Your First PostgreSQL](../../../sql/p0-first-steps/01-setup/lesson.md)
shows how to get a server and psql; this lesson assumes you can run queries.

## One server, seen from outside

```text
                 client (psql, your app, a pool)
                        │ TCP 5432 or a Unix socket
                        ▼
  ┌──────────────── postmaster (the "postgres" process) ────────────────┐
  │  forks one backend per connection                                   │
  │   backend   backend   backend ...    each runs your queries         │
  │        \       |        /                                           │
  │   ┌──────── shared memory ─────────┐                                │
  │   │ shared_buffers (page cache)    │   background processes:        │
  │   │ WAL buffers, lock table        │   checkpointer, background     │
  │   └────────────────────────────────┘   writer, walwriter,           │
  │                                        autovacuum launcher+workers, │
  │                                        WAL senders, archiver        │
  └───────────────────────────┬─────────────────────────────────────────┘
                              ▼
     data directory (PGDATA): base/ (tables, 8 kB pages), pg_wal/ (the log),
     postgresql.conf, pg_hba.conf, ...
```

- **One process per connection.** The postmaster listens and forks a *backend* process for each
  client. A backend has its own memory for sorts, hashes and caches, so 1,000 connections are
  1,000 processes. That is why production systems put a **connection pool** in front (lesson
  [Extensions and Connection Pooling](../../p3-advanced/06-extensions-pooling/lesson.md)).
- **Shared buffers** are PostgreSQL's page cache: tables and indexes are files of 8 kB pages,
  read into shared memory. The operating system's file cache sits under it.
- **The write-ahead log (WAL).** Every change is first written to the log in `pg_wal/`; the data
  pages are written later by the checkpointer and background writer. After a crash, the server
  replays the WAL from the last checkpoint. The same log feeds replicas and point-in-time
  recovery (phase Mastery).
- **Background processes** do the housekeeping: the checkpointer, the autovacuum workers that
  clean up old row versions (phase Fundamentals), the WAL writer and, on a primary with
  replicas, WAL senders.

## Cluster, database, schema

| Word | What it is | One connection sees |
| --- | --- | --- |
| **cluster** | one server instance: one data directory, one port, one set of roles | — |
| **database** | an isolated set of schemas inside the cluster (`CREATE DATABASE shop`) | exactly one, chosen when connecting |
| **schema** | a namespace inside a database (`public`, `billing`, `pg_catalog`) | all of them, as `schema.table` |
| **role** | a user or group, shared by every database of the cluster | — |

A query cannot join tables of two databases (that needs `postgres_fdw` or `dblink`), but it can
join any schemas of its database. The `search_path` setting decides which schemas an unqualified
name is looked up in (`SHOW search_path` gives `"$user", public` by default).

## Versions

A major version ships every year (17 in 2024, 18 in 2025) and is supported for **five years**
with minor releases (17.1, 17.2, ...) that only fix bugs and security issues. Minor upgrades are
a restart with new binaries; major upgrades change the on-disk format and need `pg_upgrade`, a
dump and restore, or logical replication (lesson
[Backups, Point-in-Time Recovery and Upgrades](../../p4-mastery/02-backup-recovery/lesson.md)).
Always run the latest minor release of your major version.

## psql, the tool you will use every day

| Command | Does | Reads, roughly |
| --- | --- | --- |
| `\l` | list databases | `pg_database` |
| `\c shop` | connect to another database | a new connection |
| `\dn`, `\dt`, `\di`, `\dv`, `\df` | schemas, tables, indexes, views, functions | `pg_namespace`, `pg_class`, `pg_proc` |
| `\d orders` | describe a table: columns, indexes, constraints, triggers | several catalog queries |
| `\d+ orders` | the same with sizes, storage and comments | |
| `\x auto` | expanded output for wide rows | |
| `\timing on` | print each statement's time | |
| `\e` | edit the last query in `$EDITOR` | |
| `\i file.sql` | run a file | |
| `\gexec` | run each value of the result as a statement | |
| `\watch 2` | re-run the last query every 2 seconds | |
| `\set ECHO_HIDDEN on` | show the catalog queries behind `\d` | |

`psql -E` (or `ECHO_HIDDEN`) is the best way to learn the catalogs: every `\d` prints the SQL it
runs.

`\gexec` turns a query into a script. This one runs `ANALYZE` on every table of a schema:

```text
SELECT format('ANALYZE %I.%I', schemaname, tablename)
FROM pg_tables WHERE schemaname = 'billing' \gexec
```

## The system catalogs

Everything PostgreSQL knows about your objects is stored in tables in the `pg_catalog` schema,
which you can query like any other table. `information_schema` is the standard, portable view of
the same data, but it hides PostgreSQL-specific details (index types, storage, OIDs).

| Catalog | One row per | Useful columns |
| --- | --- | --- |
| `pg_class` | relation: table (`relkind` `r`), partitioned table (`p`), index (`i`), sequence (`S`), view (`v`), materialized view (`m`), TOAST table (`t`) | `relname`, `relnamespace`, `relkind`, `reltuples` (estimated rows), `relpages` |
| `pg_namespace` | schema | `nspname` |
| `pg_attribute` | column | `attrelid`, `attname`, `attnum` (> 0 for user columns), `attnotnull`, `attisdropped` |
| `pg_constraint` | constraint | `contype` (`p` primary, `f` foreign, `u` unique, `c` check, `x` exclusion), `conrelid`, `conkey` (column numbers), `confrelid` |
| `pg_index` | index | `indexrelid`, `indrelid`, `indkey`, `indisunique` |
| `pg_settings` | setting | `name`, `setting`, `unit`, `source`, `boot_val`, `reset_val`, `context` |
| `pg_stat_activity` | backend | `pid`, `usename`, `state`, `query`, `wait_event_type`, `xact_start` |
| `pg_stat_user_tables` | table, with activity counters | `seq_scan`, `idx_scan`, `n_live_tup`, `n_dead_tup`, `last_autovacuum` |

Objects are identified by an **OID**. The `regclass` type converts between names and OIDs, so you
rarely need to join `pg_class` by hand:

```sql
SELECT 'public.orders'::regclass::oid;          -- the OID
SELECT 16412::regclass;                         -- back to a name, schema-qualified if needed
SELECT attname, format_type(atttypid, atttypmod)
FROM pg_attribute
WHERE attrelid = 'orders'::regclass AND attnum > 0 AND NOT attisdropped
ORDER BY attnum;
```

`conkey` and `indkey` hold **column numbers** (`attnum`), not names. To compare them, remember
that `pg_index.indkey` is an `int2vector` whose first element is `indkey[0]`, while
`pg_constraint.conkey` is an ordinary `smallint[]` starting at `conkey[1]`.

### How big is it?

| Function | Counts |
| --- | --- |
| `pg_relation_size(rel)` | the main data file only |
| `pg_table_size(rel)` | the table with its TOAST and free-space maps, without indexes |
| `pg_indexes_size(rel)` | all indexes of the table |
| `pg_total_relation_size(rel)` | all of the above |
| `pg_database_size('shop')` | a whole database |
| `pg_size_pretty(bytes)` | `8192` → `8192 bytes`, `10485760` → `10 MB` |

```sql
SELECT c.oid::regclass AS table_name,
       pg_size_pretty(pg_total_relation_size(c.oid)) AS total
FROM pg_class AS c
JOIN pg_namespace AS n ON n.oid = c.relnamespace
WHERE c.relkind IN ('r', 'p')
  AND n.nspname NOT IN ('pg_catalog', 'information_schema')
ORDER BY pg_total_relation_size(c.oid) DESC
LIMIT 10;
```

**TOAST** ("The Oversized-Attribute Storage Technique") is where PostgreSQL keeps values too large
for a row on its page (more than about 2 kB): long text, big JSON documents. They are compressed
and moved to a side table, which is why `pg_table_size` can be much bigger than
`pg_relation_size`.

### What is running right now?

```sql
SELECT pid, usename, state, now() - xact_start AS in_transaction, left(query, 60)
FROM pg_stat_activity
WHERE backend_type = 'client backend'
ORDER BY xact_start NULLS LAST;
```

You will use this query in almost every incident (phase Mastery).

## Summary

- A cluster is one server with one data directory; a connection sees one database and all of
  its schemas; roles are cluster-wide.
- Each connection is a process; shared buffers cache 8 kB pages; every change goes to the WAL
  first, and checkpoints write the pages later.
- A major every year, each supported five years; always run the latest minor.
- psql's `\d` family, `\x`, `\timing`, `\gexec` and `\watch` cover daily work; `psql -E` shows
  the catalog queries behind them.
- `pg_class`, `pg_attribute`, `pg_constraint`, `pg_index`, `pg_settings` and `pg_stat_*` answer
  almost any question about a database; `regclass` and the size functions save joins.

## Further reading

- PostgreSQL 17, "Overview of PostgreSQL Internals": <https://www.postgresql.org/docs/17/overview.html>
- PostgreSQL 17, "Database Physical Storage": <https://www.postgresql.org/docs/17/storage.html>
- PostgreSQL 17, "psql": <https://www.postgresql.org/docs/17/app-psql.html>
- PostgreSQL 17, "System Catalogs": <https://www.postgresql.org/docs/17/catalogs.html>
- PostgreSQL 17, "Database Object Management Functions" (sizes): <https://www.postgresql.org/docs/17/functions-admin.html#FUNCTIONS-ADMIN-DBOBJECT>
- PostgreSQL versioning policy: <https://www.postgresql.org/support/versioning/>
- Egor Rogov, "PostgreSQL 14 Internals" (free book): <https://postgrespro.com/community/books/internals>
