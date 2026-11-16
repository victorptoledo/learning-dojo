---
from: python
status: draft
concept: "Streams of events"
from_way: "A loop over consumer.poll() that writes each message and lets the client auto-commit."
native_way: "Commit offsets after the results are written, deduplicate redeliveries, and window by event time."
sensei_tip: "With confluent-kafka, set enable.auto.commit=False and call consumer.commit(asynchronous=False) after your write; Faust-style or Bytewax libraries and PyFlink add windows and watermarks."
---

| Python | Streaming |
| --- | --- |
| confluent_kafka.Consumer({'group.id': ...}) | a consumer group |
| consumer.commit() after the write | at-least-once |
| producer.produce(topic, key=..., value=...) | keyed records |
| msg.offset(), msg.partition() | position for deduplication |
| PyFlink / Bytewax windows | event-time aggregation |
