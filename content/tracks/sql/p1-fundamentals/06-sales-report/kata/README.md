# Sales Report

The shop's tables are in `fixture/01_shop.sql`:

```text
customers (id, name, email, country)
products  (id, name, category, price)
orders    (id, customer_id → customers, placed_at timestamptz, status: pending | paid | cancelled)
order_items (order_id → orders, product_id → products, quantity, unit_price)
```

The value of an item is `quantity * unit_price`; the revenue of an order is the sum of its
items. Only **paid** orders count as sales. Times are in UTC.

Write one query in each file:

| File | Columns | Rows and order |
| --- | --- | --- |
| `revenue_by_customer.sql` | name, number of paid orders, revenue of the paid orders | every customer, also those without paid orders (`0`, `0`); highest revenue first, equal revenue by name |
| `best_sellers.sql` | product name, units sold in paid orders | the 3 products with the most units; equal units by name |
| `never_ordered.sql` | product name | products that appear in no order at all (any status); alphabetical |
| `monthly_revenue.sql` | month (a `date`: the first day of the month), number of paid orders, revenue | months of 2026 with a revenue of **at least** 100; oldest first |

Try your queries with `docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play`
from the kata folder, then `\i revenue_by_customer.sql`.

> [!WARNING]
> Each order appears once per item when you join `order_items`. Counting orders after that
> join needs care.

There are 10 tests: 1 in `tests/visible.sql` and 9 hidden ones. Hidden tests change the data
first (new customers, other statuses, ties, other years), so answer the question, not the
sample.
