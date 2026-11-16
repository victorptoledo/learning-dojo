---
from: java
status: draft
concept: "Streams of events"
from_way: "Spring @KafkaListener methods that handle one message at a time, with auto-commit."
native_way: "Partitioned topics with keys for ordering, offsets committed after processing, and event-time windows in Kafka Streams or Flink."
sensei_tip: "Kafka Streams and Flink are Java-first: TimeWindows.ofSizeAndGrace(Duration.ofMinutes(1), Duration.ofSeconds(10)) is a tumbling window with allowed lateness, and processing.guarantee=exactly_once_v2 uses transactions."
---

| Java | Streaming |
| --- | --- |
| @KafkaListener | a consumer in a group |
| AckMode.MANUAL + ack.acknowledge() | committing after processing |
| ProducerRecord(topic, key, value) | keyed records for ordering |
| TimeWindows.ofSizeAndGrace | tumbling windows with lateness |
| enable.idempotence=true | an idempotent producer |
