# NULL and Three-Valued Logic

`NULL` is SQL's marker for "no value here": unknown, not applicable, not entered yet. It is
not zero, not an empty string and not false. Most SQL bugs that survive code review involve
NULL, because it follows rules of its own.

## Comparisons with NULL are UNKNOWN

Any comparison with NULL gives neither TRUE nor FALSE but **UNKNOWN**:

```sql
SELECT NULL = NULL;       -- NULL (unknown), not true
SELECT 1 <> NULL;         -- NULL
SELECT NULL + 1;          -- NULL: arithmetic with NULL is NULL
SELECT 'a' || NULL;       -- NULL in PostgreSQL and the standard (Oracle treats '' as NULL)
```

SQL logic has three values. The tables for AND and OR:

| AND | TRUE | FALSE | UNKNOWN |
| --- | --- | --- | --- |
| **TRUE** | TRUE | FALSE | UNKNOWN |
| **FALSE** | FALSE | FALSE | FALSE |
| **UNKNOWN** | UNKNOWN | FALSE | UNKNOWN |

| OR | TRUE | FALSE | UNKNOWN |
| --- | --- | --- | --- |
| **TRUE** | TRUE | TRUE | TRUE |
| **FALSE** | TRUE | FALSE | UNKNOWN |
| **UNKNOWN** | TRUE | UNKNOWN | UNKNOWN |

`NOT UNKNOWN` is UNKNOWN. The rule to remember: **`WHERE`, `HAVING`, `ON` and `CASE WHEN`
keep only TRUE.** UNKNOWN is thrown away like FALSE. A `CHECK` constraint is the opposite: it
rejects only FALSE, so UNKNOWN passes.

So `WHERE phone = NULL` never matches anything. Test with:

```sql
WHERE phone IS NULL
WHERE phone IS NOT NULL
```

## The NOT IN trap

```sql
-- customers who referred nobody?
SELECT name FROM customers
WHERE id NOT IN (SELECT referred_by FROM customers);
```

If any `referred_by` is NULL, this returns **no rows**. `id NOT IN (3, 5, NULL)` means
`id <> 3 AND id <> 5 AND id <> NULL`; the last part is UNKNOWN, so the whole condition is at
best UNKNOWN and never TRUE. Fixes:

```sql
WHERE NOT EXISTS (SELECT 1 FROM customers r WHERE r.referred_by = c.id)   -- preferred
WHERE id NOT IN (SELECT referred_by FROM customers WHERE referred_by IS NOT NULL)
```

`IN` with a NULL in the list is harmless (a match is still TRUE); only `NOT IN` breaks.

## Tools for NULL

| Tool | Does | Example |
| --- | --- | --- |
| `IS [NOT] NULL` | tests for NULL | `WHERE shipped_at IS NULL` |
| `COALESCE(a, b, ...)` | the first non-NULL argument | `coalesce(nickname, name)`, `coalesce(sum(x), 0)` |
| `NULLIF(a, b)` | NULL when a = b, else a | `total / nullif(count, 0)` avoids division by zero |
| `a IS DISTINCT FROM b` | null-safe `<>`: two NULLs are not distinct | `WHERE old.email IS DISTINCT FROM new.email` |
| `a IS NOT DISTINCT FROM b` | null-safe `=` | MySQL: `a <=> b`; SQLite: `a IS b` |

`IS DISTINCT FROM` is standard and supported by PostgreSQL, SQL Server 2022 and SQLite (as
`IS NOT`), but not MySQL. It is the right test when "both missing" should count as equal,
for example when detecting changed values.

## How the rest of SQL treats NULL

| Feature | Behaviour |
| --- | --- |
| `count(*)` | counts rows, NULLs included |
| `count(col)`, `sum`, `avg`, `min`, `max` | **skip** NULLs; `sum` of no values is NULL, not 0 |
| `avg(col)` | divides by the non-NULL count, so `avg` is not `sum / count(*)` |
| `GROUP BY`, `DISTINCT` | all NULLs form **one** group |
| `ORDER BY` | PostgreSQL and Oracle put NULLs last ascending; MySQL, SQL Server and SQLite first. Say `NULLS FIRST/LAST` to be sure (not in MySQL or SQL Server) |
| `UNIQUE` | NULLs are not equal, so many rows may have NULL (PostgreSQL 15+: `UNIQUE NULLS NOT DISTINCT` to allow one; SQL Server allows only one) |
| Outer joins | missing matches show up as NULL columns (next lesson) |

## Design advice

- Make columns `NOT NULL` unless a missing value is meaningful. Fewer NULLs, fewer surprises.
- Do not invent "magic" values (`-1`, `'1900-01-01'`, `'N/A'`) to avoid NULL: they leak into
  sums and averages.
- When a NULL has two possible meanings ("unknown" and "does not apply"), consider a separate
  column or table that says which.

## Summary

- NULL is "no value". Comparisons with it are UNKNOWN; arithmetic with it is NULL.
- WHERE, HAVING and ON keep only TRUE; CHECK rejects only FALSE.
- Use IS NULL, never = NULL. NOT IN with a NULL in the list returns nothing: use NOT EXISTS.
- COALESCE picks the first value, NULLIF makes one, IS DISTINCT FROM compares null-safely.
- Aggregates skip NULLs; count(*) does not; all NULLs form one group.

## Further reading

- Modern SQL, "The Three-Valued Logic of SQL": <https://modern-sql.com/concept/three-valued-logic>
- Modern SQL, "NULL in SQL": <https://modern-sql.com/concept/null>
- Modern SQL, "IS DISTINCT FROM": <https://modern-sql.com/feature/is-distinct-from>
- PostgreSQL 17, "Comparison Functions and Operators": <https://www.postgresql.org/docs/17/functions-comparison.html>
- PostgreSQL wiki, "Don't Do This" (NOT IN and more): <https://wiki.postgresql.org/wiki/Don%27t_Do_This>
