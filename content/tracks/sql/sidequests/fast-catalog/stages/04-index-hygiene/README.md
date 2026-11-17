The product page shows reviews, and reviews have no index yet. Then you clean up: every index
costs disk, memory and time on each `INSERT`, `UPDATE` and `DELETE`, so the ones that help no
query should go.

| File | Columns | Rows and order | Budget |
|---|---|---|---|
| `recent_reviews.sql` | `id`, `rating`, `created_at` | the 5 latest reviews of product `65413` rated 4 or 5; newest first, equal times by `id`, highest first | 20 buffers |
| `review_summary.sql` | `sku`, `reviews`, `average` | one row for product `65413`: its sku, its number of reviews and their average rating rounded to 2 decimals (`0` and `NULL` when it has none) | 20 buffers |

PostgreSQL indexes primary keys and unique constraints by itself, but **not foreign keys**:
`reviews.product_id` has no index.

**`cleanup.sql`** drops, with `DROP INDEX`, the indexes of the fixture that do not earn their
keep (`\di` in psql lists them all). Drop an index when:

- it has the same columns as another index, such as the primary key's;
- its columns are the **leading** columns of another index (an index on `(a, b)` also finds
  rows by `a` alone);
- it is on a column with so few values that a sequential scan is cheaper (a boolean that is
  true in nine rows out of ten, a rating from 1 to 5).

Keep the primary keys and your own indexes. After `indexes.sql` and `cleanup.sql`,
`products` has at most 7 indexes and every page of the earlier stages is still within its
budget: the tests measure them again.

> [!TIP]
> `pg_stat_user_indexes.idx_scan` counts how often each index was used since the statistics
> were reset: in production, that and `pg_relation_size` tell you what to drop.
