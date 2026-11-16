---
from: csharp
status: draft
concept: "Streams of events"
from_way: "Azure Service Bus queues where each message is completed after handling."
native_way: "Kafka or Event Hubs partitions with offsets and checkpoints, and event-time windows in Stream Analytics or Flink."
sensei_tip: "Event Hubs speaks the Kafka protocol; its checkpoints are committed offsets, and Azure Stream Analytics' TumblingWindow(minute, 1) with event ordering policies is event-time windowing in SQL."
---

| C# | Streaming |
| --- | --- |
| Service Bus queue | a queue: messages removed when completed |
| Confluent.Kafka IConsumer | a consumer in a group |
| consumer.Commit(result) | committing an offset |
| Event Hubs checkpoint | a stored position |
| Stream Analytics TumblingWindow | tumbling windows |
