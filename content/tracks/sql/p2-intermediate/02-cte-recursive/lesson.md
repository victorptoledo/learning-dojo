# Common Table Expressions, Recursive Queries and Views

Real queries grow: a filter, a grouping, a join to the grouping, a ranking on top. Nested
derived tables turn that into code you read inside out. **Common table expressions** (CTEs)
let you name each step and read top to bottom; their recursive form walks trees and graphs;
and **views** keep a named query in the database for everyone.

## WITH: name the steps

```sql
WITH paid AS (
    SELECT o.id, o.customer_id, sum(i.quantity * i.unit_price) AS total
    FROM orders o
    JOIN order_items i ON i.order_id = o.id
    WHERE o.status = 'paid'
    GROUP BY o.id, o.customer_id
),
per_customer AS (
    SELECT customer_id, count(*) AS orders, sum(total) AS revenue
    FROM paid
    GROUP BY customer_id
)
SELECT c.name, p.orders, p.revenue
FROM per_customer p
JOIN customers c ON c.id = p.customer_id
WHERE p.revenue > (SELECT avg(revenue) FROM per_customer)
ORDER BY p.revenue DESC;
```

Each CTE is visible to the CTEs after it and to the main query, and can be used more than
once (`per_customer` is used twice above). A CTE exists only for the one statement.

**Performance.** Since PostgreSQL 12, a CTE used once and without side effects is **inlined**:
optimised as if you had written a subquery. Add `AS MATERIALIZED` to force computing it once
and storing it (useful when it is expensive and used many times), or `AS NOT MATERIALIZED` to
force inlining. Before version 12 every CTE was an optimisation fence, which is why old advice
says "CTEs are slow". MySQL supports CTEs from 8.0, SQL Server and SQLite for a long time.

PostgreSQL also allows DML in a CTE (`WITH moved AS (DELETE ... RETURNING *) INSERT INTO
archive SELECT * FROM moved`): one statement that moves rows atomically.

## WITH RECURSIVE: trees and graphs

Hierarchies are stored as **adjacency lists**: each row points to its parent.

```text
employees
id | name   | manager_id
 1 | Ines   | NULL        ← the CEO
 2 | Bruno  | 1
 3 | Carla  | 1
 4 | Duarte | 2
 5 | Eva    | 4
```

A recursive CTE has an **anchor** (where to start) and a **recursive member** (how to take
one step), joined by `UNION ALL`:

```sql
WITH RECURSIVE chain AS (
    -- anchor: Bruno himself
    SELECT id, name, manager_id, 0 AS depth, name::text AS path
    FROM employees
    WHERE name = 'Bruno'

    UNION ALL

    -- recursive member: the direct reports of the rows found in the previous step
    SELECT e.id, e.name, e.manager_id, c.depth + 1, c.path || ' > ' || e.name
    FROM employees e
    JOIN chain c ON e.manager_id = c.id
)
SELECT name, depth, path FROM chain ORDER BY path;
-- Bruno  0  Bruno
-- Duarte 1  Bruno > Duarte
-- Eva    2  Bruno > Duarte > Eva
```

How it runs: the anchor fills a working table; the recursive member runs against **only the
rows added in the last step**, adds what it finds, and repeats until a step adds nothing.
Going up (the chain of managers above Eva) is the same query with the join reversed:
`JOIN chain c ON e.id = c.manager_id`.

Watch the types: the anchor decides the column types, so cast the path (`name::text` or
`CAST(name AS varchar(1000))`) or appending to it fails or truncates.

### Cycles

In a graph (flights, friendships, a bad data import) a path can come back to where it started,
and the recursion never ends. Protect it:

```sql
WITH RECURSIVE reach AS (
    SELECT dst AS airport, ARRAY[src, dst] AS visited
    FROM flights WHERE src = 'LIS'
    UNION ALL
    SELECT f.dst, r.visited || f.dst
    FROM flights f JOIN reach r ON f.src = r.airport
    WHERE f.dst <> ALL (r.visited)              -- do not revisit
)
SELECT DISTINCT airport FROM reach;
```

