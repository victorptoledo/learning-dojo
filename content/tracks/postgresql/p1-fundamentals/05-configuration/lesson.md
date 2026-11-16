# Configuration: Settings, Memory, Timeouts and Logging

PostgreSQL has more than 350 settings. You need about twenty of them, and you need to know where a
value comes from and what it takes to change it. This lesson covers both, then the memory,
timeout and logging settings every production server should set deliberately. Tuning for
throughput comes back in the lesson
[Monitoring and Tuning in Production](../../p4-mastery/03-monitoring-tuning/lesson.md).

## Where a setting comes from

A setting can be set in many places. The **later** ones in this list win:

| Level | How | Scope |
| --- | --- | --- |
| built-in default | compiled in (`boot_val` in `pg_settings`) | everything |
| `postgresql.conf` (and files it `include`s) | edit the file, then reload or restart | the server |
| `postgresql.auto.conf` | `ALTER SYSTEM SET work_mem = '32MB'` | the server; never edit this file by hand |
| command line | `postgres -c shared_buffers=4GB` | the server |
| database | `ALTER DATABASE shop SET statement_timeout = '30s'` | new sessions in that database |
| role | `ALTER ROLE reporting SET work_mem = '256MB'` | new sessions of that role |
| role in database | `ALTER ROLE app IN DATABASE shop SET ...` | both |
| session | `SET work_mem = '64MB'` | this session |
| transaction | `SET LOCAL work_mem = '64MB'` | until `COMMIT` or `ROLLBACK` |
| function | `CREATE FUNCTION ... SET search_path = ...` | while the function runs |

`pg_settings` tells you the value **and** where it came from:

```sql
SELECT name, setting, unit, source, sourcefile, sourceline, pending_restart
FROM pg_settings WHERE name IN ('shared_buffers', 'work_mem', 'statement_timeout');

SHOW work_mem;                        -- just the value
SELECT current_setting('work_mem');   -- the same, usable in a query
SELECT * FROM pg_file_settings WHERE error IS NOT NULL;   -- typos in the config files
```

## What it takes to change one: the context

Each setting has a **context** (`pg_settings.context`) that says when it can change:

| Context | Change takes effect | Examples |
| --- | --- | --- |
| `postmaster` | only after a **restart** | `shared_buffers`, `max_connections`, `wal_level`, `listen_addresses` |
| `sighup` | after a **reload** (`SELECT pg_reload_conf()`, `pg_ctl reload`) | `autovacuum_*`, `log_*`, `checkpoint_timeout` |
| `superuser` | at once, by a superuser (or a role granted the right) | `log_min_duration_statement` in a session |
| `user` | at once, by anyone, with `SET` | `work_mem`, `statement_timeout`, `search_path` |

After an `ALTER SYSTEM` or an edit of a `postmaster` setting, `pending_restart` is true until you
restart. Plan those changes for a maintenance window; most others need only a reload.

## Memory

| Setting | Is | Rule of thumb |
| --- | --- | --- |
| `shared_buffers` | PostgreSQL's page cache, shared by all backends | about 25% of the RAM on a dedicated server; default 128 MB is tiny |
| `effective_cache_size` | **not an allocation**: tells the planner how much the OS cache plus shared buffers can hold | 50–75% of RAM |
| `work_mem` | memory **per sort or hash operation**, per backend, before spilling to disk | 4 MB default; 16–64 MB is common; raise it per role or per query for reports |
| `hash_mem_multiplier` | hash tables may use `work_mem × this` (2.0) | |
| `maintenance_work_mem` | for `CREATE INDEX`, `VACUUM`, `ALTER TABLE ADD FOREIGN KEY` | 256 MB – 1 GB |
| `autovacuum_work_mem` | the same for autovacuum workers (‑1 = use maintenance_work_mem) | |

`work_mem` is the dangerous one: it is **not a per-connection limit**. A query with four sorts and
two hash joins may use six times `work_mem`, and 200 connections may each run one. Raise it for
the sessions that need it, not globally:

