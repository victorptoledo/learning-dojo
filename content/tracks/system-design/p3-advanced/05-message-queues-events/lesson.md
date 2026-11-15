# Messaging, Apache Kafka and Event Patterns

Asynchronous messaging decouples who produces work from who does it: in time (the consumer can
be down), in rate (the queue absorbs bursts) and in knowledge (the producer does not know who
listens). The Backend track covered delivery guarantees, the outbox and idempotent consumers;
this lesson is about the architecture: queues versus logs, partitions and ordering, sizing,
and the patterns built on events.

## Queues and logs

| | Message queue (RabbitMQ, SQS, ActiveMQ) | Distributed log (Kafka, Pulsar, Kinesis) |
| --- | --- | --- |
| A message is | a task, removed when acknowledged | a record appended to a partition, kept for the retention period |
| Consumers | compete: each message goes to one consumer | each consumer group reads the whole log at its own **offset** |
| Replay | no (once acknowledged it is gone) | yes: reset the offset and read again |
| Ordering | per queue, broken by competing consumers and redelivery | strict **per partition** |
| Routing | rich (exchanges, topics, headers, priorities, delays) | by partition key |
| Typical use | background jobs, work distribution | event streams, CDC, integration between many services, analytics |

Both are valid; many systems use a log for events between services and a queue for jobs
inside one service.

## Kafka's model in one picture

```text
topic "orders" (6 partitions, replication factor 3)
  producer: partition = hash(order_id) mod 6   → all events of one order are in order
  p0 [0 1 2 3 4 ...]  p1 [0 1 2 ...]  ...  p5 [0 1 ...]
       ▲                                       consumer group "billing":   3 consumers, 2 partitions each
       └─ offsets committed per group          consumer group "analytics": its own offsets, reads everything
```

- **Partitions** are the unit of parallelism and ordering. A group can have at most as many
  active consumers as partitions.
- **Keys** decide the partition, so choose the key that defines the order you need (per order,
  per account). A hot key makes one partition hot.
- **Replication**: each partition has a leader and followers; `acks=all` with
  `min.insync.replicas=2` means an acknowledged write survives the loss of one broker.
- **Retention** by time or size; **log compaction** keeps only the latest record per key, which
  turns a topic into a replicated table (the current state of every account).
- **Rebalancing**: when consumers join or leave, partitions are reassigned; processing pauses
  briefly and uncommitted work is redone, so consumers must be idempotent.

## Sizing a topic

```text
target: 50,000 events/s at peak, 1 KB each → 50 MB/s in
one consumer processes 2,000 events/s     → at least 25 consumers → at least 25 partitions
headroom for growth and rebalances         → 48 or 64 partitions
retention 7 days: 50 MB/s × 604,800 s × 3 replicas ≈ 90 TB of broker disk
```

Partitions are cheap to have and expensive to change (adding them changes `hash mod P`, breaking
per-key order for a while), so choose generously up front.

## Delivery and processing, briefly

At-least-once delivery plus an **idempotent** consumer (a processed-ids table, or upserts keyed
by event id) is the practical default. Kafka's "exactly-once" (idempotent producer plus
transactions) covers read-process-write loops **inside Kafka**; side effects outside it (an
email, a call to a payment provider) still need idempotency keys. Failed messages go to a
**dead-letter queue** after a few retries with backoff, with an alert and a way to redrive them.

## Event patterns

| Pattern | The event carries | Consumers | Trade-off |
| --- | --- | --- | --- |
| **Event notification** | "order 42 shipped" (an id) | call back the producer for details | small events, but coupling and load on the producer |
| **Event-carried state transfer** | the full new state of the order | keep their own copy, no call back | bigger events, duplicated data, autonomy |
| **Event sourcing** | every change, as the source of truth | rebuild state by replaying | full history and audit, harder queries and schema evolution |
| **CQRS projections** | changes from the write side | build read models (previous phase) | lag between write and read |

Events are facts in the past tense (`OrderPlaced`), owned by the producer; **commands**
(`ShipOrder`) are requests to one owner. Mixing them up leads to services that "listen" to
commands meant for someone else.

## Coordinating work across services

A business process spanning services (order → payment → stock → shipping) either uses
**choreography** (each service reacts to the previous one's events: loose coupling, but the flow
is implicit and hard to see) or **orchestration** (a workflow engine or a saga orchestrator
sends commands and tracks state: explicit, easier to monitor and change). Either way, failures
are handled with **compensating actions** (refund, release stock), not distributed transactions.

## Schemas and evolution

Producers and consumers deploy independently, so event formats must evolve without breaking
anyone: use a schema (Avro, Protobuf or JSON Schema) with a **schema registry** that checks
compatibility, add only optional fields, never reuse or retype a field, and version topics for
breaking changes (`orders.v2`).

## Stream processing

Consumers that compute over the stream (counts per minute, joins between streams, fraud scores)
are stream processors (Kafka Streams, Flink). Two concepts come up immediately: **windows**
(tumbling, sliding, session) and **event time versus processing time**, with **watermarks**
deciding when a window is complete despite late events.

## Summary

- Queues distribute tasks to competing consumers; logs keep ordered, replayable streams per
  partition for many independent consumer groups.
- The partition key sets ordering and parallelism; size partitions for peak throughput with
  headroom; keys can create hot partitions.
- At-least-once plus idempotent consumers is the default; exactly-once stops at Kafka's edge.
- Choose the event style deliberately: notification, state transfer, event sourcing,
  projections.
- Orchestrate or choreograph long processes with compensations; evolve schemas compatibly.

## Further reading

- Apache Kafka documentation (design, replication, semantics): <https://kafka.apache.org/documentation/>
- RabbitMQ documentation, "Reliability Guide": <https://www.rabbitmq.com/docs/reliability>
- Martin Kleppmann, *Designing Data-Intensive Applications*, chapter 11 (stream processing): <https://dataintensive.net/>
- Azure Architecture Center, cloud design patterns (competing consumers, pub/sub, choreography): <https://learn.microsoft.com/en-us/azure/architecture/patterns/>
- Amazon Builders' Library, "Avoiding insurmountable queue backlogs": <https://aws.amazon.com/builders-library/avoiding-insurmountable-queue-backlogs/>
