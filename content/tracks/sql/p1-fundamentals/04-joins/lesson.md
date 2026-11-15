# Joins of Every Kind

Normalised data is spread over tables, so most real queries join. A join is a product of
two tables (every row with every row) filtered by a condition; the kinds differ in what
they do with rows that find **no match**.

We will use:

```text
customers                 orders
id | name   | country     id | customer_id | total | status
 1 | Ada    | PT           10 | 1           | 30.00 | paid
 2 | Grace  | US           11 | 1           | 12.50 | cancelled
 3 | Linus  | FI           12 | 2           | 99.00 | paid
                           13 | NULL        |  5.00 | paid      -- a guest order
```

## INNER JOIN: only matching pairs

```sql
SELECT c.name, o.id, o.total
FROM customers AS c
JOIN orders AS o ON o.customer_id = c.id;     -- JOIN means INNER JOIN
```

Ada appears twice (two orders), Grace once. Linus (no orders) and order 13 (no customer)
disappear. Use **table aliases** (`c`, `o`) and qualify every column once you join.

The old comma syntax `FROM customers c, orders o WHERE o.customer_id = c.id` is an inner join
too. Avoid it: forget the condition and you get a silent cross join.

## Outer joins: keep the rows without a match

| Join | Keeps | Missing side becomes |
| --- | --- | --- |
| `LEFT JOIN` | every row of the left table | NULL columns from the right |
| `RIGHT JOIN` | every row of the right table | NULL columns from the left |
| `FULL JOIN` | every row of both | NULLs on whichever side is missing |

```sql
SELECT c.name, o.id
FROM customers c
LEFT JOIN orders o ON o.customer_id = c.id;
-- Ada 10, Ada 11, Grace 12, Linus NULL
```

`RIGHT JOIN` is a `LEFT JOIN` with the tables swapped; most teams write only `LEFT`. MySQL
has no `FULL JOIN` (emulate it with a `UNION` of a left and a right join); SQLite has it since
3.39.

### ON versus WHERE in an outer join

The `ON` condition decides which rows match; `WHERE` filters the **result**. With an inner
join they are interchangeable. With an outer join they are not:

```sql
-- every customer with their paid orders (Linus kept, with NULL)
SELECT c.name, o.id
FROM customers c
LEFT JOIN orders o ON o.customer_id = c.id AND o.status = 'paid';

-- BUG: Linus disappears. His row has o.status NULL, and NULL = 'paid' is not TRUE
SELECT c.name, o.id
FROM customers c
LEFT JOIN orders o ON o.customer_id = c.id
WHERE o.status = 'paid';
```

A `WHERE` on a column of the optional table turns the left join into an inner join. Put
conditions about the optional table in `ON`; conditions about the kept table in `WHERE`.

## Counting with outer joins

```sql
SELECT c.name, count(o.id) AS orders     -- count(o.id): NULLs are not counted
FROM customers c
LEFT JOIN orders o ON o.customer_id = c.id
GROUP BY c.id, c.name;
-- Ada 2, Grace 1, Linus 0
```

`count(*)` would give Linus 1: his row exists, it just has NULLs.

## CROSS JOIN and self joins

`CROSS JOIN` pairs every row with every row: 3 sizes × 4 colours = 12 variants. Useful for
generating combinations, a disaster when it happens by accident.

A **self join** joins a table to itself with two aliases:

```sql
-- employees and their managers (manager_id references employees.id)
SELECT e.name AS employee, m.name AS manager
FROM employees e
LEFT JOIN employees m ON m.id = e.manager_id;
```

## Semi and anti joins: "has one" and "has none"

To ask whether a match **exists**, without repeating the row per match, use `EXISTS`:

```sql
-- customers with at least one paid order (each customer once)
SELECT c.name FROM customers c
WHERE EXISTS (SELECT 1 FROM orders o WHERE o.customer_id = c.id AND o.status = 'paid');

-- customers with no order at all
SELECT c.name FROM customers c
WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.customer_id = c.id);

-- the same anti join with an outer join
SELECT c.name FROM customers c
LEFT JOIN orders o ON o.customer_id = c.id
WHERE o.id IS NULL;
```

A plain join followed by `DISTINCT` gives the same names as the semi join, but it builds all
the pairs first and hides the intent.

## Fan-out: the silent multiplier

Joining one row to **two** independent child tables multiplies them:

```sql
-- BUG: Ada has 2 orders and 3 support tickets, so each order appears 3 times
SELECT c.name, sum(o.total)
FROM customers c
JOIN orders o ON o.customer_id = c.id
JOIN tickets t ON t.customer_id = c.id
GROUP BY c.name;
```

Aggregate each child **before** joining:

```sql
SELECT c.name, o.revenue, t.tickets
FROM customers c
LEFT JOIN (SELECT customer_id, sum(total) AS revenue FROM orders GROUP BY customer_id) o
       ON o.customer_id = c.id
LEFT JOIN (SELECT customer_id, count(*) AS tickets FROM tickets GROUP BY customer_id) t
       ON t.customer_id = c.id;
```

When a total looks too big, count the rows per key before and after each join.

## USING, NATURAL and LATERAL

- `JOIN orders USING (customer_id)` joins on equally named columns and shows the column
  once. `NATURAL JOIN` joins on **every** equally named column; one new column named
  `updated_at` silently changes it. Avoid `NATURAL`.
- `LATERAL` lets a subquery in `FROM` use columns of the tables before it, like a loop per
  row. "The last 3 orders of each customer":

```sql
SELECT c.name, last.id, last.total
FROM customers c
CROSS JOIN LATERAL (
    SELECT o.id, o.total FROM orders o
    WHERE o.customer_id = c.id
    ORDER BY o.id DESC
    FETCH FIRST 3 ROWS ONLY
) AS last;
```

`LATERAL` is standard and works in PostgreSQL, MySQL 8.0.14+ and Oracle; SQL Server spells it
`CROSS APPLY` / `OUTER APPLY`.

## How engines run joins

You choose the result; the planner chooses the algorithm: **nested loop** (for each outer
row, look up matches, best with an index and few rows), **hash join** (build a hash table of
the smaller side, good for large unsorted inputs) or **merge join** (walk two sorted inputs).
Phase 3 shows how to see which one it picked.

## Summary

- INNER keeps matching pairs; LEFT, RIGHT and FULL also keep unmatched rows with NULLs.
- Conditions on the optional table go in ON; in WHERE they undo the outer join.
- Count a column of the optional table, not `*`, to get zeros.
- EXISTS and NOT EXISTS are semi and anti joins: "has one", "has none".
- Joining two child tables multiplies rows: aggregate each first.

## Further reading

- PostgreSQL 17 tutorial, "Joins Between Tables": <https://www.postgresql.org/docs/17/tutorial-join.html>
- PostgreSQL 17, "Table Expressions" (joined tables, LATERAL): <https://www.postgresql.org/docs/17/queries-table-expressions.html>
- Use The Index, Luke, "The Join Operation": <https://use-the-index-luke.com/sql/join>
- roadmap.sh SQL topics (joins): <https://github.com/nilbuild/developer-roadmap/tree/master/roadmaps/sql/content>
