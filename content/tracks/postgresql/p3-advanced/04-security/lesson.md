# Roles, Privileges and Row-Level Security

Most applications connect to PostgreSQL as one all-powerful user, often the owner of every table
or even a superuser. Then a single SQL injection or leaked password can drop the database. This
lesson covers the security model: who can connect (`pg_hba.conf`), roles and membership,
privileges on schemas and tables, the trap of default privileges, and **row-level security**,
which lets the database itself keep tenants apart.

## Getting in: pg_hba.conf and passwords

`pg_hba.conf` is read top to bottom; the **first** matching line decides how a connection is
authenticated:

```text
# TYPE   DATABASE  USER      ADDRESS          METHOD
local    all       postgres                   peer            # OS user postgres only, via the socket
hostssl  shop      app_api   10.0.0.0/16      scram-sha-256   # password over TLS, from the app network
hostssl  all       all       0.0.0.0/0        cert            # client certificates for everyone else
host     all       all       0.0.0.0/0        reject
```

- Use `scram-sha-256` for passwords (`password_encryption = 'scram-sha-256'`, the default since
  14); never `trust` or `md5` on a network.
- `hostssl` requires TLS; set `ssl = on` and give clients `sslmode=verify-full`.
- Changes need a reload (`SELECT pg_reload_conf()`); `SELECT * FROM pg_hba_file_rules` shows
  how the file was parsed, with errors.

## Roles

PostgreSQL has only **roles**. A "user" is a role with `LOGIN`; a "group" is a role others are
members of. Roles are cluster-wide.

```sql
CREATE ROLE app_readonly NOLOGIN;                         -- a group
CREATE ROLE app_writer   NOLOGIN;
CREATE ROLE api   LOGIN PASSWORD '...' IN ROLE app_writer;   -- a user, member of app_writer
CREATE ROLE report_bot LOGIN IN ROLE app_readonly;
GRANT app_readonly TO app_writer;                         -- writers can also read
```

| Attribute | Gives |
| --- | --- |
| `SUPERUSER` | everything, bypassing every check (including RLS). Only for administrators |
| `CREATEDB`, `CREATEROLE` | create databases, create and manage roles |
| `LOGIN` | may connect |
| `REPLICATION` | may stream WAL (replicas, backups) |
| `BYPASSRLS` | ignores row-level security |
| `INHERIT` (default) | uses the privileges of the roles it is a member of without `SET ROLE` |

Predefined roles grant common powers without superuser: `pg_read_all_data`, `pg_write_all_data`,
`pg_monitor` (for monitoring tools), `pg_signal_backend` (cancel and terminate sessions),
`pg_checkpoint`, `pg_maintain` (17+: VACUUM, ANALYZE, REINDEX on everything).

## Privileges

An object belongs to its **owner**, who can do anything with it, including `DROP` and
`ALTER`. Everyone else needs grants:

```sql
GRANT CONNECT ON DATABASE shop TO app_readonly;
GRANT USAGE ON SCHEMA billing TO app_readonly;                    -- see objects in the schema
GRANT SELECT ON ALL TABLES IN SCHEMA billing TO app_readonly;      -- existing tables only!
GRANT INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA billing TO app_writer;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA billing TO app_writer;      -- for identity and serial columns
GRANT UPDATE (status) ON orders TO support;                        -- column-level privileges
REVOKE ALL ON payments FROM PUBLIC;
```

Two traps:

- `GRANT ... ON ALL TABLES` covers the tables that exist **now**. Tables created tomorrow get
  nothing. Use **default privileges**, which apply to objects a given role creates later:

  ```sql
  ALTER DEFAULT PRIVILEGES FOR ROLE migrator IN SCHEMA billing
      GRANT SELECT ON TABLES TO app_readonly;
  ```

- `PUBLIC` is every role. Since PostgreSQL 15, `PUBLIC` can no longer create objects in the
  `public` schema, but it still has `CONNECT` and `TEMP` on databases and `EXECUTE` on new
  functions. Revoke what you do not want.

A sane layout: a **migrator** role owns the schema and its tables and runs migrations; the
application connects as a role that can only read and write data, so it cannot `DROP` or
`ALTER` anything; reporting tools get a read-only role. `\dp` and `\ddp` in psql show privileges
and default privileges; `has_table_privilege('api', 'orders', 'DELETE')` tests one.

