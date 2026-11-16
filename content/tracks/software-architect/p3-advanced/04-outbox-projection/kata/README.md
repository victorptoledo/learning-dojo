# Outbox and Projection

`dojo.events` has a tiny transactional `Database` (orders and an outbox, all or nothing per
`inTransaction`) and a `Broker` to publish events to. `OrderService.place` publishes **inside**
the transaction, before it has finished validating: a refused order still reaches the broker,
and a broker that is down makes the order fail.

`Database`, `Transaction`, `Broker`, `Event`, `OrderRow` and `OutboxMessage` are given: do not
change them.

## Part 1: the outbox

`OrderService(Database db)` (no broker any more):

- `place(orderId, customerId, amountCents)`: an amount below 1 or an order id that exists throws
  `IllegalArgumentException`. Otherwise, **in one transaction**, insert
  `OrderRow(orderId, customerId, amountCents, "PLACED")` and append an unsent outbox message with
  `Event("evt-" + orderId + "-placed", "OrderPlaced", orderId, customerId, amountCents)`.
- `cancel(orderId)`: unknown → `IllegalArgumentException`; already cancelled →
  `IllegalStateException`. Otherwise, in one transaction, set the status to `"CANCELLED"` and
  append `Event("evt-" + orderId + "-cancelled", "OrderCancelled", orderId, customerId,
  amountCents)`.

## Part 2: the relay

`OutboxRelay(Database db, Broker broker)`, `int relay()`: publishes the unsent messages **in
outbox order**, marking each one sent right after the broker accepts it. When `publish` throws,
it stops and returns how many it published; the next call starts again from the first unsent
message. A second `relay()` with nothing new publishes nothing.

## Part 3: the projection

`OrderSummaryProjection implements Consumer<Event>` builds a read model:
`summary(customerId)` returns `CustomerSummary(customerId, openOrders, openAmountCents)`
(zeros for a customer it has never seen). `OrderPlaced` adds one open order and its amount;
`OrderCancelled` removes them. An event whose id it has already applied is **ignored**, because
the relay delivers at least once.

## What the grading tests check

- Refused orders write nothing and publish nothing; placed and cancelled orders write the row and
  the outbox message together, and nothing reaches the broker before `relay()`.
- The relay: order, marking, a broker that fails half way, and resuming.
- The projection, with duplicates and cancellations, alone and fed by the relay through a broker
  that redelivers.

The visible test uses the new `OrderService(Database)` constructor, so it does not compile
until Part 1 is done.
