# SQL from Applications: Parameters, N+1 and Pagination

SQL written in psql runs once. SQL in an application runs thousands of times a second, with
input from strangers, through a driver, a pool and often an ORM. Four habits separate code that
scales from code that leaks data or melts the database.

## 1. Bind parameters, always

Never build SQL by gluing input into a string:

```python
# WRONG: input "x' OR '1'='1" returns every user; "x'; DROP TABLE users; --" is worse
cursor.execute("SELECT id FROM users WHERE email = '" + email + "'")

# RIGHT: the value travels separately from the SQL text
cursor.execute("SELECT id FROM users WHERE email = %s", (email,))
```

With a **bind parameter** the driver sends the statement and the values apart (in PostgreSQL's
extended protocol, `Parse` with `$1` then `Bind` with the value), so a value can never become
SQL. That is the only reliable defence against **SQL injection**, which keeps appearing in the
OWASP Top 10. Escaping by hand is not.

Placeholders differ by driver: `$1` (PostgreSQL native, node-postgres, pgx), `?` (JDBC, SQLite,
MySQL), `%s` (psycopg), `@name` (ADO.NET, SQL Server), `:name` (many ORMs). Notes:

- A `LIKE` pattern is a parameter too: `WHERE name LIKE $1` with `'%' || escaped || '%'`, after
  escaping `%` and `_` in the input if they should be literal.
- A list is one array parameter in PostgreSQL: `WHERE id = ANY($1)` with an array, instead of
  building `IN (?, ?, ?, ...)` of varying length.
- **Identifiers** (table or column names, `ASC`/`DESC`) cannot be parameters. Choose them from
  an allow-list in code (`{"name": "name", "date": "placed_at"}[sort]`), never from raw input.
  In PostgreSQL functions, `format('%I', name)` quotes an identifier.
- Prepared statements also save planning time for hot queries.

## 2. Kill N+1 queries

```text
SELECT id, customer_id FROM orders ORDER BY placed_at DESC LIMIT 20;   -- 1 query
SELECT name FROM customers WHERE id = 7;                               -- + 20 queries,
SELECT name FROM customers WHERE id = 3;                               --   one per order
...
```

Each round trip costs a network hop and a statement, so 1 + N queries turn a 2 ms page into a
200 ms page, and they hide in loops and in ORMs' lazy loading (`order.customer.name` inside a
template). Fixes:

```sql
-- one query with a join
SELECT o.id, o.placed_at, c.name
FROM orders o JOIN customers c ON c.id = o.customer_id
ORDER BY o.placed_at DESC LIMIT 20;

-- or one query per relation, batched: 1 + 1 instead of 1 + N
SELECT order_id, product_id, quantity FROM order_items WHERE order_id = ANY($1);   -- $1 = the 20 ids
```

ORMs call this eager loading: `JOIN FETCH` or entity graphs in JPA/Hibernate, `Include` in
Entity Framework, `select_related`/`prefetch_related` in Django, `include` in Prisma,
`Preload` in GORM. To find N+1 in production, count queries per request: ORM SQL logging,
`pg_stat_statements` (a query with huge `calls` and tiny time each), or APM traces.

The same applies to writes: insert many rows with one multi-row `INSERT`, `COPY`, or
`INSERT ... SELECT FROM unnest($1, $2)`, not a loop of single inserts.

## 3. Paginate with keysets, not OFFSET

```sql
-- page 5000 with OFFSET: reads and discards 99 980 rows; rows shift when new ones arrive
SELECT id, placed_at, total FROM orders
ORDER BY placed_at DESC, id DESC
LIMIT 20 OFFSET 99980;

-- keyset ("seek"): continue after the last row the client saw
SELECT id, placed_at, total FROM orders
WHERE (placed_at, id) < ($1, $2)                  -- the last row of the previous page
ORDER BY placed_at DESC, id DESC
LIMIT 20;
```

With an index on `(placed_at, id)`, every page costs the same: the index seeks to the position
and reads 20 entries. The order must be **unique** (add `id` as a tie-breaker), and the row-value
comparison `(a, b) < (x, y)` must match the `ORDER BY` direction. The API returns an opaque
cursor (the last `placed_at` and `id`, encoded) instead of a page number. The trade-off: no
"jump to page 37"; for user interfaces that is rarely needed. Row values work in PostgreSQL,
MySQL and SQLite; in SQL Server write `placed_at < $1 OR (placed_at = $1 AND id < $2)`.

## 4. Connections, transactions and retries

- **Pool connections.** Opening a PostgreSQL connection forks a process and takes milliseconds;
  each one uses memory. Use the driver's pool (HikariCP, pgx pool, SQLAlchemy pool) or PgBouncer,
  sized small: often 2 to 4 per CPU core of the database server is plenty.
- **Transactions in code.** Begin, do the work, commit, and roll back on any error, with the
  language's structure (`try/finally`, `using`, `defer tx.Rollback()`, `@Transactional`). Keep
  them short; never call a remote service inside one.
- **Retry what the database asks you to.** SQLSTATE `40001` (serialization failure) and `40P01`
  (deadlock) mean "run the whole transaction again", with a small random backoff. Do not retry
  constraint violations (`23xxx`): they are answers.
- **Timeouts.** Set `statement_timeout` (and `lock_timeout`) per role or per transaction, so a bad
  query fails instead of holding a connection forever.

## ORMs, query builders and migrations

ORMs map rows to objects and write most simple SQL for you. Use them, but keep control: turn on
SQL logging in development, check the queries of every new endpoint, write the complicated
reports as SQL (most ORMs accept native queries), and remember that the ORM's defaults (lazy
loading, `SELECT *`, one `UPDATE` per changed object) are the anti-patterns of the last lesson.
Query builders (jOOQ, Knex, SQLAlchemy Core, sqlc which generates code from SQL) sit in
between: you write SQL-shaped code with types and parameters.

Schema changes belong in **versioned migrations** (Flyway, Liquibase, Alembic, Prisma Migrate,
EF Core migrations, golang-migrate), reviewed and applied by the deploy pipeline, never by hand.
Make them compatible with the running code: add a column, deploy code that writes it, backfill
in batches, then add the constraint (`NOT VALID` then `VALIDATE`) and only later drop what is no
longer used ("expand and contract").

## Summary

- Bind every value; allow-list identifiers. Concatenated SQL is an injection waiting to happen.
- N+1: one query per row of a list. Join, or batch with `= ANY($1)`; count queries per request.
- Keyset pagination with a unique order and a matching index; OFFSET only for shallow pages.
- Pool connections, keep transactions short, retry 40001 and 40P01, set statement timeouts.
- Read the SQL your ORM writes; ship schema changes as expand-and-contract migrations.

## Further reading

- OWASP, "SQL Injection Prevention Cheat Sheet": <https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html>
- PostgreSQL 17, "PREPARE": <https://www.postgresql.org/docs/17/sql-prepare.html>
- Use The Index, Luke, "OFFSET is bad for skipping previous rows": <https://use-the-index-luke.com/sql/partial-results/fetch-next-page>
- Use The Index, Luke, "We need tool support for keyset pagination": <https://use-the-index-luke.com/no-offset>
- Modern SQL, unit tests on transient data: <https://modern-sql.com/use-case/unit-tests-on-transient-data>
- roadmap.sh SQL topics (dynamic SQL, query optimization): <https://github.com/nilbuild/developer-roadmap/tree/master/roadmaps/sql/content>
