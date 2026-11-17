Move the tasks into SQLite, with SQL you write.

- The file `db_path` holds a table other tools can read:

```sql
CREATE TABLE tasks (
    id    INTEGER PRIMARY KEY,
    title TEXT    NOT NULL,
    done  INTEGER NOT NULL   -- 0 or 1
);
```

  Create it when it does not exist yet. Every change is committed at once: a second app built
  on the same file sees everything the first one wrote, and so does a plain `sqlite3`
  connection.
- `GET /tasks` gains two optional filters, which can be combined:
  - `?done=true` or `?done=false`;
  - `?q=milk`: tasks whose title contains the text, ignoring case; every character of `q` is
    plain text (`%` and `_` are not wildcards).

  `total` counts the tasks that match. Filter in SQL (`WHERE`), with `?` placeholders for
  the values, never by building SQL with f-strings: a `q` such as `x' OR '1'='1` must find
  nothing.
