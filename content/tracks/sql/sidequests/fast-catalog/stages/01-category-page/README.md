Two pages of the shop, and the indexes behind them.

| File | Columns | Rows and order | Budget |
|---|---|---|---|
| `category_page.sql` | `id`, `sku`, `price` | the 20 cheapest **active** products of category 7; equal prices by `id` | 40 buffers, and no `Sort` in the plan |
| `sku_lookup.sql` | `id`, `sku`, `name` | the product whose sku is `eh-004242`, **ignoring case** (the shop stores `EH-004242`) | 6 buffers |

`indexes.sql` holds your `CREATE INDEX` statements. Give your indexes names
(`CREATE INDEX products_..._idx ON ...`): you will refer to them later.

The shop also wants skus to be **unique ignoring case**: inserting `eh-004242` next to
`EH-004242` must fail with a unique violation, while new skus are still accepted. The table
has no constraint on `sku` yet; an index can be that constraint.

Before you write any index, look at the plans as they are now:

```sql
EXPLAIN (ANALYZE, BUFFERS) SELECT ... -- your category page
```

The fixture already has an index on `category_id`. Why does the plan still read about a
thousand pages, and sort?

> [!TIP]
> A B-tree on `(a, b, c)` finds the rows with `a = 7` and returns them already sorted by
> `b, c`, so `ORDER BY b, c LIMIT 20` stops after 20 entries. An index on an expression,
> such as `lower(sku)`, is used only when the query has the same expression.
