Purchasing plans on **2026-11-01**. A row of `prices` is a supplier's price for a part from
`valid_from` on, until the next row of the same part and supplier; rows dated after the day
are announced but not valid yet.

| File | Columns | Rows and order |
|---|---|---|
| `current_prices.sql` | `sku`, `supplier`, `price` | for every part someone sells on the day, the supplier with the lowest **current** price (equal prices: the supplier first by name); by `sku` |
| `price_changes.sql` | `sku`, `supplier`, `valid_from`, `price`, `previous_price`, `change_pct` | every row of `prices`, with the price of the row before it for the same part and supplier, and the change in percent rounded to 1 decimal (`NULL`, `NULL` on the first row); by `sku`, `supplier`, `valid_from` |
| `purchase_plan.sql` | `sku`, `to_buy`, `supplier`, `price`, `line_cost`, `running_total` | the components to buy to build **20** `BK-ROAD` (see below); biggest `line_cost` first, equal costs by `sku` |

The purchase plan:

- `to_buy` is what 20 bikes need (the exploded quantities of stage 1, times 20) minus
  `stock.on_hand`; a component without a `stock` row has nothing on hand. Only components
  with `to_buy > 0` appear.
- `supplier` and `price` are the cheapest current ones, as in `current_prices.sql`;
  `line_cost` is `to_buy * price`.
- A component nobody sells on the day still appears, with `NULL` supplier, price and line
  cost, after the others.
- `running_total` is the sum of `line_cost` over this row and every row above it.

> [!TIP]
> "The latest row of each group" and "the best row of each group" are both
> `row_number() OVER (PARTITION BY ... ORDER BY ...)` kept where it is 1. `lag()` reads the
> row before in its window, and `sum(...) OVER (ORDER BY ...)` is a running total. Name a
> window once with `WINDOW w AS (...)` when several functions share it.
