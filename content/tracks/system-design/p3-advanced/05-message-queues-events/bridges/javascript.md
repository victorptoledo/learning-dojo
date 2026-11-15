---
from: javascript
status: draft
concept: "Talking through messages"
from_way: "EventEmitter inside one process, or BullMQ jobs on Redis."
native_way: "Durable queues and logs between processes and services, with keys for ordering and consumer groups for scaling."
sensei_tip: "An EventEmitter loses every event when the process dies and has no replay; a Kafka topic keeps them for days. KafkaJS's eachMessage gives you partition and offset: log them, they are your debugging coordinates."
---

| JavaScript | Messaging concept |
| --- | --- |
| `EventEmitter` | in-process pub/sub, not durable |
| BullMQ `Queue.add(name, data, { attempts, backoff })` | a job queue with retries |
| KafkaJS `producer.send({ topic, messages: [{ key, value }] })` | keyed records |
| KafkaJS `consumer({ groupId: 'billing' })` | a consumer group |
| `@aws-sdk/client-sqs` with a redrive policy | a queue with a dead-letter queue |
