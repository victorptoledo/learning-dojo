# Common SQL Anti-Patterns

Most SQL that fails in production is not exotic. It is a handful of patterns that look right,
pass the tests on small data, and then return wrong numbers or take minutes. Learn to see
them in review; every one has a known fix.

## Wrong results

### NOT IN over a column that can be NULL

```sql
SELECT name FROM customers WHERE id NOT IN (SELECT referred_by FROM customers);  -- no rows if any NULL
```

One NULL in the list makes `NOT IN` UNKNOWN for every row (Phase 1). **Fix:** `NOT EXISTS`, or
filter the NULLs out of the subquery.

### Fan-out: joining two child tables, then summing

```sql
SELECT c.name, sum(o.total), count(t.id)
FROM customers c JOIN orders o ON o.customer_id = c.id JOIN tickets t ON t.customer_id = c.id
GROUP BY c.name;   -- sums multiplied by the number of tickets, counts by the number of orders
```

**Fix:** aggregate each child in its own subquery or CTE, then join the results (or use
`LATERAL`). A number that is "a bit too high" is the symptom.

### DISTINCT as a bandage

A join returns duplicates, so someone adds `DISTINCT`. The duplicates came from a missing join
condition or a fan-out, and `DISTINCT` only hides them, at the cost of a sort, while sums stay
wrong. **Fix:** find the join that multiplies rows; use `EXISTS` when you only need "has one".

### A filter on the optional side of an outer join, in WHERE

`LEFT JOIN orders o ON ... WHERE o.status = 'paid'` silently becomes an inner join (Phase 1).
**Fix:** move the condition into `ON`.

### AND and OR without parentheses

`WHERE a = 1 OR b = 2 AND c = 3` means `a = 1 OR (b = 2 AND c = 3)`. With joins in `WHERE`
(the comma syntax) a stray `OR` turns a join into a cross join. **Fix:** explicit `JOIN ... ON`,
and parentheses around every `OR`.

### BETWEEN on timestamps

```sql
WHERE placed_at BETWEEN '2026-03-01' AND '2026-03-31'   -- misses all of March 31 after 00:00
```

The upper bound is midnight at the **start** of the 31st. `'2026-03-31 23:59:59'` still misses
fractions of a second. **Fix:** a half-open range, `placed_at >= '2026-03-01' AND placed_at <
'2026-04-01'`. Watch time zones too: "March" in Lisbon and in UTC are different instants.

### Integer division and float money

`SELECT 1 / 3` is `0` with integers (PostgreSQL, SQL Server, SQLite); `avg` of an integer
column truncates in SQL Server. `0.1 + 0.2` in `double precision` is `0.30000000000000004`.
**Fix:** cast to `numeric` before dividing, and store money as `numeric` (or integer cents).

### Relying on an order you did not ask for

Rows come back in "insertion order" until a plan changes. Pagination, "latest", "first" and
reports all need `ORDER BY`, on a unique combination when ties are possible.

## Slow results

### Functions and casts on indexed columns

`WHERE date(placed_at) = ...`, `WHERE lower(email) = ...`, `WHERE id::text = '42'`,
`WHERE to_char(placed_at, 'YYYY-MM') = '2026-03'` hide the column from its index (Phase 3).
Implicit casts do the same: comparing a `varchar` column with a number in MySQL, or an
`nvarchar` parameter with a `varchar` column in SQL Server, can turn a seek into a scan.
**Fix:** rewrite as a range on the bare column, compare with the column's own type, or build an
expression index.

### Leading wildcards

`WHERE name LIKE '%son'` cannot use a B-tree. **Fix:** a trigram index (`pg_trgm`) or full-text
search when it matters.

### OFFSET for deep pages

`ORDER BY placed_at DESC OFFSET 100000 LIMIT 20` reads and throws away 100 000 rows, and pages
shift when rows are inserted. **Fix:** keyset pagination (next lesson).

### Row by row ("RBAR")

A loop in the application that runs one `SELECT` or `UPDATE` per row (the N+1 problem, next
lesson), or a cursor in a stored procedure doing what one set-based statement can do.
**Fix:** one statement over the whole set: `UPDATE ... FROM`, `INSERT ... SELECT`, `= ANY($1)`.

### SELECT * in application code

It reads columns you do not use (some of them large), defeats index-only scans, and breaks
code that reads columns by position when the table changes. **Fix:** list the columns.

### COUNT(*) to check existence

`SELECT count(*) FROM orders WHERE customer_id = 7` counts every match to answer "is there
one?". **Fix:** `SELECT EXISTS (SELECT 1 FROM orders WHERE customer_id = 7)`.

### Huge IN lists and generated SQL

Thousands of literals in `IN (...)` blow up parsing and fill the plan cache with one-off
statements. **Fix:** pass an array parameter (`= ANY($1)`) or join a temporary table.

## Schema anti-patterns

| Anti-pattern | Why it hurts | Better |
| --- | --- | --- |
| comma-separated ids in a column ("jaywalking") | no foreign key, no index, LIKE '%,42,%' queries | a junction table |
| entity-attribute-value tables | no types, no constraints, a join per attribute | real columns, plus `jsonb` for the truly open part |
| `float` for money | rounding errors | `numeric` or integer cents |
| magic values (`-1`, `'1900-01-01'`) instead of NULL | they leak into sums and averages | NULL, or a separate status column |
| no foreign keys "for speed" | orphans and silent inconsistency | foreign keys with indexed columns |
| `timestamp` without zone for events | ambiguous instants, DST bugs | `timestamptz` |
| polymorphic `(owner_type, owner_id)` | no foreign key possible | one nullable foreign key per owner type, with a CHECK |
| one table per customer or per month | schema changes multiply | one table, partitioned if it is huge |

## Summary

- Wrong results: NOT IN with NULLs, fan-out, DISTINCT bandages, WHERE on the optional side,
  AND/OR precedence, BETWEEN on timestamps, integer division, missing ORDER BY.
- Slow results: functions on indexed columns, leading wildcards, deep OFFSET, row by row
  work, SELECT *, COUNT for existence, huge IN lists.
- Schema: lists in columns, EAV, float money, magic values, missing foreign keys.

## Further reading

- PostgreSQL wiki, "Don't Do This": <https://wiki.postgresql.org/wiki/Don%27t_Do_This>
- *SQL Antipatterns* by Bill Karwin: <https://pragprog.com/titles/bksap1/sql-antipatterns-volume-1/>
- Use The Index, Luke, "Functions" in the WHERE clause: <https://use-the-index-luke.com/sql/where-clause/functions>
- Use The Index, Luke, "We need tool support for keyset pagination" (no OFFSET): <https://use-the-index-luke.com/no-offset>
- jOOQ blog, "10 Common Mistakes Java Developers Make when Writing SQL": <https://blog.jooq.org/10-common-mistakes-java-developers-make-when-writing-sql/>
- SQLite, "Quirks, Caveats, and Gotchas": <https://www.sqlite.org/quirks.html>
