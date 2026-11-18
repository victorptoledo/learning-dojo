The website counts every product page view in the database, thousands per minute. Each click
is an `UPDATE`, and under MVCC every `UPDATE` writes a new version of the row. Done badly, the
table bloats, every index gets a new entry per click, and two clicks at once count as one.
Write `views.sql`.

**The table `product_stats`**

| Column | Rule |
|---|---|
| `product_id` | primary key, references `products` |
| `views` | `bigint`, required, `0` by default |
| `last_viewed_at` | `timestamptz`, the time of the last view |

**The function**

`record_view(p_product bigint) RETURNS bigint` counts one view of the product and returns
its new total: `1` for the first view, then `2`, `3`... It sets `last_viewed_at` to `now()`.
A product that does not exist is an error (a foreign key violation, `23503`).

**What the tests measure**

- **HOT updates.** The tests view 1,000 products once each (the rows are inserted), then once
  more each (the rows are updated), in one transaction, and read the table's counters in
  `pg_stat_xact_user_tables`: at least **90%** of those updates must be HOT (`n_tup_hot_upd`).
- **No lost clicks.** Session A views a product inside a transaction that is still open;
  session B views the same product: B **waits** for A and, once A commits, returns A's total
  plus one (if A rolls back, the total without A's click). The same holds for the very first
  view of a product, when there is no row yet: neither session gets an error.

> [!TIP]
> A HOT (heap-only tuple) update writes the new version **on the same page** and adds no
> index entry. It needs two things: no indexed column changes, and free space on that page.
> A freshly filled table has none: its pages are packed to the table's `fillfactor`, 100% by
> default. `CREATE TABLE ... WITH (fillfactor = N)` keeps room for the next versions of the
> rows. How much room does one more version of every row need? Then, for the lost clicks:
> `views = views + 1` reads the row **after** waiting for its lock, while a value read into
> a variable first was read before. And `INSERT ... ON CONFLICT (...) DO UPDATE` does the
> "insert or update" in one statement, without the gap between "not found" and "insert".
