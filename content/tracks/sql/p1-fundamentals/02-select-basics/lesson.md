# SELECT, WHERE, ORDER BY and Expressions

A query reads like English but does not run in the order you write it. Once you know the
real order, most "why does this fail?" questions answer themselves.

## The logical order

You write:

```sql
SELECT category, count(*) AS products
FROM products
WHERE price > 5
GROUP BY category
HAVING count(*) > 1
ORDER BY products DESC
LIMIT 3;
```

The engine evaluates, logically:

| Step | Clause | Produces |
| --- | --- | --- |
| 1 | `FROM` (and `JOIN`) | the input rows |
| 2 | `WHERE` | rows that pass the filter |
| 3 | `GROUP BY` | one group per distinct key |
| 4 | `HAVING` | groups that pass the filter |
| 5 | `SELECT` | the output columns, with their aliases |
| 6 | `DISTINCT` | duplicates removed |
| 7 | `ORDER BY` | sorted rows |
| 8 | `LIMIT` / `FETCH FIRST` | the first n |

Consequences:

- An alias made in `SELECT` **cannot be used in `WHERE`** (step 2 runs before step 5). It
  can be used in `ORDER BY` (step 7).
- `WHERE` filters rows, `HAVING` filters groups (next lessons).
- The planner may do the work in another physical order; the result is as if it followed
  this one.

## Picking columns

```sql
SELECT * FROM products;                          -- every column: fine in psql, not in code
SELECT name, price FROM products;                -- projection
SELECT name, price * 1.23 AS gross FROM products;   -- expressions with an alias
SELECT name || ' (' || category || ')' AS label FROM products;   -- || joins text
SELECT 'today is ' || current_date;              -- no FROM needed in PostgreSQL
```

`SELECT *` in application code breaks when someone adds or reorders columns, and it reads
columns you do not need. List them.

## Filtering with WHERE

| Operator | Example |
| --- | --- |
| comparison | `price >= 10`, `country <> 'PT'` (`!=` works too) |
| logic | `a AND b`, `a OR b`, `NOT a` |
| range | `price BETWEEN 10 AND 20` (both ends included) |
| list | `country IN ('PT', 'BR', 'AO')` |
| pattern | `name LIKE 'Ba%'` (`%` any text, `_` one character); `ILIKE` ignores case (PostgreSQL) |
| null test | `phone IS NULL` (next lesson: never `= NULL`) |

`AND` binds tighter than `OR`, so use parentheses:

```sql
WHERE category = 'training' OR category = 'books' AND price < 10      -- not what you meant
WHERE (category = 'training' OR category = 'books') AND price < 10
```

Text comparisons depend on the column's **collation**: whether `'a' = 'A'`, and how
`ORDER BY` sorts accents. PostgreSQL compares case-sensitively by default; MySQL's default
collations ignore case; SQL Server depends on the server setting.

## CASE: if/else inside an expression

```sql
SELECT name,
       CASE
           WHEN price < 10 THEN 'cheap'
           WHEN price <= 50 THEN 'regular'
           ELSE 'premium'
       END AS band
FROM products;
```

The first `WHEN` that is true wins; with no `ELSE` the result is NULL. `CASE` works anywhere
an expression does: `SELECT`, `WHERE`, `ORDER BY`, inside aggregates.

Useful functions: `lower`, `upper`, `length`, `trim`, `substring`, `replace`, `round(x, 2)`,
`abs`, `coalesce` (next lesson), `now()`, `current_date`, `extract(year FROM d)`,
`date_trunc('month', ts)` (PostgreSQL), `cast(x AS integer)` or PostgreSQL's `x::integer`.

## Sorting

```sql
ORDER BY price DESC, name          -- price high to low, ties by name
ORDER BY 2                         -- by the second output column: legal, fragile
ORDER BY price DESC NULLS LAST     -- where NULLs go (PostgreSQL, Oracle; standard)
```

Ascending is the default. Sort on enough columns to make the order unique when it matters:
"the latest orders" sorted only by a date returns ties in any order, and pages of results
then repeat or skip rows.

## DISTINCT and limiting

`SELECT DISTINCT country FROM customers` returns each value once; with several columns, each
**combination** once.

To keep the first rows:

```sql
SELECT name, price FROM products ORDER BY price DESC
FETCH FIRST 5 ROWS ONLY;            -- standard; PostgreSQL, Oracle, SQL Server (with OFFSET)
SELECT name, price FROM products ORDER BY price DESC
LIMIT 5;                            -- PostgreSQL, MySQL, SQLite
SELECT TOP 5 name, price FROM products ORDER BY price DESC;   -- SQL Server
```

`FETCH FIRST 5 ROWS WITH TIES` also keeps rows tied with the fifth (PostgreSQL 13+, SQL
Server's `TOP 5 WITH TIES`). `OFFSET 20` skips rows, but it still reads them: Phase 4 shows
a better way to page.

## Summary

- Logical order: FROM, WHERE, GROUP BY, HAVING, SELECT, DISTINCT, ORDER BY, LIMIT.
- Aliases from SELECT work in ORDER BY, not in WHERE.
- Use parentheses when you mix AND and OR; BETWEEN includes both ends.
- CASE returns the first matching branch, NULL without ELSE.
- Sort on a unique combination when order matters; FETCH FIRST is the standard LIMIT.

## Further reading

- PostgreSQL 17, "Queries" (table expressions, select lists, sorting, limit): <https://www.postgresql.org/docs/17/queries.html>
- PostgreSQL 17, "LIMIT and OFFSET": <https://www.postgresql.org/docs/17/queries-limit.html>
- PostgreSQL 17, "Conditional Expressions": <https://www.postgresql.org/docs/17/functions-conditional.html>
- Modern SQL, "CASE": <https://modern-sql.com/feature/case>
- Modern SQL, SELECT without FROM: <https://modern-sql.com/use-case/select-without-from>
