# Design an Aggregate

`dojo.orders.Order` is anemic: getters and setters for everything, a live `List<OrderLine>`
anyone can edit, a `totalCents` field that callers must remember to update, and a whole
`Customer` inside. `OrderService` checks some rules in some methods.

Rewrite it as an aggregate.

## The target design

| Type | What it is |
| --- | --- |
| `Money` | **record** `(long cents, String currency)`: cents ≥ 0 and a three-letter currency, or `IllegalArgumentException`; `plus(Money)` (same currency, or `IllegalArgumentException`), `times(int)`, `static Money zero(String currency)` |
| `OrderId`, `CustomerId` | **records** `(String value)`, not blank |
| `OrderLine` | **record** `(String sku, int quantity, Money unitPrice)` with `Money total()` |
| `OrderStatus` | enum `DRAFT`, `SUBMITTED`, `CANCELLED` |
| `DomainEvent` | a **sealed** interface; records `OrderSubmitted(OrderId orderId, CustomerId customerId, Money total)` and `OrderCancelled(OrderId orderId, String reason)` implement it |
| `Order` | the aggregate root: a **final** class with no public constructor and no setters |
| `OrderRepository` | in memory: `save(Order)`, `Optional<Order> find(OrderId)` |
| `OrderService` | `OrderService(OrderRepository orders, Consumer<DomainEvent> publisher)` |

`Order`:

- `static Order draft(OrderId id, CustomerId customerId, String currency)`; `id()`,
  `customerId()`, `currency()`, `status()`; `lines()` returns an unmodifiable copy; `total()` is
  the sum of the lines (zero in the order's currency when empty).
- `addLine(sku, quantity, unitPrice)`: only in `DRAFT` (`IllegalStateException("order is " +
  status)`); quantity ≥ 1 and the order's currency (`IllegalArgumentException`). The same sku
  again adds to its quantity, but only at the same unit price (`IllegalArgumentException("price
  changed for " + sku)`). At most 10 different skus (`IllegalStateException("an order has at most
  10 lines")`). The total may not go above 1,000,000 cents (`IllegalStateException("order total
  above limit")`). A refused command changes **nothing**.
- `removeLine(sku)`: only in `DRAFT`; an unknown sku throws `IllegalArgumentException`.
- `submit()`: only in `DRAFT`, and not when empty (`IllegalStateException("an empty order cannot
  be submitted")`); records `OrderSubmitted` with the total.
- `cancel(reason)`: from `DRAFT` or `SUBMITTED` (else `IllegalStateException`); records
  `OrderCancelled`.
- `pullEvents()` returns the events recorded since the last call, oldest first, and forgets them.

`OrderService` has `OrderId create(String orderId, String customerId, String currency)`,
`addLine(OrderId, String sku, int quantity, Money unitPrice)`, `submit(OrderId)` and
`cancel(OrderId, String reason)`: each loads the order (unknown → `IllegalArgumentException`),
calls it, saves it, and passes its pulled events to the publisher. `Customer` stays a separate
aggregate that `Order` no longer holds.

## What the grading tests check

- The structure: `Order` is final with no public constructor, no public `set...` method and no
  `Customer` field; `lines()` cannot be used to change the order; the value objects are records
  and `DomainEvent` is sealed.
- Every invariant above, including that refused commands leave lines, total, status and events
  unchanged.
- The events, through the service and the publisher.

The visible tests describe the target design, so they do not compile until it exists.
