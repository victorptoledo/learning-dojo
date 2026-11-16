# Index Lab

`fixture/01_lab.sql` creates six tables with only their primary keys. Each one has a workload
that needs a **different kind** of index:

| Table | Rows | The workload |
| --- | ---: | --- |
| `events (id, at, device_id, kind)` | 200,000 | time ranges such as the last hour: `WHERE at >= ... AND at < ...`. Rows are always appended in time order, and the table will grow to billions, so the index must stay **tiny** |
| `products (id, name, tags text[], attrs jsonb)` | 20,000 | `WHERE tags @> ARRAY['outdoor']` and `WHERE attrs @> '{"brand": "Kestrel"}'` |
| `users (id, email)` | 20,000 | log-in by email whatever its capitalisation, and **no two accounts** with the same email in different case |
| `jobs (id, status, run_at)` | 100,000 | the queue reads the next pending jobs by `run_at`; only 100 jobs are pending |
| `customers (id, name)` | 20,000 | search by any fragment of the name: `WHERE name ILIKE '%silva%'` |
| `orders (id, customer_id, total, notes)` | 100,000 | the account page: `SELECT total FROM orders WHERE customer_id = $1`, answered **from the index alone** |

| File | Kind | Task |
| --- | --- | --- |
| `indexes.sql` | statements | Create the indexes (and any extension they need) for the six workloads, at most eight indexes in all, and change no data. |
| `login.sql` | one query | The user whose email is `ANA.SILVA@example.com`, whatever its case: `id`, `email`. It must use your index. |
| `pending_jobs.sql` | one query | The next ten pending jobs: `id`, `run_at`, oldest `run_at` first. It must use your index without a separate sort. |

How it is checked: each hidden test runs your `indexes.sql`, then `ANALYZE`, then looks at the
plan of the workload's query (no `Seq Scan`, and no `Sort` where the index can give the order),
at the type of the index (`pg_am`), and, for the BRIN and partial indexes, at their size. The
fixture has already vacuumed `orders`, so index-only scans can skip the table.

Try it with `docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play`, then
`\i indexes.sql`, `ANALYZE;`, and `EXPLAIN` each workload. `\di+` lists indexes with their sizes.

There are 13 tests: 1 in `tests/visible.sql` and 12 hidden ones.
