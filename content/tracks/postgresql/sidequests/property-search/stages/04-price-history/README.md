The market reports. `price_history` gets a row every 5 minutes, from 2016 to the end of 2025,
always appended at the end, and it will keep growing for years. Write the two queries and
add their indexes to `indexes.sql`.

| File | Columns | Rows and order | Budget |
|---|---|---|---|
| `week_average.sql` | `average_price`, `observations` | one row for the week from 2024-03-04 (included) to 2024-03-11 (excluded): the average `price` rounded to 2 decimals, and how many observations | 300 buffers |
| `daily_counts.sql` | `day`, `observations`, `highest` | one row per day of March 2024 (a `date`), with its number of observations and its highest `price`; by `day` | 400 buffers |

And one more rule: **all the indexes of `price_history` together take at most 256 kB**
(`pg_relation_size` of each). A B-tree on `observed_at` would meet the budgets, at about 22 MB
for a million rows, a size that grows with every row.

> [!TIP]
> A BRIN (block range) index stores, for each range of table pages (128 by default), the
> smallest and largest value in it. It is tiny, and it is only useful when the column follows
> the physical order of the rows, as an append-only log's timestamp does: then a time window
> maps to a few page ranges, and the plan reads just those (a `Bitmap Heap Scan` with
> `lossy` heap blocks, rechecking each row). Look at `correlation` in `pg_stats` for
> `observed_at`, and try `WITH (pages_per_range = ...)`.
