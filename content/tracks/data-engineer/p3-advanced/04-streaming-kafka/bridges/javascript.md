---
from: javascript
status: draft
concept: "Streams of events"
from_way: "WebSocket or EventEmitter streams processed in arrival order in memory."
native_way: "A durable, partitioned log that consumers replay from committed offsets."
sensei_tip: "Unlike an EventEmitter, a Kafka topic remembers: a new consumer group can read last week's events, which is how you rebuild a projection after a bug."
---

| JavaScript / TypeScript | Streaming |
| --- | --- |
| EventEmitter | in-memory events, no replay |
| kafkajs consumer.run({ eachMessage }) | a consumer in a group |
| autoCommit: false + commitOffsets | committing after processing |
| RxJS bufferTime(60000) | processing-time windows |
| event.timestamp from the payload | event time |
