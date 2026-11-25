# Checkpoint 3: Stock Reservations

The dojo's shop sells 20 000 products. `fixture/01_shop.sql` creates three tables:

```text
products (id, sku, name, price, attrs jsonb, version)
         attrs: {"brand": "Brand 07", "color": "red", "sizes": ["S", "M"], "promo": true}
stock    (product_id → products, on_hand, reserved)       CHECK (reserved <= on_hand)
orders   (id, customer, placed_at timestamptz, items jsonb)
         items: [{"sku": "SKU-00042", "qty": 2, "price": 19.90}, ...]   (price paid per unit)
```

Units **available** to sell are `on_hand - reserved`. Write the four files:

**1. `indexes.sql`** — the catalogue runs these two queries all day, and both read the whole
table. Create **at most two** indexes so that neither plan has a `Seq Scan`, and the second has
no `Sort` either (the index must return the rows already in order):

```sql
SELECT id, sku FROM products WHERE attrs @> '{"color": "red"}' ORDER BY id;

SELECT id, name, price FROM products
WHERE attrs ->> 'brand' = 'Brand 07'
ORDER BY price, id
LIMIT 10;
```

**2. `reserve.sql`** — order 501 wants **2 units of product 42**. Write **one statement** that
reserves them only if at least 2 are available, and returns `product_id` and the units still
`available` afterwards. When fewer than 2 are available it changes nothing and returns no row
(it does not raise an error). Several checkouts run at the same time: a hidden test makes another
session reserve product 42 while your statement runs, and your statement must decide on the
stock as it is **after** that session commits.

**3. `update_attrs.sql`** — an admin opened product 42 when it was at **version 3**, changed its
color to `"blue"` and removed the promotion. Write one statement that saves that: `color`
becomes `"blue"`, the key `promo` is removed, **every other key is kept**, and `version` goes
up by 1, but only if the product is still at version 3 (if someone saved it in between, nothing
changes and no row comes back). Return `id` and the new `version`.

**4. `units_by_product.sql`** — a report of the orders placed in **November 2026, UTC**:

| Columns | Rows and order |
| --- | --- |
| sku, product name, units, revenue | one row per sku ordered that month; `units` is the sum of `qty`, `revenue` the sum of `qty × price` **as paid** (the `price` in the item, not today's catalogue price); a sku that is not in the catalogue still appears, with a NULL name; most units first, then by sku |

Try it with `docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play`: `EXPLAIN` the
two catalogue queries before and after `\i indexes.sql`, and open a second `psql` with
`docker exec -it <container> psql -d dojo` to race two reservations by hand (`BEGIN` in one,
reserve, and run `reserve.sql` in the other before you `COMMIT`).

There are 18 tests: 1 in `tests/visible.sql` and 17 hidden ones.
