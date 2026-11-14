---
from: java
status: draft
concept: "Queues vs event logs"
from_way: "Services calling each other synchronously over HTTP in long chains."
native_way: "Event-driven: services publish events to partitioned topics; consumers read at their own pace without overloading the producer."
sensei_tip: "End-to-end exactly-once delivery is expensive and rare. The industry standard is at-least-once delivery plus idempotent consumers."
---
