# Slowly Changing Dimensions

Dimensions change: customers move, products change category, sales reps change region.
They change **slowly** compared with facts, but they change, and every dimension
attribute needs an answer to one question: **when it changes, what should old facts
show?** Kimball named the answers "slowly changing dimension" (SCD) types.

## The types

| Type | On change | History | Use for |
| --- | --- | --- | --- |
| **0** | keep the original value forever | the first value only | birth date, original signup channel |
| **1** | overwrite in place | lost | corrections (typos), attributes nobody reports historically |
| **2** | close the current row, insert a new version | full | attributes reports must show "as it was": segment, region, category |
| **3** | keep the previous value in an extra column | one step back | a reorganisation where people compare "old region" and "new region" |
| **6** | type 2 rows **plus** a type 1 "current value" column on every version | full, and today's value | reports that need both "as it was" and "as it is now" |

Types 4 (history in a separate table) and 5 exist too; 1 and 2 cover most needs. The choice
is **per attribute**, not per table: one customer dimension can overwrite the e-mail
(type 1) and keep history of the segment (type 2).

## Type 2 in detail

```text
customer_key | customer_id | city      | segment | valid_from | valid_to   | is_current
-------------+-------------+-----------+---------+------------+------------+-----------
          11 |           1 | London    | retail  | 2026-01-01 | 2026-06-01 | false
          37 |           1 | Cambridge | retail  | 2026-06-01 | 9999-12-31 | true
```

- Each **version** has its own **surrogate key**. Facts store the key of the version that
  was current when they happened, so they keep pointing at "London".
- `valid_from` is inclusive and `valid_to` exclusive, so consecutive versions meet without
  a gap or an overlap. The current row ends on a far-future date (`9999-12-31`) rather
  than `NULL`, which keeps range conditions simple.
- `is_current` makes "the current version" a cheap filter; a partial unique index
  (`UNIQUE (customer_id) WHERE is_current`) guarantees there is only one.

Applying a change is two statements in one transaction: **close** the current row
(`valid_to = change date`, `is_current = false`) and **insert** the new version. A new
customer gets a first version. An unchanged customer gets nothing, which makes the job
idempotent.

Comparing attributes needs care with `NULL`: `NULL <> 'enterprise'` is `NULL`, not true,
so the change is missed. Use `IS DISTINCT FROM`, which treats `NULL` as a value.

## Point-in-time joins

When facts carry the natural key and a date instead of the surrogate key (common when they
arrive before the dimension is updated), join on the range:

```sql
SELECT d.segment, sum(f.amount)
FROM fact_orders AS f
JOIN dim_customer AS d
  ON d.customer_id = f.customer_id
 AND f.order_date >= d.valid_from
 AND f.order_date <  d.valid_to
GROUP BY d.segment;
```

For "by today's segment", join on the natural key to the row with `is_current`, or keep a
type 6 `current_segment` column on every version.

## Pitfalls

- **Late-arriving dimensions**: a fact refers to a customer the dimension has not seen.
  Insert a placeholder ("inferred member") row and fill it when the data arrives, rather
  than dropping the fact.
- **Out-of-order changes**: applying an older snapshot after a newer one rewrites history
  wrongly. Process changes in order, or rebuild the dimension from the full change log.
- **Noise**: type 2 on an attribute that changes every day (last login) explodes the
  table. Move such attributes to a fact or a separate "mini-dimension".

## Summary

- Decide per attribute what old facts should show: original (0), current (1) or as it was (2).
- Type 2 rows have their own surrogate key and a half-open `[valid_from, valid_to)` range.
- Close then insert, compare with `IS DISTINCT FROM`, and the job stays idempotent.
- Join facts to the version valid at the fact's date.

## Further reading

- Ralph Kimball and Margy Ross, *The Data Warehouse Toolkit*, 3rd edition, chapter 5 (slowly changing dimension techniques)
- Kimball Group, "Slowly Changing Dimensions, Types 0 to 7": <https://www.kimballgroup.com/2013/02/design-tip-152-slowly-changing-dimension-types-0-4-5-6-7/>
- dbt documentation, snapshots (type 2 history from mutable tables): <https://docs.getdbt.com/docs/build/snapshots>
- PostgreSQL documentation, comparison predicates and `IS DISTINCT FROM`: <https://www.postgresql.org/docs/17/functions-comparison.html>
