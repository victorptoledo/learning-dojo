# A Type 2 Customer Dimension

`dim_customer` (see `fixture/01_customers.sql`) keeps the **history** of each customer: one
row per version, valid from `valid_from` (inclusive) to `valid_to` (exclusive). The current
version ends on `9999-12-31` and has `is_current = true`. A unique index allows one current
version per customer.

Every night the CRM sends a full extract, `customer_snapshot`, with one row per customer it
knows and the same `snapshot_date` on every row. Write the job that applies it.

## `apply_snapshot.sql`

Compare each snapshot row with the dimension:

| Case | What to do |
| --- | --- |
| the customer is new | insert a first version: `valid_from` = snapshot date, `valid_to` = `9999-12-31`, current |
| `city` or `segment` differs from the **current** version (type 2) | close the current version (`valid_to` = snapshot date, not current) and insert a new current version from the snapshot date |
| `name` differs (type 1, a correction) | overwrite the name in **every** version of that customer, without a new version |
| nothing differs | nothing |
| the customer is missing from the snapshot | nothing (deletions are not tracked) |

A customer can have a type 1 and a type 2 change in the same snapshot. Closed versions keep
their `customer_key`, so facts that point at them stay valid; new versions get new keys from
the identity column.

`segment` can be `NULL` (not classified yet). `NULL` becoming `'enterprise'` **is** a change,
and `NULL` staying `NULL` is not: `<>` gets both wrong, `IS DISTINCT FROM` gets both right.

Applying the same snapshot twice must change nothing.

> [!TIP]
> A data-modifying CTE can close the versions and hand the changed rows to the `INSERT`
> that opens the new ones: `WITH closed AS (UPDATE ... RETURNING ...) INSERT ... SELECT ... FROM closed`.

## `revenue_by_segment.sql`

Revenue of `fact_orders` by the segment the customer had **on the order date** (the version
whose range contains it), with `'unknown'` for a `NULL` segment, ordered by segment. This is
the point-in-time join that a type 2 dimension exists for.

Try it with `docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play` and
`\i apply_snapshot.sql`. Some tests apply a second snapshot after the first.

There are 12 tests: 1 in `tests/visible.sql` and 11 hidden ones.
