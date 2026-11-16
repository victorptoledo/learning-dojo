# Event-Driven Architecture

"Event-driven" means four different things, and teams that use the word without saying which
one end up with designs that satisfy none of them. This lesson sorts them out and solves the one
problem all of them share: publishing an event **reliably**.

## Events and commands

- A **command** asks for something to happen: `ShipOrder`. It has one intended receiver, may be
  refused, and the sender cares about the outcome.
- An **event** states that something **has** happened: `OrderPlaced`. It is a fact in the past
  tense, the publisher does not know or care who listens, and nobody can refuse it.

The difference is a dependency direction. With a command, the sender depends on the receiver;
with an event, the receivers depend on the publisher's event contract and the publisher depends
on nobody. That is why events decouple, and why they are harder to follow: the flow is no longer
written down in one place.

## Fowler's four patterns

Martin Fowler's "What do you mean by Event-Driven?" distinguishes:

1. **Event notification**: the event says "order 42 was placed", with little data. Consumers who
   need more call back. Very loose coupling of the publisher; the call-backs add load and
   coupling the other way.
2. **Event-carried state transfer**: the event carries the data consumers need (lines, address,
   totals), so they keep their own copy and never call back. More autonomy and availability for
   consumers; bigger events, duplicated data and a larger contract to evolve.
3. **Event sourcing**: the system of record **is** the sequence of events; current state is
   derived by replaying them. A complete audit trail and the ability to rebuild any view, paid for
   with complexity: event schema versioning, snapshots, and queries that need projections.
4. **CQRS** (command query responsibility segregation): separate models for writing and for
   reading. Commands go to a model built to enforce invariants (aggregates); queries go to read
   models shaped for each screen, often updated from events. It pays off when reads and writes
   have very different shapes or loads; for a simple CRUD screen it is extra machinery.

They combine (CQRS is often fed by events, sometimes by event sourcing) but none implies the
others. Pick per problem and write down which one you mean.

## Read models and eventual consistency

A **read model** (or projection) is a denormalised view built from events: "open orders per
customer", "best sellers this week". It is fast and simple to query, and always a little behind:
the user who just placed an order may not see it in the list for a second. That is **eventual
consistency**, and it is a product decision as much as a technical one: show "processing", read
your own writes from the write model, or accept the lag. The System Design track's lesson on
read-heavy designs covers fan-out and denormalisation at scale.

## Choreography and orchestration

When a business process spans several services:

- **Choreography**: each service reacts to events and emits its own (orders → `OrderPlaced` →
  payments → `PaymentTaken` → shipping). No central coordinator, very loose coupling, and the
  process exists only implicitly, spread over many services: hard to see, change and monitor.
- **Orchestration**: a coordinator (an orchestrator or process manager) sends commands and waits
  for replies. The process is explicit in one place and easier to change and monitor; the
  orchestrator knows every participant.

Choreography suits a few steps owned by different teams; orchestration suits long processes with
compensations, which the next lesson's sagas need.

## The dual-write problem

"Save the order, then publish `OrderPlaced`" writes to two systems, a database and a broker, with
no transaction spanning both:

- crash after the commit and before the publish: the order exists, the event is lost forever;
- publish inside the transaction and then roll back: consumers act on an order that never existed;
- broker down: either the order fails, or the event is lost.

### The transactional outbox

Write the event into an **outbox table** in the **same local transaction** as the state change.
Either both are committed or neither is. A separate **relay** (a poller, or change data capture
with Debezium reading the database log) then publishes outbox rows to the broker and marks them
sent. If it crashes between publishing and marking, it publishes again on restart: delivery is
**at least once**, never zero. The *Outbox and Projection* kata builds both pieces.

### Idempotent consumers

At-least-once delivery means **duplicates**. Exactly-once delivery across a network is, in
general, not available; what you can build is **exactly-once processing**: make each consumer
**idempotent**. Two common ways:

- record the ids of processed events (in the same transaction as the consumer's own change) and
  skip the ones already seen;
- make the update naturally idempotent: "set status to shipped" rather than "add one to the
  shipped count".

Ordering is the other trap: most brokers order only within a partition, so key partitions by
aggregate id when the order of one aggregate's events matters.

## Further reading

- Martin Fowler, "What do you mean by 'Event-Driven'?" (2017): <https://martinfowler.com/articles/201701/event-driven.html>
- Martin Fowler, "CQRS": <https://martinfowler.com/bliki/CQRS.html>
- Martin Fowler, "Event Sourcing": <https://martinfowler.com/eaaDev/EventSourcing.html>
- Chris Richardson, "Pattern: Transactional outbox": <https://microservices.io/patterns/data/transactional-outbox.html>
- Chris Richardson, "Pattern: Idempotent Consumer": <https://microservices.io/patterns/communication-style/idempotent-consumer.html>
- Debezium documentation, "Outbox Event Router": <https://debezium.io/documentation/reference/stable/transformations/outbox-event-router.html>
- Martin Kleppmann, *Designing Data-Intensive Applications* (O'Reilly, 2017), chapter 11, "Stream Processing"
