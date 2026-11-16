# Extract a Bounded Context

Two services share `dojo.shop.catalog.Product` and its `ProductRepository`:

- `sales.SalesService` quotes, reprices and takes orders, and while taking an order it checks
  and decrements `stockOnHand` itself;
- `warehouse.WarehouseService` receives goods, says where to pick them, and reports stock.

Both write to the same mutable object. Split them into two **bounded contexts**.

## The target design

| Context | Type | What it is |
| --- | --- | --- |
| sales | `sales.Product` | a **record** `(String sku, String name, long priceCents, boolean discontinued)` |
| sales | `sales.Products` | an in-memory store: `save(Product)`, `Optional<Product> find(String sku)` |
| sales | `sales.SalesService` | `SalesService(Products products, Stock stock)`: `quote`, `reprice`, `placeOrder` |
| warehouse | `warehouse.StockItem` | a **record** `(String sku, int weightGrams, String bin, int onHand)` |
| warehouse | `warehouse.StockItems` | an in-memory store: `save(StockItem)`, `Optional<StockItem> find(String sku)` |
| warehouse | `warehouse.api.Stock` | the published interface: `boolean reserve(String sku, int quantity)` |
| warehouse | `warehouse.WarehouseService` | `WarehouseService(StockItems items)` implements `Stock`: `receive`, `pickLocation`, `stock` |
| root | `dojo.shop.App` | `create(List<catalog.Product> legacy)` copies each legacy record into both stores and wires the contexts |

The relationship is **customer and supplier**: sales (downstream) uses only
`warehouse.api`; the warehouse knows nothing of sales. Neither context uses `dojo.shop.catalog`
any more; it stays as the legacy import format, read only by `App`.

The rules (`Order` and `Quote` keep their shape):

- `quote(sku, quantity)` is price × quantity; `reprice(sku, cents)` changes the sales price only.
- `placeOrder(orderId, sku, quantity)`: an unknown sku or a quantity below 1 throws
  `IllegalArgumentException`; a discontinued product throws `IllegalStateException("not for
  sale: " + sku)`; then `stock.reserve` is asked, and `false` throws `IllegalStateException("out
  of stock: " + sku)`. The order total is price × quantity.
- `reserve(sku, quantity)` returns `false` (and changes nothing) for an unknown sku or when fewer
  are on hand, otherwise lowers `onHand`. `receive(sku, quantity)` raises it (quantity at least 1,
  sku known, or `IllegalArgumentException`). `pickLocation` returns the bin, `stock` the count on
  hand.

## What the grading tests check

- The graph: sales uses nothing in `catalog` and nothing in `warehouse` outside `warehouse.api`;
  the warehouse uses nothing in `sales` or `catalog`.
- The two records hold exactly the components above.
- Every rule, through `App.create` and through each context alone.

Keep your tests in the package `dojo`.
