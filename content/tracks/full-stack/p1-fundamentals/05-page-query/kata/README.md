# From Tables to a Page: the "My Orders" Screen

The shop's "My orders" screen shows a customer's orders, newest first, three at a time, each with
its lines and total, and a "Load more" button. The first version made one query for the orders
and then one more query per order for its lines (the N+1 problem). You replace it with **one
query that returns the whole page as the JSON the UI renders**, plus the migration the screen
needs.

The tables are in `fixture/01_shop.sql`:

```text
customers    (id, name)
products     (id, name, price_cents)
orders       (id, customer_id → customers, placed_at timestamptz)
order_items  (order_id → orders, product_id → products, quantity, unit_price_cents)
page_request (customer_id, page_size, after_placed_at, after_id)   -- one row: the screen's request
```

`page_request` stands for the API's query parameters: read the customer, the page size and the
cursor from its single row (`after_placed_at` and `after_id` are both NULL for the first page).

## What to write

| File | What |
| --- | --- |
| `migration.sql` | Add `orders.status`: `text`, `NOT NULL`, default `'placed'`, only `'placed'`, `'shipped'`, `'delivered'` or `'cancelled'` allowed (existing orders become `'placed'`). Create the index `orders_page_idx` on `orders (customer_id, placed_at DESC, id DESC)`, which matches the screen's order |
| `order_page.sql` | One row per order of the page: `id`, `placed_at`, `item_count` (the sum of the quantities), `total_cents` (the sum of quantity × unit price). The orders of `page_request.customer_id`, newest first, with **`id` descending as the tie-breaker**; with a cursor, only the orders that come strictly after `(after_placed_at, after_id)` in that order; at most `page_size` rows |
| `page_json.sql` | One row, one `jsonb` column: the document below |

```json
{
  "customer": { "id": 1, "name": "Ada" },
  "orders": [
    {
      "id": 6,
      "placedAt": "2026-01-15T08:15:00Z",
      "totalCents": 2950,
      "items": [
        { "product": "Mug", "quantity": 1, "unitPriceCents": 1200 },
        { "product": "Notebook", "quantity": 2, "unitPriceCents": 800 },
        { "product": "Pen", "quantity": 1, "unitPriceCents": 150 }
      ]
    }
  ],
  "nextCursor": { "placedAt": "2026-01-12T18:00:00Z", "id": 4 }
}
```

- `orders` is the page of `order_page.sql`, in the same order, each with its `items` ordered by
  product name; an empty page is `[]`, not `null`.
- `placedAt` is UTC in exactly the format `YYYY-MM-DDTHH:MM:SSZ`, whatever the session's time
  zone (the browser formats it for the reader).
- `nextCursor` is the `placedAt` and `id` of the last order of the page when **more orders
  follow**, and JSON `null` when this page is the last one (fetch one row more than the page
  size to know).

Keyset pagination with `(placed_at, id) < (cursor_placed_at, cursor_id)` stays fast and stable
while new orders arrive, unlike `OFFSET`.

Try your queries with `docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play`.

There are 11 tests: 1 in `tests/visible.sql` and 10 hidden ones (a second page, ties on
`placed_at`, an empty page, another time zone, the migration's checks).
