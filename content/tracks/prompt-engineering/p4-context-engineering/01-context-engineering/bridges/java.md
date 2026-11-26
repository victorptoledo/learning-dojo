---
from: java
status: draft
concept: "Session state that must stay small"
from_way: "HTTP sessions kept small, caches such as Caffeine with size-based eviction, and event sourcing with snapshots."
native_way: "An agent's history is an event log sent on every call: snapshot old events into a summary (compaction), evict old payloads (clearing tool results) and keep durable facts in a separate store (notes)."
sensei_tip: "Event sourcing is the closest model: the summary is a snapshot, the recent turns are the events after it, and you never drop half of an event, just as you never split a tool call from its result."
---

| Java | Context-engineering concept |
| --- | --- |
| Caffeine `maximumSize` eviction | clearing old tool results |
| an event-sourcing snapshot | a compaction summary |
| events after the snapshot | recent turns kept verbatim |
| a separate read model or table | notes and memory outside the window |
| a task submitted to an `ExecutorService` | a sub-agent with its own context |
