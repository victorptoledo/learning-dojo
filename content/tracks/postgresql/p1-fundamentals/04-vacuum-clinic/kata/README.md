# Vacuum Clinic

Four tasks from a week on call. `fixture/01_clinic.sql` creates two real tables, `orders` and
`events`, and a table `stats_snapshot` that someone exported from production last night:

```text
stats_snapshot (relname, reltuples, n_dead_tup, reloptions text[])
    reloptions as pg_class stores them, for example {fillfactor=80,autovacuum_vacuum_threshold=5000}
```

| File | Kind | Task |
| --- | --- | --- |
| `sessions.sql` | statements | Create the table `sessions (id bigint primary key, user_id bigint, token text, last_seen timestamptz, created_at timestamptz)`, every column `NOT NULL`, a `token` belongs to one session only, and lookups by `user_id` and by `token` use an index. The app updates `last_seen` on **every request**: design the table so those updates are **HOT**. |
| `vacuum_due.sql` | one query | From `stats_snapshot`: the tables whose dead tuples are **above** autovacuum's trigger, `threshold + scale_factor × reltuples`. A table's own `autovacuum_vacuum_threshold` and `autovacuum_vacuum_scale_factor` in `reloptions` win over the server settings (read those with `current_setting`). Return `relname`, `n_dead_tup`, `vacuum_at` (the trigger, rounded to a `bigint`) and `autovacuum_disabled` (true when the table has `autovacuum_enabled=false`), by `relname`. |
| `tune.sql` | statements | `events` will grow to hundreds of millions of rows: make autovacuum vacuum it after 1% of its rows plus 1000 are dead, analyze it after 0.5% changed, and give its workers a cost limit of 1000. Someone disabled autovacuum on `orders`: give it back **without** touching its other storage parameters. |
| `wraparound.sql` | one query | Every database of the cluster: `datname`, `xid_age` (the age of its `datfrozenxid`) and `percent_of_freeze_max_age` (that age as a percentage of `autovacuum_freeze_max_age`, rounded to 1 decimal), oldest first, then by name. |

How `sessions.sql` is checked: the tests run your file, insert 5,000 sessions, update
`last_seen` for 250 of them, and read the counters of the current transaction in
`pg_stat_xact_user_tables`. At least 90% of the updates must be HOT. Remember the two conditions
from the lesson: no indexed column changes, and the new version fits on the same page.

Try it with `docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play`. In psql,
`\i sessions.sql`, insert some rows, update them, and look at `ctid` before and after, and at
`n_tup_hot_upd` in `pg_stat_xact_user_tables` (inside `BEGIN`) or `pg_stat_user_tables`.

There are 14 tests: 1 in `tests/visible.sql` and 13 hidden ones.
