# Messaging You Can Trust

Messages make systems loosely coupled, and they also make failures quieter: a lost event does not
throw an exception anywhere, it just leaves an order unshipped. This lesson is about the guarantees
brokers really give and the patterns that make "every event processed exactly once, in effect" true.

## Delivery guarantees

| Guarantee | What can happen | How |
| --- | --- | --- |
| **At most once** | messages can be lost, never duplicated | acknowledge before processing |
| **At least once** | messages are never lost, can arrive **twice** | acknowledge after processing |
| **Exactly once** | each message processed once | not achievable end to end over a network |

At least once is what you build on. A consumer processes a message, and the acknowledgement is lost
(crash, timeout, rebalance): the broker delivers it again. So the practical goal is **effectively
once**: at-least-once delivery plus **idempotent processing**.

## Idempotent consumers

Processing the same message twice must leave the same result as once.

- **Natural idempotency**: "set status to SHIPPED" can be repeated; "add 1 to the counter" cannot.
- **Deduplication**: give every message a unique **id**; record processed ids in the same
  transaction as the effect, and skip ids already seen.

```java
@Transactional
public void on(OrderPlaced event) {
    if (processed.existsById(event.id())) {
        return;                                   // a duplicate: already done
    }
    shipments.create(event.orderId());
    processed.save(new ProcessedEvent(event.id()));   // same transaction as the effect
}
```

A `unique` (or primary key) constraint on the processed id also protects against two consumers
handling the same duplicate at the same time.

## The dual-write problem and the outbox

A service updates its database **and** publishes an event:

```java
orders.save(order);                 // 1. commit to the database
broker.publish(new OrderPlaced());  // 2. send to the broker
```

If the process dies between 1 and 2, the order exists but nobody hears about it. Publishing first
has the opposite problem. There is no transaction that spans the database and the broker.

The **transactional outbox** fixes it:

```text
one database transaction:
    insert into orders ...
    insert into outbox (id, type, payload, created_at, published_at = null)

a relay, separately and repeatedly:
    select * from outbox where published_at is null order by created_at
    publish each to the broker
    mark it published (or delete it)
```

The event is stored atomically with the change. The relay may publish an event twice (crash after
publishing, before marking), which is fine because consumers are idempotent. Relays are a polling
job, or **change data capture** (Debezium reading the database's log). Spring Modulith's event
publication registry is an outbox built in.

## Retries, backoff and dead letters

A message fails to process. Was it **transient** (database down, timeout) or **permanent** (a bug,
invalid data)?

- Retry transient failures a few times with **exponential backoff** (1 s, 2 s, 4 s...).
- After N attempts, move the message to a **dead-letter queue** (DLQ) instead of retrying forever: a
  "poison message" retried in a loop blocks everything behind it.
- Alert on the DLQ, inspect, fix, and **replay** the messages when the cause is fixed.

## Ordering

- Brokers order messages only within a **partition** (Kafka) or a single queue with one consumer.
  Use a key (the order id) so all events of one entity go to the same partition and stay in order.
- Retries and parallel consumers break ordering; design consumers to tolerate it where you can
  (check a version or timestamp, ignore older updates).

## Events versus commands

- An **event** says something happened (`OrderPlaced`), in the past tense; the publisher does not
  know or care who listens.
- A **command** asks a specific service to do something (`ShipOrder`); it has one receiver.

Events give the loosest coupling. Put enough in the event for most consumers (ids plus the key
facts), and version the schema like an API.

## Summary

- Build on at-least-once delivery and make processing idempotent: effectively once.
- Deduplicate by message id in the same transaction as the effect, backed by a unique constraint.
- Avoid dual writes with a transactional outbox and a relay (or CDC); duplicates from the relay are
  handled by idempotent consumers.
- Retry transient failures with backoff, park poison messages in a DLQ, alert and replay.
- Order is per partition: key by entity id.