The standard `CYCLE` clause (PostgreSQL 14+) does the bookkeeping:
`... ) CYCLE airport SET is_cycle USING path`. A depth limit (`WHERE depth < 20`) is a
last-resort guard. With `UNION` instead of `UNION ALL`, rows already seen are dropped, which
also ends cycles when the rows carry no path.

### Generating rows

Recursion is also a generator, for example a row for every day so that days without sales
still show up:

```sql
WITH RECURSIVE days AS (
    SELECT DATE '2026-01-01' AS day
    UNION ALL
    SELECT day + 1 FROM days WHERE day < DATE '2026-01-31'
)
SELECT d.day, coalesce(sum(o.total), 0) AS revenue
FROM days d
LEFT JOIN orders o ON o.placed_at::date = d.day
GROUP BY d.day ORDER BY d.day;
```

PostgreSQL has `generate_series(DATE '2026-01-01', DATE '2026-01-31', interval '1 day')` for
this; the recursive form is the portable one.

## Views

A **view** is a named query stored in the database:

```sql
CREATE VIEW paid_orders AS
SELECT o.id, o.customer_id, o.placed_at, sum(i.quantity * i.unit_price) AS total
FROM orders o JOIN order_items i ON i.order_id = o.id
WHERE o.status = 'paid'
GROUP BY o.id;

SELECT customer_id, sum(total) FROM paid_orders GROUP BY customer_id;
```

A view stores **no data**: every query against it runs the underlying query, merged with the
outer one, so it is always current. Views give one definition of "a paid order" for every
report, hide columns from users who should not see them (grant on the view, not the table),
and keep old column names working during a migration.

Simple views (one table, no aggregates) are **updatable**: `UPDATE paid_customers SET ...`
changes the base table. `WITH CHECK OPTION` rejects changes that would make a row vanish from
the view. `CREATE OR REPLACE VIEW` changes the definition; `DROP VIEW` removes it.

## Materialized views

A **materialized view** stores the result, like a table filled by a query:

```sql
CREATE MATERIALIZED VIEW monthly_revenue AS
SELECT date_trunc('month', placed_at) AS month, sum(total) AS revenue
FROM paid_orders GROUP BY 1;

REFRESH MATERIALIZED VIEW monthly_revenue;                 -- recompute (locks readers)
REFRESH MATERIALIZED VIEW CONCURRENTLY monthly_revenue;    -- readers keep reading; needs a unique index
```

Reads are as fast as a table and can be indexed, but the data is as old as the last refresh,
which you schedule yourself. PostgreSQL and Oracle have them; SQL Server has **indexed views**
(kept current automatically, with many restrictions); MySQL and SQLite have none (use a table
filled by a job).

## Summary

- WITH names steps; PostgreSQL 12+ inlines single-use CTEs; MATERIALIZED forces storing one.
- WITH RECURSIVE = anchor UNION ALL recursive member, repeated until no new rows appear.
- Track depth and path in the CTE; guard graphs against cycles with a visited path or CYCLE.
- A view stores the query and is always current; a materialized view stores the result and
  must be refreshed.

## Further reading

- PostgreSQL 17, "WITH Queries (Common Table Expressions)": <https://www.postgresql.org/docs/17/queries-with.html>
- PostgreSQL 17, "CREATE VIEW": <https://www.postgresql.org/docs/17/sql-createview.html>
- PostgreSQL 17, "Materialized Views": <https://www.postgresql.org/docs/17/rules-materializedviews.html>
- Modern SQL, "WITH: organize complex queries": <https://modern-sql.com/feature/with>
- Modern SQL, literate SQL with WITH: <https://modern-sql.com/use-case/literate-sql>
- Modern SQL, which engines support WITH RECURSIVE: <https://modern-sql.com/caniuse/with_recursive_(top_level)>