## Row-level security

Privileges decide **which tables** a role can touch. **Row-level security** (RLS) decides
**which rows**: once enabled on a table, every query sees and changes only the rows that the
table's **policies** allow.

```sql
ALTER TABLE invoices ENABLE ROW LEVEL SECURITY;

-- Each request runs: SET LOCAL app.tenant_id = '42';
CREATE POLICY tenant_isolation ON invoices
    USING (tenant_id = current_setting('app.tenant_id')::integer)          -- rows you can see, update, delete
    WITH CHECK (tenant_id = current_setting('app.tenant_id')::integer);    -- rows you can insert or update to
```

| Detail | Behaviour |
| --- | --- |
| no policy, RLS enabled | **nothing** is visible (default deny) |
| `USING` | filters rows for `SELECT`, `UPDATE`, `DELETE`; rows outside it are silently invisible |
| `WITH CHECK` | rejects inserted or updated rows that fail it, with an error (SQLSTATE 42501) |
| `FOR SELECT / INSERT / UPDATE / DELETE`, `TO role` | a policy for some commands or roles only |
| several permissive policies | combined with `OR`; `AS RESTRICTIVE` policies are combined with `AND` |
| table owner | **bypasses** RLS unless `ALTER TABLE ... FORCE ROW LEVEL SECURITY` |
| superusers and `BYPASSRLS` roles | always bypass RLS |

Three mistakes that break tenant isolation:

1. **Testing as the owner or a superuser.** Policies seem to work because they never apply. Test
   with `SET ROLE api`.
2. **A missing setting.** `current_setting('app.tenant_id')` raises an error when the setting does
   not exist; `current_setting('app.tenant_id', true)` returns NULL instead, and `tenant_id =
   NULL` hides every row. Both fail closed, which is what you want; never fall back to "all rows".
3. **Setting the tenant per session behind a pool.** With PgBouncer in transaction mode, use
   `SET LOCAL` (or `set_config('app.tenant_id', '42', true)`) inside each transaction, never a
   session-level `SET`.

RLS also applies to the rows a foreign key check or a `SECURITY DEFINER` function reads only as
seen by their own role, so test the whole request path, not only the `SELECT`.

## Other tools

- `SECURITY DEFINER` functions grant one narrow action (lesson
  [PL/pgSQL](../../p2-intermediate/06-plpgsql/lesson.md)): always with `SET search_path`.
- `pgcrypto` for hashing and encrypting values; `pgaudit` for detailed audit logs of statements.
- `log_connections`, `log_disconnections` and `log_statement = 'ddl'` record who did what.

## Summary

- `pg_hba.conf` decides who connects and how; use `hostssl` and `scram-sha-256`.
- Roles are users and groups; give the application a role that owns nothing and is not a
  superuser; use predefined roles such as `pg_monitor`.
- Grant `USAGE` on schemas and privileges on tables and sequences; use `ALTER DEFAULT
  PRIVILEGES` for future objects; revoke what `PUBLIC` should not have.
- RLS policies filter rows (`USING`) and check new rows (`WITH CHECK`); default deny; owners
  bypass unless `FORCE`; test as the application role; set the tenant with `SET LOCAL`.

## Further reading

- PostgreSQL 17, "Client Authentication" and "The pg_hba.conf File": <https://www.postgresql.org/docs/17/auth-pg-hba-conf.html>
- PostgreSQL 17, "Database Roles": <https://www.postgresql.org/docs/17/user-manag.html>
- PostgreSQL 17, "Predefined Roles": <https://www.postgresql.org/docs/17/predefined-roles.html>
- PostgreSQL 17, "Privileges": <https://www.postgresql.org/docs/17/ddl-priv.html>
- PostgreSQL 17, "ALTER DEFAULT PRIVILEGES": <https://www.postgresql.org/docs/17/sql-alterdefaultprivileges.html>
- PostgreSQL 17, "Row Security Policies": <https://www.postgresql.org/docs/17/ddl-rowsecurity.html>
- PostgreSQL 17, "CREATE POLICY": <https://www.postgresql.org/docs/17/sql-createpolicy.html>
