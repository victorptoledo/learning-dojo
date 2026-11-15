# Subqueries and Set Operations

A query returns a table, so a query can go anywhere SQL expects a value or a table. That is
a **subquery**. Set operations then combine whole results the way the algebra combines
relations.

## Three shapes of subquery

| Shape | Returns | Used as | Example |
| --- | --- | --- | --- |
| scalar | one row, one column | a value | `(SELECT max(price) FROM products)` |
| row / list | one column, many rows | the right side of `IN`, `ANY`, `ALL` | `WHERE id IN (SELECT ...)` |
| table | rows and columns | a table in `FROM` (a **derived table**) | `FROM (SELECT ...) AS t` |

```sql
-- scalar: each product with how far it is from the most expensive
SELECT name, price, (SELECT max(price) FROM products) - price AS below_top
FROM products;

-- list: customers who placed an order in 2026
SELECT name FROM customers
WHERE id IN (SELECT customer_id FROM orders WHERE placed_at >= DATE '2026-01-01');

-- derived table: revenue per customer, then only the big ones
SELECT t.customer_id, t.revenue
FROM (SELECT customer_id, sum(total) AS revenue FROM orders GROUP BY customer_id) AS t
WHERE t.revenue > 1000;
```

A scalar subquery that returns **more than one row** is an error at run time; one that
returns **no row** gives NULL. A derived table needs an alias (`AS t`) in PostgreSQL before
16, in MySQL and in SQL Server.

## Correlated subqueries

A subquery that uses a column of the outer query is **correlated**. Logically it runs once
per outer row:

```sql
-- employees who earn more than the average of their own department
SELECT e.name, e.salary
FROM employees e
WHERE e.salary > (SELECT avg(x.salary)
                  FROM employees x
                  WHERE x.department_id = e.department_id);
```

"Once per row" is the meaning, not necessarily the execution: planners turn many correlated
subqueries into joins. When they cannot (a scalar subquery in the SELECT list over a big
table, for example), it really runs per row, and an equivalent join with a grouped derived
table is much faster:

```sql
SELECT e.name, e.salary
FROM employees e
JOIN (SELECT department_id, avg(salary) AS avg_salary
      FROM employees GROUP BY department_id) d
  ON d.department_id = e.department_id
WHERE e.salary > d.avg_salary;
```

Phase 2's window functions give a third way: `avg(salary) OVER (PARTITION BY department_id)`.

## IN, EXISTS, ANY and ALL

| Predicate | TRUE when | With NULLs |
| --- | --- | --- |
| `x IN (subquery)` | x equals some value | a NULL in the list never makes it TRUE |
| `x NOT IN (subquery)` | x differs from every value | **any NULL makes it never TRUE** |
| `EXISTS (subquery)` | the subquery returns at least one row | ignores what the rows contain |
| `x > ANY (subquery)` | x is greater than at least one value | `= ANY` is the same as `IN` |
| `x > ALL (subquery)` | x is greater than every value | TRUE for an empty subquery |

`EXISTS` only asks "is there a row?", so `SELECT 1` or `SELECT *` inside it are the same.
Prefer `EXISTS`/`NOT EXISTS` for "has one"/"has none" checks; `IN` reads well for short
lists and non-correlated lookups. Modern planners run `IN` and `EXISTS` as the same semi join;
`NOT IN` cannot be turned into an anti join because of the NULL rule, which also makes it slow.

Row values compare several columns at once: `WHERE (customer_id, product_id) IN (SELECT ...)`
and `WHERE (year, month) > (2025, 12)` work in PostgreSQL, MySQL and SQLite.

## Set operations

Two queries with the **same number of columns** and compatible types can be combined:

| Operation | Result | Duplicates |
| --- | --- | --- |
| `A UNION B` | rows in A or B | removed |
| `A UNION ALL B` | all rows of A, then all of B | kept |
| `A INTERSECT B` | rows in both | removed (`INTERSECT ALL` keeps them) |
| `A EXCEPT B` | rows of A not in B | removed (`EXCEPT ALL` keeps them) |

```sql
-- every email we know, once
SELECT email FROM customers
UNION
SELECT email FROM newsletter_subscribers;

-- products sold in 2025 but not in 2026
SELECT product_id FROM order_items i JOIN orders o ON o.id = i.order_id
WHERE o.placed_at >= DATE '2025-01-01' AND o.placed_at < DATE '2026-01-01'
EXCEPT
SELECT product_id FROM order_items i JOIN orders o ON o.id = i.order_id
WHERE o.placed_at >= DATE '2026-01-01';
```

Rules:

- Columns are matched **by position**, not by name; the names come from the first query.
- `ORDER BY` and `LIMIT` apply to the whole result and go at the end. To sort one part, put
  it in parentheses or a derived table.
- Set operations compare whole rows and treat two NULLs as equal (unlike `=`).
- `UNION` must find duplicates (a sort or hash). When the parts cannot overlap, or you want
  the duplicates, write `UNION ALL`: it just appends.
- Oracle calls `EXCEPT` `MINUS`; MySQL has `INTERSECT` and `EXCEPT` only since 8.0.31.

## Summary

- Subqueries return a value, a list or a table; derived tables need an alias in many engines.
- A correlated subquery refers to the outer row; rewrite it as a grouped join when it is slow.
- EXISTS is the clear semi join; NOT IN breaks on NULLs; ALL is true for an empty list.
- UNION, INTERSECT and EXCEPT match columns by position and remove duplicates; ALL keeps them.
- Prefer UNION ALL when duplicates are impossible or wanted.

## Further reading

- PostgreSQL 17, "Subquery Expressions": <https://www.postgresql.org/docs/17/functions-subquery.html>
- PostgreSQL 17, "Combining Queries (UNION, INTERSECT, EXCEPT)": <https://www.postgresql.org/docs/17/queries-union.html>
- PostgreSQL 17, "Value Expressions" (scalar subqueries, row constructors): <https://www.postgresql.org/docs/17/sql-expressions.html>
- roadmap.sh SQL topics (subqueries, correlated subqueries): <https://github.com/nilbuild/developer-roadmap/tree/master/roadmaps/sql/content>
