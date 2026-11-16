# Streaming with Kafka: Topics, Offsets, Delivery and Windows

Batch pipelines answer "what happened yesterday". Some consumers need "what is happening
now": fraud scoring, live operations, user-facing counters. **Apache Kafka** is the most
common backbone for that, and its ideas (a durable, partitioned log) appear in Amazon
Kinesis, Azure Event Hubs, Redpanda and Pulsar too.

## A distributed log

- A **topic** is a named stream of records (`payments`). Records are key, value, timestamp
  and headers.
- A topic is split into **partitions**. Each partition is an **append-only log**: records
  get increasing **offsets** (0, 1, 2...) and are kept for a **retention** period (days, or
  forever), whether or not anyone read them.
- The **key** decides the partition (a hash of the key). All records with the same key go
  to the same partition, so they stay **in order**. There is no order across partitions.
- Partitions are **replicated** to several brokers; producers choose durability with `acks`
  (`acks=all` waits for the in-sync replicas).

The number of partitions caps the parallelism of consumers, and changing it later moves
keys to different partitions, so choose it with some headroom.

## Consumers and offsets

Consumers in the same **consumer group** share the partitions of a topic: each partition is
read by one member, and when members join or leave the group **rebalances**. Each group
stores its position per partition by **committing offsets** (the offset of the **next**
record to read). Different groups read the same topic independently: the log is replayable.

When you commit decides what a crash does:

| Commit | Crash between processing and commit | Guarantee |
| --- | --- | --- |
| before processing | records are skipped | at most once |
| after processing | records are processed again | **at least once** |
| with the output, atomically | neither | effectively once |

At least once is the normal choice, so consumers must handle **redelivery**:
deduplicate (per-partition offsets, or an event id), or make the output **idempotent**
(upsert by key). Kafka's **idempotent producer** and **transactions** give exactly-once
from topic to topic (Kafka Streams, Flink use them); to an external database you need
idempotent writes or to store offsets in the same transaction as the results.

**Consumer lag** (latest offset minus committed offset) is the key health metric of a
streaming pipeline.

## Retention and compaction

- **Time or size retention** deletes old segments: good for event streams.
- **Log compaction** keeps at least the **latest record per key** and drops older ones:
  a topic becomes a changelog of current state (CDC topics, configuration). A record with a
  null value (a **tombstone**) deletes the key.

## Stream processing: time and windows

Counting "payments per minute" on an endless stream needs **windows**:

- **Tumbling**: fixed, non-overlapping (`[12:00, 12:01)`, `[12:01, 12:02)`).
- **Hopping / sliding**: fixed size, overlapping (5 minutes, every minute).
- **Session**: per key, closed after a gap of inactivity.

Which minute does an event belong to? **Event time** (when it happened, in the record) gives
correct answers; **processing time** (when you saw it) is simpler but changes with delays and
replays. With event time, events arrive **out of order**, so the processor needs a
**watermark**: "I do not expect events older than T any more", usually the largest event
time seen minus an allowed lateness. A window is emitted when the watermark passes its end.
Events that arrive after that are **late**: drop them, send them to a side output, or
update the result. More lateness means more correct and more delayed results.

Engines that do this for you: **Kafka Streams** (a Java library), **Apache Flink**, **Spark
Structured Streaming**, and SQL-on-streams systems such as ksqlDB, RisingWave and
Materialize.

## Summary

- Kafka topics are partitioned, replicated, retained logs; keys keep per-key order.
- Consumer groups share partitions and commit offsets; committing after processing gives
  at-least-once, so outputs must be idempotent or deduplicated.
- Compaction keeps the latest value per key; retention keeps a time window of history.
- Window by event time, close windows with a watermark, and choose a late-data policy.
- Watch consumer lag.

## Further reading

- Apache Kafka documentation, design and concepts: <https://kafka.apache.org/documentation/#design>
- Confluent, "Kafka consumer design: consumers, consumer groups and offsets": <https://docs.confluent.io/platform/current/clients/consumer.html>
- Tyler Akidau, Slava Chernyak and Reuven Lax, *Streaming Systems* (O'Reilly, 2018), chapters 1 to 3 (event time, windows, watermarks)
- Apache Flink documentation, timely stream processing: <https://nightlies.apache.org/flink/flink-docs-stable/docs/concepts/time/>
- Jay Kreps, "The Log: What every software engineer should know about real-time data's unifying abstraction": <https://engineering.linkedin.com/distributed-systems/log-what-every-software-engineer-should-know-about-real-time-datas-unifying>
