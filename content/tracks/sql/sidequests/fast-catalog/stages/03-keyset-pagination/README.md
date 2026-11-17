The "new arrivals" page lists the active products, newest first, 25 per page, and people
scroll far. Paging with `OFFSET 50000` reads and throws away 50,000 rows before it returns
anything, so the further down, the slower.

| File | Columns | Rows and order | Budget |
|---|---|---|---|
| `newest.sql` | `id`, `sku`, `created_at` | the first page: the 25 newest **active** products; equal `created_at` by `id`, highest first (`ORDER BY created_at DESC, id DESC`) | 10 buffers |
| `next_page.sql` | `id`, `sku`, `created_at` | the next page: the 25 active products that come **right after** the row stored in `page_cursor` (its `created_at` and `id`), in the same order; fewer at the end | 12 buffers, on any page |

`page_cursor` always holds one row: the `created_at` and `id` of the last product the
learner's app showed. The tests move it around (to the end of the first page, between three
products created at the same minute, near the oldest product) before they run your query.
`next_page.sql` must not use `OFFSET`.

> [!TIP]
> This is **keyset pagination**: `WHERE (created_at, id) < (cursor values)` with a row
> comparison, and an index whose order is the page order. Read the cursor in a subquery
> (`(SELECT created_at, id FROM page_cursor)`): look at the plan to see why a join to
> `page_cursor` is not the same.
