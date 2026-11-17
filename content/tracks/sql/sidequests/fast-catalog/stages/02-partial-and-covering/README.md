Two more pages for the back office. Add their indexes to `indexes.sql`.

| File | Columns | Rows and order | Budget |
|---|---|---|---|
| `low_stock.sql` | `id`, `sku`, `stock` | the **active** products with less than 5 in stock; by `stock`, then `id` | 30 buffers, through a **partial** index of less than 512 kB |
| `brand_summary.sql` | `products`, `cheapest`, `dearest` | one row: how many active `Kestrel` products there are, and their lowest and highest price | 40 buffers, as an **Index Only Scan** |

About 2% of the products are low on stock, spread all over the table. An index on `stock`
finds them fast, but then fetching each row from the table still reads hundreds of pages:
look at the buffers. Two ideas fix that:

- a **partial index** (`CREATE INDEX ... WHERE <condition>`) stores only the rows that match
  its condition, so it stays small; the planner uses it when the query's `WHERE` implies that
  condition;
- a **covering index** stores every column the query needs (the search keys, plus others in
  `INCLUDE (...)`), so the plan becomes an `Index Only Scan` and skips the table.

> [!NOTE]
> An index-only scan can skip the table only for pages the visibility map marks as
> all-visible; the fixture vacuums the tables, as autovacuum would. Compare
> `pg_relation_size('your_index')` of a partial index and of the same index without `WHERE`.
