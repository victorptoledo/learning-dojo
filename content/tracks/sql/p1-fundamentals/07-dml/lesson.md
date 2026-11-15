# INSERT, UPDATE, DELETE, Upserts and MERGE

Queries read; DML changes. The syntax is short, and the consequences of a missing `WHERE` are
long, so this lesson is as much about habits as statements.

## INSERT

```sql
INSERT INTO customers (email, name, country)
VALUES ('ada@example.com', 'Ada', 'PT');

INSERT INTO customers (email, name, country)          -- several rows in one statement
VALUES ('grace@example.com', 'Grace', 'US'),
       ('linus@example.com', 'Linus', 'FI');

INSERT INTO archived_orders (id, customer_id, total)  -- the rows of a query
SELECT id, customer_id, total
FROM orders
WHERE placed_at < DATE '2025-01-01';
```

Always list the columns: the statement keeps working when a column is added, and generated
columns (`id`, `created_at DEFAULT now()`) fill themselves. Multi-row `VALUES` is much faster
than one statement per row.

## UPDATE and DELETE

```sql
UPDATE products SET price = price * 1.10 WHERE category = 'books';
UPDATE orders SET status = 'shipped', shipped_at = now() WHERE id = 42;
DELETE FROM sessions WHERE expires_at < now();
```

Without `WHERE`, **every row** changes. Habits that save careers:

1. Write the `WHERE` first, as a `SELECT count(*) ... WHERE ...`, and look at the number.
2. Run risky changes inside a transaction and read the reported row count before `COMMIT`:

```sql
BEGIN;
UPDATE products SET price = price * 1.10 WHERE category = 'books';
-- UPDATE 37   (expected about 40? good)
COMMIT;        -- or ROLLBACK;
```

3. In psql, `\set AUTOCOMMIT off` makes every statement wait for an explicit `COMMIT`.

To change rows using **another table**, the syntax differs per engine:

```sql
-- PostgreSQL (UPDATE ... FROM) and SQL Server
UPDATE orders o SET status = 'blocked'
FROM customers c
WHERE c.id = o.customer_id AND c.banned;

-- standard and portable: a subquery
UPDATE orders SET status = 'blocked'
WHERE customer_id IN (SELECT id FROM customers WHERE banned);

-- PostgreSQL: DELETE ... USING;  MySQL: DELETE o FROM orders o JOIN customers c ON ...
DELETE FROM orders o USING customers c
WHERE c.id = o.customer_id AND c.banned;
```

`TRUNCATE` empties a table much faster than `DELETE` (it does not visit rows), but it takes
a strong lock and does not fire row triggers.

## RETURNING: see what you changed

PostgreSQL (and SQLite 3.35+, MariaDB) can return the affected rows:

```sql
INSERT INTO customers (email, name, country) VALUES ('kay@example.com', 'Kay', 'PT')
RETURNING id;                                   -- the generated key, no second query
UPDATE accounts SET balance = balance - 50 WHERE id = 7 RETURNING balance;
DELETE FROM jobs WHERE status = 'done' RETURNING id;
```

SQL Server uses `OUTPUT inserted.id`; MySQL has `LAST_INSERT_ID()`.

## Upsert: insert or update

"Insert this row, or update it if the key already exists" is common (syncs, counters,
caches). Doing it as SELECT-then-INSERT in application code is a race: two sessions both see
"no row" and both insert. Let the database do it in one statement:

```sql
INSERT INTO page_views (page, views) VALUES ('/home', 1)
ON CONFLICT (page) DO UPDATE SET views = page_views.views + EXCLUDED.views;

INSERT INTO tags (name) VALUES ('sql')
ON CONFLICT (name) DO NOTHING;
```

`EXCLUDED` is the row you tried to insert. `ON CONFLICT` needs a unique constraint or index
on the conflict columns. Other engines:

| Engine | Upsert |
| --- | --- |
| PostgreSQL, SQLite | `INSERT ... ON CONFLICT (key) DO UPDATE SET ... = excluded.col` |
| MySQL | `INSERT ... ON DUPLICATE KEY UPDATE views = views + VALUES(views)` (8.0.19+: a row alias) |
| SQL Server, Oracle | `MERGE` |

## MERGE: the standard way

`MERGE` (SQL:2003; PostgreSQL 15+) compares a source with a target and says what to do in
each case:

```sql
MERGE INTO stock AS s
USING counts AS c ON c.sku = s.sku
WHEN MATCHED AND c.quantity = 0 THEN DELETE
WHEN MATCHED THEN UPDATE SET quantity = c.quantity, updated_at = now()
WHEN NOT MATCHED THEN INSERT (sku, quantity, updated_at) VALUES (c.sku, c.quantity, now())
WHEN NOT MATCHED BY SOURCE THEN DELETE;        -- PostgreSQL 17+, SQL Server
```

`MERGE` is not a concurrency-safe upsert in PostgreSQL: two sessions inserting the same new
key can still collide, so for single-row upserts prefer `ON CONFLICT`. PostgreSQL 17 also lets
`MERGE` end with `RETURNING merge_action(), s.*`.

## Summary

- List columns in INSERT; multi-row VALUES and INSERT ... SELECT insert in bulk.
- UPDATE and DELETE without WHERE change every row: count first, run in a transaction.
- Joins in UPDATE/DELETE are dialect-specific; a subquery in WHERE is portable.
- RETURNING gives back generated keys and changed rows.
- Upsert with ON CONFLICT (PostgreSQL, SQLite), ON DUPLICATE KEY (MySQL) or MERGE.

## Further reading

- PostgreSQL 17, "Data Manipulation": <https://www.postgresql.org/docs/17/dml.html>
- PostgreSQL 17, "INSERT" (ON CONFLICT): <https://www.postgresql.org/docs/17/sql-insert.html>
- PostgreSQL 17, "MERGE": <https://www.postgresql.org/docs/17/sql-merge.html>
- SQLite, "UPSERT": <https://www.sqlite.org/lang_upsert.html>
- SQL Server, "MERGE": <https://learn.microsoft.com/en-us/sql/t-sql/statements/merge-transact-sql>
- Modern SQL, faster multi-row inserts: <https://modern-sql.com/use-case/reduce-network-latency-for-insert>
