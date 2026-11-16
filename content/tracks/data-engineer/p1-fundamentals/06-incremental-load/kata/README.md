# An Idempotent Incremental Load

An extract job appends every version of every changed order to the landing table
`raw_orders` (see `fixture/01_landing.sql`). It is an honest landing zone, so it is messy:

- an order appears once per change (`placed`, then `paid`...);
- a retried extract lands **exact duplicates**;
- an old version can be **delivered again** later, with a newer `_ingested_at`.

Analysts query `orders`, one row per order with its latest version. Reloading everything
every night is getting slow, so write an **incremental** load.

## `load_orders.sql`

A script (several statements are fine) that:

1. reads only the rows of `raw_orders` whose `_ingested_at` is **after** the watermark in
   `load_state` (row `'orders'`);
2. keeps **one** version per order from those rows: the newest `updated_at`; when two have the
   same `updated_at`, the one that landed last (`_ingested_at`);
3. upserts them into `orders`, but **never** replaces a row with a version whose `updated_at`
   is older than or equal to the one already stored;
4. moves the watermark to the newest `_ingested_at` it read. When there was nothing new, the
   watermark stays where it was.

Running the script again with nothing new must change nothing: the load is **idempotent**.

> [!WARNING]
> `INSERT ... ON CONFLICT DO UPDATE` refuses to update the same row twice in one statement
> (`ON CONFLICT DO UPDATE command cannot affect row a second time`). That is what happens
> if duplicates reach it: deduplicate first, with `row_number() OVER (PARTITION BY ...)`.

## `latest_orders.sql`

A query over the whole `raw_orders` table (no watermark) for a data-quality dashboard:
`order_id`, `status` and `amount` of the latest version (same rule as above), and the
number of raw rows of that order (duplicates included), ordered by `order_id`.

Try it with `docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play` and
`\i load_orders.sql`. The tests run your load on the fixture, then add a second extract and
run it again.

There are 13 tests: 1 in `tests/visible.sql` and 12 hidden ones.
