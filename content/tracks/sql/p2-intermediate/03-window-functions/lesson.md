# Window Functions: OVER, PARTITION BY, ROW_NUMBER and RANK

`GROUP BY` answers "the total per customer" and throws the rows away. Often you want both:
every order **and** the customer's total, every day **and** the running sum, every player
**and** their rank. Window functions compute over a set of related rows, the **window**,
and keep every row.

## OVER turns an aggregate into a window function

```sql
SELECT o.id, o.customer_id, o.total,
       sum(o.total) OVER (PARTITION BY o.customer_id) AS customer_total,
       o.total / sum(o.total) OVER (PARTITION BY o.customer_id) AS share
FROM orders o;
```

| Part of `OVER (...)` | Does | If missing |
| --- | --- | --- |
| `PARTITION BY` | splits rows into independent groups | one window: all rows |
| `ORDER BY` | orders rows inside each partition | no order: the window is the whole partition |
| frame (`ROWS BETWEEN ...`) | which rows around the current one count | see "Frames" below |

Window functions run in the `SELECT` step, after `WHERE`, `GROUP BY` and `HAVING`. Two
consequences:

- You **cannot** use them in `WHERE` or `HAVING`. Filter on them in an outer query or CTE.
- They can run **on top of** a grouping: `rank() OVER (ORDER BY sum(total) DESC)` ranks groups.

A name keeps long queries readable:
`... OVER w ... WINDOW w AS (PARTITION BY store_id ORDER BY day)`.

## Ranking

```sql
SELECT name, department_id, salary,
       row_number() OVER w AS row_number,
       rank()       OVER w AS rank,
       dense_rank() OVER w AS dense_rank
FROM employees
WINDOW w AS (PARTITION BY department_id ORDER BY salary DESC);
```

| salary | `row_number` | `rank` | `dense_rank` |
| --- | --- | --- | --- |
| 9000 | 1 | 1 | 1 |
| 8000 | 2 | 2 | 2 |
| 8000 | 3 | 2 | 2 |
| 7000 | 4 | **4** | **3** |

`row_number` gives every row a different number (ties in an arbitrary order, so add a
tie-breaker to `ORDER BY`); `rank` gives ties the same number and leaves gaps; `dense_rank`
leaves no gaps. Also `ntile(4)` (quartiles), `percent_rank()`, `cume_dist()`.

### Top-N per group

The classic question "the 2 best-paid employees of each department":

```sql
WITH ranked AS (
    SELECT name, department_id, salary,
           row_number() OVER (PARTITION BY department_id ORDER BY salary DESC, id) AS rn
    FROM employees
)
SELECT name, department_id, salary
FROM ranked
WHERE rn <= 2;
```

Use `rank` or `dense_rank` instead when ties at the boundary should all be included.
PostgreSQL's `DISTINCT ON (department_id) ... ORDER BY department_id, salary DESC` is a short
form for top-1 per group; `LATERAL` with `FETCH FIRST` is another way (Phase 1, joins).

## Comparing with other rows: LAG, LEAD, FIRST_VALUE

```sql
SELECT day, revenue,
       lag(revenue)  OVER (ORDER BY day)               AS previous_day,
       revenue - lag(revenue) OVER (ORDER BY day)      AS change,
       lead(revenue) OVER (ORDER BY day)               AS next_day,
       lag(revenue, 7, 0) OVER (ORDER BY day)          AS same_day_last_week   -- offset 7, default 0
FROM daily_sales;
```

`first_value(x)`, `last_value(x)` and `nth_value(x, n)` read a row of the frame. Gaps and
islands problems (streaks of consecutive days, sessions of activity) start from `lag`: a new
island begins where the difference to the previous row is larger than one step.

## Running totals and frames

With `ORDER BY` in `OVER`, an aggregate becomes **cumulative**:

```sql
SELECT day, revenue,
       sum(revenue) OVER (ORDER BY day) AS running_total,
       sum(revenue) OVER (PARTITION BY date_trunc('month', day) ORDER BY day) AS month_to_date
FROM daily_sales;
```

The **frame** says which rows of the partition the function sees for the current row:

```sql
avg(revenue) OVER (ORDER BY day ROWS BETWEEN 6 PRECEDING AND CURRENT ROW)   -- 7-row moving average
sum(revenue) OVER (ORDER BY day ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)
avg(revenue) OVER (ORDER BY day RANGE BETWEEN INTERVAL '6 days' PRECEDING AND CURRENT ROW)  -- 7 calendar days
```

| Frame unit | Counts | Ties (equal ORDER BY values) |
| --- | --- | --- |
| `ROWS` | physical rows | each row separate |
| `RANGE` | values of the ORDER BY column | peers are included together |
| `GROUPS` | groups of peers | (PostgreSQL 11+) |

The default frame with `ORDER BY` is `RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW`:
rows with the **same** `ORDER BY` value are included together, so two orders on the same day
get the same running total. Write `ROWS` when you mean one row at a time. And beware
`last_value`: with the default frame it returns the current row; use
`ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING`.

`ROWS 6 PRECEDING` counts rows, so a missing day silently widens the window to more than a
week. `RANGE` with an interval (PostgreSQL 11+, Oracle; not SQL Server) counts calendar days.

## Support

Window functions are standard (SQL:2003) and work in PostgreSQL, SQL Server, Oracle, MySQL 8.0+
and SQLite 3.25+. `GROUPS` frames and `RANGE` with intervals are the parts that vary.

## Summary

- `OVER` computes over a window of rows and keeps every row; GROUP BY collapses them.
- PARTITION BY splits, ORDER BY orders, the frame picks the neighbours.
- row_number is unique, rank leaves gaps after ties, dense_rank does not.
- Filter on a window function in an outer query or CTE: top-N per group.
- The default frame with ORDER BY is RANGE ... CURRENT ROW, which groups ties; say ROWS when
  you mean rows.

## Further reading

- PostgreSQL 17 tutorial, "Window Functions": <https://www.postgresql.org/docs/17/tutorial-window.html>
- PostgreSQL 17, "Window Functions" (reference): <https://www.postgresql.org/docs/17/functions-window.html>
- PostgreSQL 17, window function calls and frames: <https://www.postgresql.org/docs/17/sql-expressions.html>
- Modern SQL, "Do identical OVER clauses always produce identical row orders?": <https://modern-sql.com/blog/2026-03/order-equivalence-over-clause>
- Use The Index, Luke, "Sorting and Grouping" (pipelined top-N): <https://use-the-index-luke.com/sql/sorting-grouping>
