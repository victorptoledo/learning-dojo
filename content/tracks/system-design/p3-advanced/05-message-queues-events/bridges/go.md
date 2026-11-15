---
from: go
status: draft
concept: "Talking through messages"
from_way: "Channels between goroutines in one process."
native_way: "Durable queues and logs between processes: partitions are like channels that survive restarts and can be replayed by many readers."
sensei_tip: "A buffered channel is a queue with competing consumers that vanishes on exit. franz-go and segmentio/kafka-go give you the durable version; set the record Key to keep per-entity ordering."
---

| Go | Messaging concept |
| --- | --- |
| `chan Event` | an in-memory queue |
| `kgo.Record{Topic: "orders", Key: []byte(id)}` (franz-go) | keyed records |
| `kgo.ConsumerGroup("billing")` | a consumer group |
| NATS JetStream durable consumers | a log with acknowledgements |
| `context` cancellation in a consumer loop | clean shutdown before rebalancing |
