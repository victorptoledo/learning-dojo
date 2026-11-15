---
from: java
status: draft
concept: "Talking through messages"
from_way: "Calling another service synchronously with a REST client and waiting for the answer."
native_way: "Publishing facts to a log or tasks to a queue; consumers in groups process them at their own pace, ordered per partition key, idempotently."
sensei_tip: "With Spring for Apache Kafka, the record key you pass to KafkaTemplate.send(topic, key, value) is the ordering decision of this lesson. Leave it null and Kafka spreads records with no per-entity order."
---

| Java / Spring | Messaging concept |
| --- | --- |
| `kafkaTemplate.send("orders", orderId, event)` | partition key = order id |
| `@KafkaListener(groupId = "billing", concurrency = "6")` | a consumer group with 6 consumers |
| `DefaultErrorHandler` + `DeadLetterPublishingRecoverer` | retries and a dead-letter topic |
| `@RabbitListener` with manual ack | a competing-consumer queue |
| Kafka Streams `groupByKey().windowedBy(...)` | stream processing with windows |
