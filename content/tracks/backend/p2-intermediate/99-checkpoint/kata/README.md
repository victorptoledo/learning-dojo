# Checkpoint 2: Order Events with an Outbox

The entities (`ShopOrder`, `OutboxEvent`, `Shipment`, `ProcessedEvent`), the broker
(`MessageBroker` and a `FakeBroker` that can be told to fail), two repositories and the error
handling are written. You write the outbox and processed-event repositories, `OrderService`,
`OutboxRelay`, `ShippingConsumer` and `OrderController`.

## 1. Placing an order (`POST /orders` → `201 {"id": ...}`)

In **one transaction**, save the `ShopOrder` and an `OutboxEvent("OrderPlaced", payload)` with
exactly this payload:

```json
{"orderId":1,"customer":"ada","totalCents":500}
```

Then, still in the transaction, a customer named `blocked` is refused with
`OrderRefusedException` (it stands for any failure late in a real transaction): **neither** the
order nor the event may remain. Nothing is published here.

## 2. The relay (`publishPending(batchSize)`)

- Reads up to `batchSize` **pending** events (not published, not dead) **in the order they were
  written**, publishes each to the broker, and marks it `published(clock.instant())`.
- On a broker failure: call `event.failed()` and **stop the run** (the next events must not
  overtake it). After `MAX_ATTEMPTS` failures the event is **dead** and later runs skip it.
- Returns `RelayResult(published, failed)`.

## 3. The consumer (`on(eventId, type, payload)`)

Brokers deliver **at least once**, so the same event can arrive twice, even at the same time. For an
`OrderPlaced` event, create **one** `Shipment` for its `orderId` and return `true`; for a duplicate
return `false` and create nothing. Record the `ProcessedEvent(eventId)` **in the same transaction**
as the shipment: its unique `eventId` makes a concurrent duplicate fail, and that failure means
"already processed".

> Tip: `@Transactional` on `place` and `publishPending`; `@Query("select e from OutboxEvent e where
> e.publishedAt is null and e.dead = false order by e.id")` with a `Pageable`; a `TransactionTemplate`
> in the consumer so you can catch the `DataIntegrityViolationException` of a duplicate around the
> transaction; and a regex such as `"orderId":(\d+)` to read the id from the payload.
