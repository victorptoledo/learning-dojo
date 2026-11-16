# Module Boundaries

`dojo.store` is a modular monolith with three modules, each split in two:

```text
dojo.store.catalog.api       published: what other modules may use
dojo.store.catalog.internal  private: free to change
dojo.store.orders.api / .internal
dojo.store.shipping.api / .internal
dojo.store.App, Store        the composition root, outside every module
```

## Part 1: the rule

Write `dojo.fitness.ModuleRules.violations(graph, root, modules)`. `modules` are the names of
the modules (`"catalog"`, `"orders"`, `"shipping"`); a class belongs to a module when its name
starts with `root + "." + module + "."`. For every use `from -> to` where **both** classes
belong to a module:

- across two modules, the use is allowed only if `to` is in the other module's `api` package (or
  a subpackage of it);
- inside one module, anything is allowed, except an `api` class using a class of the same
  module's `internal` package: an api that exposes internal types publishes them too.

Uses from or to a class outside the modules (the composition root, the JDK) are not checked.
Return the violations as `"<from> -> <to>"`, sorted, without duplicates.

## Part 2: close the shortcuts

`ArchitectureTest` lists them. The target:

| Type | What it does |
| --- | --- |
| `catalog.api.ProductView` | the record `(sku, name, priceCents, weightGrams)`, without the `of(ProductRow)` factory |
| `catalog.internal.CatalogService` | maps its own rows to `ProductView` |
| `shipping.api.Shipping` | adds `Shipment ship(String orderId, int weightGrams)`; `shipments()` stays |
| `shipping.internal.ShippingService` | `ShippingService()`; ships by weight: up to 2000 g a `"letter"` for 450 cents, up to 20000 g a `"parcel"` for 900, above that `"freight"` for 4000 |
| `orders.internal.OrderService` | `OrderService(Catalog catalog, Shipping shipping)` implements `Orders` using only the two apis |
| `dojo.store.App` | `create(List<ProductRow> products)` wires the modules through their apis |

`Orders.place(orderId, quantities)` (a map from sku to quantity) keeps its rules: every sku must
exist (`IllegalArgumentException("unknown sku " + sku)`) and every quantity be positive
(`IllegalArgumentException`), checked before anything is shipped; the goods cost the sum of price
× quantity; the parcel weighs the sum of weight × quantity; the confirmation carries the carrier
and the shipping cost. `orders.internal.OrderStore` is not needed any more: you may delete it or
leave it unused.

## What the grading tests check

- `ModuleRules` on small graphs: across modules, inside a module, api subpackages, classes
  outside the modules, and modules whose names start alike.
- The store's own graph: the boundary rule holds.
- Orders, prices, weights, carriers and rejected orders, through `App.create`.

Keep your tests in the package `dojo`.
