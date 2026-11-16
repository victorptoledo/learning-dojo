# Tenant Events

A SaaS product stores the events of all its customers (tenants) in one table, which grows by
tens of millions of rows a month and keeps two months of history. `fixture/01_tenants.sql`
creates `tenants (id, name)` with tenants 1, 2 and 3. You build the rest.

| File | Kind | Task |
| --- | --- | --- |
| `schema.sql` | statements | Create `events (id bigint identity, tenant_id integer → tenants, at timestamptz, kind text, payload jsonb NOT NULL DEFAULT '{}')`, every column `NOT NULL`, **partitioned by range of `at`**, with partitions `events_2026_10`, `events_2026_11` and `events_2026_12` (calendar months in UTC), a default partition `events_default`, a primary key, and an index on `(tenant_id, at)` that every partition gets. |
| `security.sql` | statements | Create the role `app_api` (no login needed here) that the API uses. It may read `tenants` and read and **add** events, nothing else. Then make the database keep tenants apart: every request runs `SET LOCAL app.tenant_id = '<id>'`, and `app_api` must only see and add events of that tenant. **No tenant means no rows**, not an error. The rule also applies to the table's owner. |
| `retention.sql` | statements | Month-end job: create `events_2027_01` before its first event arrives, and remove October 2026 completely, without a `DELETE`. |
| `tenant_report.sql` | one query | The API's report: the current tenant's events of November 2026 by `kind`: `kind`, `events` (a count). It runs as `app_api`, so it needs no tenant filter of its own, and it must read only the November partition. |

Details that matter:

- A unique key of a partitioned table must include the partition key.
- `current_setting('app.tenant_id')` raises an error when the setting was never set in the
  session; `current_setting('app.tenant_id', true)` returns NULL instead. And a setting that was
  set once with `SET LOCAL` reads as `''` (empty) after that transaction. Your policy must treat
  NULL and `''` as "no tenant".
- The tests read your files as the test user (a superuser, which always bypasses row-level
  security) and then switch to `app_api` with `SET LOCAL ROLE app_api` before checking. Test
  your policy the same way in psql.
- `DETACH PARTITION ... CONCURRENTLY` cannot run inside a transaction block, and the tests run
  your files in one; use the plain form here.

Try it with `docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play`, then
`BEGIN; \i schema.sql`, `\i security.sql`, insert some events, `SET LOCAL ROLE app_api;`,
`SET LOCAL app.tenant_id = '2';` and query.

There are 18 tests: 1 in `tests/visible.sql` and 17 hidden ones.