```sql
ALTER ROLE reporting SET work_mem = '256MB';
-- or inside one transaction
BEGIN; SET LOCAL work_mem = '512MB'; SELECT ... ; COMMIT;
```

`EXPLAIN ANALYZE` shows when a sort spilled: `Sort Method: external merge  Disk: 51200kB`.
`log_temp_files = 0` logs every temporary file.

## Timeouts every application should have

| Setting | Stops | Typical value |
| --- | --- | --- |
| `statement_timeout` | any statement running longer than this (SQLSTATE 57014) | 30 s for an API role; off for batch jobs |
| `lock_timeout` | a statement waiting for a lock longer than this (55P03) | 2–5 s in migrations |
| `idle_in_transaction_session_timeout` | sessions sitting in an open transaction | 1–5 min |
| `idle_session_timeout` | idle sessions (careful with pools) | usually off |
| `transaction_timeout` (17+) | whole transactions longer than this | per role |

Set them **per role or per database** rather than globally, so a migration or a nightly job can
use different values:

```sql
ALTER ROLE api SET statement_timeout = '30s';
ALTER ROLE api SET idle_in_transaction_session_timeout = '60s';
```

## Logging that pays for itself

```text
log_line_prefix = '%m [%p] %q%u@%d %a '   # time, pid, user@database, application
log_min_duration_statement = '500ms'      # every statement slower than this, with its text
log_lock_waits = on                       # waits longer than deadlock_timeout (1 s)
log_temp_files = 0                        # sorts and hashes that spilled to disk
log_checkpoints = on                      # on by default since 15
log_autovacuum_min_duration = '10s'       # slow autovacuum runs
log_connections = on                      # if you need an audit of who connects
```

With these, the log answers most "why was it slow at 03:12?" questions. The `auto_explain`
extension can also log the plan of slow statements (lesson
[Monitoring and Tuning in Production](../../p4-mastery/03-monitoring-tuning/lesson.md)).

## The other configuration files

- `pg_hba.conf` ("host-based authentication") decides **who may connect from where, and how**
  (`scram-sha-256`, `cert`, `peer`). It is read on reload. Lesson
  [Roles, Privileges and Row-Level Security](../../p3-advanced/04-security/lesson.md) covers it.
- `pg_ident.conf` maps operating-system user names to database roles.

Managed services (Amazon RDS, Cloud SQL, Azure) do not give you the files: you change settings
through **parameter groups** or flags, and some (like `shared_buffers`) are preset from the
instance size. `pg_settings` still tells you the truth.

## Summary

- Later levels win: defaults, the config files, `ALTER SYSTEM`, database, role, session,
  transaction, function. `pg_settings.source` tells you which one did.
- `postmaster` settings need a restart, `sighup` ones a reload, `user` ones only a `SET`.
- `shared_buffers` about 25% of RAM, `effective_cache_size` is a hint, `work_mem` is per
  operation; raise it per role or with `SET LOCAL`.
- Give application roles `statement_timeout` and `idle_in_transaction_session_timeout`, and
  migrations `lock_timeout`.
- Log slow statements, lock waits, temp files, checkpoints and slow autovacuum runs.

## Further reading

- PostgreSQL 17, "Setting Parameters": <https://www.postgresql.org/docs/17/config-setting.html>
- PostgreSQL 17, "Resource Consumption" (memory): <https://www.postgresql.org/docs/17/runtime-config-resource.html>
- PostgreSQL 17, "Client Connection Defaults" (timeouts): <https://www.postgresql.org/docs/17/runtime-config-client.html>
- PostgreSQL 17, "Error Reporting and Logging": <https://www.postgresql.org/docs/17/runtime-config-logging.html>
- PostgreSQL 17, "pg_settings": <https://www.postgresql.org/docs/17/view-pg-settings.html>
- PostgreSQL 17, "ALTER SYSTEM": <https://www.postgresql.org/docs/17/sql-altersystem.html>
- PostgreSQL wiki, "Tuning Your PostgreSQL Server": <https://wiki.postgresql.org/wiki/Tuning_Your_PostgreSQL_Server>
