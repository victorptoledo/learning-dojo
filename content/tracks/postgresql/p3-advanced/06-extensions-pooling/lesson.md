# Extensions and Connection Pooling

Two topics that decide how far one PostgreSQL server can take you. **Extensions** add types,
indexes, functions and whole subsystems (time series, geography, vector search) without leaving
the database. **Connection pooling** decides whether a fleet of application instances can share
the server without exhausting it, because every connection is a process (lesson
[Inside a PostgreSQL Server](../../p0-first-steps/01-architecture/lesson.md)).

## Extensions

An extension is a packaged set of SQL objects (and often a shared library) installed per
database:

```sql
SELECT name, default_version, installed_version, comment
FROM pg_available_extensions ORDER BY name;         -- what the server can install
CREATE EXTENSION IF NOT EXISTS pg_trgm;              -- install into the current database
CREATE EXTENSION postgis WITH SCHEMA extensions;     -- into a dedicated schema
ALTER EXTENSION pg_trgm UPDATE;                      -- after upgrading the package
\dx                                                  -- installed extensions in psql
```

The files must be present on the server (packages such as `postgresql-17-postgis-3`); on managed
services you can only install the ones the provider allows. Some extensions must also be loaded
at server start with `shared_preload_libraries` (a restart): `pg_stat_statements`,
`auto_explain`, `pg_cron`, `timescaledb`, `pgaudit`.

### The ones you will meet

| Extension | Ships with PostgreSQL? | Gives |
| --- | --- | --- |
| `pg_stat_statements` | yes (contrib) | statistics of every normalised query: calls, total and mean time, rows, buffers. The first thing to install on any server (lesson [Monitoring and Tuning](../../p4-mastery/03-monitoring-tuning/lesson.md)) |
| `auto_explain` | yes | logs the plans of slow statements |
| `pg_trgm` | yes | trigram similarity and indexes for `LIKE`, `ILIKE` and typos |
| `btree_gist`, `btree_gin` | yes | B-tree operators in GiST and GIN indexes (exclusion constraints on `room_id WITH =`) |
| `pgcrypto` | yes | hashing (`crypt`, `gen_salt`), encryption, random bytes |
| `citext` | yes | case-insensitive text type |
| `postgres_fdw`, `dblink` | yes | query tables of other PostgreSQL servers |
| `pgstattuple`, `pg_buffercache`, `amcheck` | yes | inspect bloat, the buffer cache, index corruption |
| `pg_partman` | no | creates and drops partitions on a schedule |
| `pg_cron` | no | cron jobs inside the database |
| `PostGIS` | no | geography and geometry types, spatial indexes and functions |
| `pgvector` | no | a `vector` type with HNSW and IVFFlat indexes for similarity search (embeddings) |
| `TimescaleDB` | no | hypertables, compression and continuous aggregates for time series |

### Extensions are code you trust

An extension runs inside the server with its privileges. Install only maintained extensions from
trusted packages, keep them in a known schema, upgrade them with the server
(`ALTER EXTENSION ... UPDATE` after a package upgrade or `pg_upgrade`), and check that each one
supports your next major version before upgrading. Since PostgreSQL 13, "trusted" extensions
(such as `pg_trgm`, `pgcrypto`, `citext`) can be installed by a database owner without being a
superuser.

### Foreign data wrappers

`postgres_fdw` makes tables of another server look local:

```sql
CREATE EXTENSION postgres_fdw;
CREATE SERVER billing_db FOREIGN DATA WRAPPER postgres_fdw OPTIONS (host 'billing.internal', dbname 'billing');
CREATE USER MAPPING FOR app SERVER billing_db OPTIONS (user 'reader', password '...');
IMPORT FOREIGN SCHEMA public LIMIT TO (invoices) FROM SERVER billing_db INTO billing_remote;
SELECT count(*) FROM billing_remote.invoices WHERE paid_at > now() - INTERVAL '1 day';
```

