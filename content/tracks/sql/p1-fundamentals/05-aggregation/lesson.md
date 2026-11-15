# Aggregation, GROUP BY and HAVING

Reports, dashboards and most "how many" questions collapse many rows into a few. That is
aggregation: functions that take a set of values and return one.

## Aggregate functions

| Function | Returns | NULLs |
| --- | --- | --- |
| `count(*)` | number of rows | counted |
| `count(col)` | number of non-NULL values | skipped |
| `count(DISTINCT col)` | number of different non-NULL values | skipped |
| `sum(col)`, `avg(col)` | total, mean | skipped; NULL when there is no value |
| `min(col)`, `max(col)` | smallest, largest (numbers, text, dates) | skipped |
| `string_agg(col, ', ' ORDER BY col)` | values joined into one text | skipped |
| `array_agg(col)`, `bool_and`, `bool_or` | PostgreSQL extras | |

Without `GROUP BY`, an aggregate turns the whole table into **one row**:

```sql
SELECT count(*) AS orders, sum(total) AS revenue, avg(total) AS average
FROM orders
WHERE status = 'paid';
```

Text aggregation is `string_agg` in PostgreSQL and SQL Server, `group_concat` in MySQL and
SQLite, and the standard `listagg` in Oracle and Db2.

`avg` of an integer column returns a `numeric` in PostgreSQL but an integer in SQL Server,
which truncates: `avg(1, 2)` is `1`. Cast first when you need decimals there.

## GROUP BY: one row per group

```sql
SELECT customer_id, count(*) AS orders, sum(total) AS revenue
FROM orders
GROUP BY customer_id;
```

Every distinct value of the grouping columns makes one group, and each aggregate runs once
per group. The rule that follows: **every column in SELECT must either be grouped or be inside
an aggregate**.

```sql
SELECT customer_id, placed_at, count(*) FROM orders GROUP BY customer_id;   -- ERROR
```

A customer has many `placed_at` values; which one would the row show? PostgreSQL rejects it.
(Old MySQL picked one at random; `ONLY_FULL_GROUP_BY`, the default since 5.7, rejects it too.)
One exception, from the standard: if you group by a table's primary key, you may select
the other columns of that table, because they are fixed per key:

```sql
SELECT c.id, c.name, count(o.id)
FROM customers c LEFT JOIN orders o ON o.customer_id = c.id
GROUP BY c.id;        -- c.name allowed: it depends on c.id
```

Group by expressions too:

```sql
SELECT date_trunc('month', placed_at) AS month, sum(total)
FROM orders
GROUP BY date_trunc('month', placed_at)      -- PostgreSQL also accepts GROUP BY month or GROUP BY 1
ORDER BY month;
```

## HAVING filters groups

`WHERE` runs before grouping and sees rows; `HAVING` runs after and sees groups:

```sql
SELECT customer_id, count(*) AS orders
FROM orders
WHERE placed_at >= DATE '2026-01-01'      -- rows: orders of 2026 only
GROUP BY customer_id
HAVING count(*) >= 5;                     -- groups: customers with 5 or more
```

A condition that does not use an aggregate belongs in `WHERE`: it removes rows before the
expensive grouping.

## Conditional aggregation

Several counts in one pass, instead of one query per status:

```sql
SELECT date_trunc('month', placed_at) AS month,
       count(*)                                    AS orders,
       count(*) FILTER (WHERE status = 'cancelled') AS cancelled,
       sum(total) FILTER (WHERE status <> 'cancelled') AS revenue
FROM orders
GROUP BY 1
ORDER BY 1;
```

`FILTER` is standard SQL, supported by PostgreSQL and SQLite. Everywhere else, use `CASE`
inside the aggregate; it means the same:

```sql
sum(CASE WHEN status = 'cancelled' THEN 1 ELSE 0 END)       AS cancelled,
sum(CASE WHEN status <> 'cancelled' THEN total END)         AS revenue   -- no ELSE: NULL, skipped
```

This is also how you **pivot** rows into columns: one conditional aggregate per column.

## Subtotals: ROLLUP, CUBE and GROUPING SETS

```sql
SELECT country, category, sum(total)
FROM sales
GROUP BY ROLLUP (country, category);
-- (country, category) rows, then a subtotal per country (category NULL),
-- then a grand total (both NULL)
```

`GROUPING SETS ((country), (category), ())` lists the groupings you want; `CUBE (a, b)` is
every combination. `grouping(category)` returns 1 on the rows where `category` was rolled
up, which tells a subtotal's NULL from a real NULL. MySQL writes `GROUP BY a, b WITH ROLLUP`.

## Summary

- Aggregates collapse rows; without GROUP BY the whole input is one group.
- Every selected column is grouped or aggregated (or depends on a grouped primary key).
- WHERE filters rows before grouping, HAVING filters groups after.
- `count(*) FILTER (WHERE ...)` or `sum(CASE ...)` computes many measures in one pass.
- ROLLUP, CUBE and GROUPING SETS add subtotals; `grouping()` tells which rows they are.

## Further reading

- PostgreSQL 17, "Aggregate Functions": <https://www.postgresql.org/docs/17/functions-aggregate.html>
- PostgreSQL 17, "GROUP BY and HAVING" (in Table Expressions): <https://www.postgresql.org/docs/17/queries-table-expressions.html>
- Modern SQL, "FILTER": <https://modern-sql.com/feature/filter>
- Modern SQL, pivoting with conditional aggregates: <https://modern-sql.com/use-case/pivot>
- Modern SQL, "LISTAGG": <https://modern-sql.com/feature/listagg>
- Use The Index, Luke, "Sorting and Grouping": <https://use-the-index-luke.com/sql/sorting-grouping>
