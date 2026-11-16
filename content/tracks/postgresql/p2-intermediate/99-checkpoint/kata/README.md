# Checkpoint 2: Help Desk

A help desk keeps its tickets in PostgreSQL. `fixture/01_helpdesk.sql` creates:

```text
tickets   (id, title, body, status: open | closed, priority, metadata jsonb, created_at, updated_at)
audit_log (id, at, actor DEFAULT current_user, op, ticket_id, changes jsonb)
```

`metadata` looks like `{"customer": {"plan": "pro"}, "tags": ["auth", "urgent"], "browser": "Firefox"}`;
plans are `free`, `pro` and `enterprise`. Besides 12 interesting tickets there are 20,000
routine ones, so a missing index shows.

| File | Kind | Task |
| --- | --- | --- |
| `search.sql` | statements | Add a column `doc`: a **stored generated** `tsvector` of the title (weight `A`) and the body (weight `B`) with the `english` configuration. Index it so that `doc @@ ...` searches use an index, and index `metadata` so that containment filters (`metadata @> '{...}'`) use one too. |
| `find_tickets.sql` | one query | The search box: tickets matching what a user typed, `login error` (understood as a web search), that are **open** and belong to **pro** customers, best `ts_rank` first, then by `id`, at most 5. Return `id`, `title`. The tests run your `search.sql` first, so you can use `doc`. |
| `audit.sql` | statements | Triggers on `tickets`. (1) Every `UPDATE` sets `updated_at` to the transaction's time, whoever runs it. (2) Every `UPDATE` that really changes something writes **one** row to `audit_log` with `op = 'UPDATE'` and `changes` holding only the changed columns, each as `{"old": ..., "new": ...}`: `{"status": {"old": "open", "new": "closed"}}`. `updated_at` and `doc` are not changes; an update that changes nothing else writes nothing. (3) Every `DELETE` writes `op = 'DELETE'` with `changes = {"deleted": <the whole old row, without doc>}`. |
| `functions.sql` | statements | A function `open_tickets_for(p_plan text)` returning a table `(id bigint, title text, priority integer)`: the open tickets tagged `urgent` of customers on that plan, highest priority first, then by `id`. An unknown plan raises an error with SQLSTATE `22023` (`invalid_parameter_value`). Give it the right volatility. |

Hints from the lessons: `to_jsonb(NEW)` turns a row into a document, `jsonb_each` turns a
document into `(key, value)` rows, `jsonb_object_agg` turns them back, and the `-` operator
removes a key. `TG_OP` says which operation fired the trigger.

Try it with `docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play`. Run your files
with `\i` inside `BEGIN;` ... `ROLLBACK;` to try them as often as you like.

There are 18 tests: 1 in `tests/visible.sql` and 17 hidden ones.
