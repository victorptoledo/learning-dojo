# Lock Detective

`fixture/01_bank.sql` creates one table, `accounts (id, owner, balance)`, with 1,000 rows. The
hidden tests open **real other sessions** (with the `dblink` extension) that hold and wait for
locks while your queries run. Every session sets its `application_name`, so you can tell them
apart: `billing`, `checkout`, `migration`, `report`, ...

| File | Kind | Task |
| --- | --- | --- |
| `blockers.sql` | one query | Every session that is blocked, once for **each** session that blocks it: `waiting` (its `application_name`), `blocked_by` (the blocker's `application_name`), `blocker_state` and `blocker_query` (the blocker's `state` and `query` in `pg_stat_activity`), by `waiting` then `blocked_by`. No row when nobody waits. |
| `root_blockers.sql` | one query | End the **root blockers**: the sessions that block at least one other session and are not blocked themselves. Return `application_name` and `terminated` (what the function that ended it returned) for each. Never end your own session, and leave every other session alone. |
| `migration.sql` | statements | Add a column `currency char(3) NOT NULL` with default `'EUR'`, and a check that `balance >= 0`. The table is busy: if the migration cannot get its lock within **2 seconds**, it must fail (SQLSTATE `55P03`) instead of queueing the application behind it. Validate existing rows in a separate step from adding the check. |
| `singleton.sql` | one query | Several instances start the nightly report at once; only one may run it. Return `got_lock`: true when this instance took the advisory lock with key `4242`, false when another one holds it. It must **never wait**, and the lock must be released at the end of the **transaction**. |

The scenario of the first two tests, in time order:

```text
billing    BEGIN; UPDATE accounts ... WHERE id = 1;   then idle, transaction still open
checkout   UPDATE accounts ... WHERE id = 1           waits for billing's row lock
migration  ALTER TABLE accounts ADD COLUMN ...        waits for billing and checkout
report     SELECT count(*) FROM accounts              waits behind the migration in the queue
```

Try it with `docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play`. To recreate
the scenario yourself, open more sessions in the same container from other terminals:
`docker exec -it <container> psql -d dojo` (find the container with `docker ps`), and set
`SET application_name = 'billing';` in each one.

There are 14 tests: 1 in `tests/visible.sql` and 13 hidden ones. A test that waits too long is
stopped after 15 seconds.