Conditions are pushed to the remote server when possible; joins across servers can still move a
lot of data. It is great for migrations and occasional reports, poor as a permanent join path
between busy services.

## Connections: the cost of a process each

Every connection is a backend process with its own memory (a few MB at rest, plus `work_mem` per
operation), and the server's snapshot and lock work grows with the number of connections. In
practice:

- A server does its best work with **active connections close to its CPU cores** (a few times
  that at most). Beyond that, more connections only add contention.
- `max_connections` defaults to 100; raising it to thousands costs memory and makes every
  snapshot slower, even when the connections are idle.
- Applications open far more connections than they use: 20 pods × a pool of 20 = 400
  connections, of which maybe 30 are busy at any moment.

```sql
SELECT state, count(*) FROM pg_stat_activity
WHERE backend_type = 'client backend' GROUP BY state;
--  idle 370, active 12, idle in transaction 18
```

### Application pools

Every driver has a pool (HikariCP, SQLAlchemy, node-postgres `Pool`, Npgsql, pgxpool). Size it
**small**: a common starting point is `cores × 2` connections per database server, divided across
the instances. A request waiting a few milliseconds for a pooled connection is far cheaper than a
server thrashing with 1,000 processes.

### PgBouncer and other poolers

A **connection pooler** sits between the applications and the server and multiplexes many client
connections onto a few server connections:

| Mode | A server connection is held | Safe with |
| --- | --- | --- |
| **session** | for the whole client session | everything; little gain |
| **transaction** | for one transaction, then returned | most applications; the usual choice |
| **statement** | for one statement | autocommit only; rarely used |

Transaction mode breaks anything that relies on **session state**, because the next transaction
may run on another server connection:

| Feature | In transaction mode |
| --- | --- |
| `SET` (session-level settings) | leaks to other clients or disappears; use `SET LOCAL` |
| session advisory locks | held by a connection you no longer have; use `pg_try_advisory_xact_lock` |
| `LISTEN` | does not work |
| temporary tables across transactions | do not work |
| protocol-level prepared statements | supported by PgBouncer 1.21+ (`max_prepared_statements`); older versions need them off in the driver |

Other options: Pgpool-II (pooling plus load balancing), Odyssey, PgCat and Supavisor (multi-tenant
poolers), and the poolers built into managed services (RDS Proxy, Azure's built-in PgBouncer).
A pooler is also where you put a short queue in front of the database: `default_pool_size`,
`max_client_conn` and `reserve_pool_size` in PgBouncer.

## Summary

- Extensions add types, indexes and features per database; some need
  `shared_preload_libraries` and a restart; install trusted, maintained ones and upgrade them
  with the server.
- Know the contrib set (`pg_stat_statements`, `pg_trgm`, `btree_gist`, `pgcrypto`,
  `postgres_fdw`, `pgstattuple`) and the big external ones (PostGIS, pgvector, TimescaleDB,
  pg_partman, pg_cron).
- Every connection is a process: keep active connections near the core count, keep application
  pools small, and watch `pg_stat_activity` by state.
- PgBouncer in transaction mode multiplexes many clients onto few server connections; avoid
  session state (`SET`, session advisory locks, `LISTEN`) behind it.

## Further reading

- PostgreSQL 17, "CREATE EXTENSION": <https://www.postgresql.org/docs/17/sql-createextension.html>
- PostgreSQL 17, "Additional Supplied Modules and Extensions" (contrib): <https://www.postgresql.org/docs/17/contrib.html>
- PostgreSQL 17, "postgres_fdw": <https://www.postgresql.org/docs/17/postgres-fdw.html>
- PostgreSQL 17, "Connections and Authentication" settings: <https://www.postgresql.org/docs/17/runtime-config-connection.html>
- PgBouncer, "Features" (what works in each pooling mode): <https://www.pgbouncer.org/features.html>
- PostgreSQL wiki, "Number Of Database Connections": <https://wiki.postgresql.org/wiki/Number_Of_Database_Connections>
- pgvector: <https://github.com/pgvector/pgvector>
