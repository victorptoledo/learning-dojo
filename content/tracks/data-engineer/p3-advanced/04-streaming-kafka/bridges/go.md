---
from: go
status: draft
concept: "Streams of events"
from_way: "Channels and goroutines passing events in memory."
native_way: "A durable partitioned log, consumer groups, offsets committed after processing, and windowed aggregation by event time."
sensei_tip: "A channel loses everything on restart and has no replay; with franz-go or segmentio/kafka-go, CommitRecords after your write gives at-least-once, so the write must tolerate duplicates."
---

| Go | Streaming |
| --- | --- |
| chan Event | in-memory streaming, no durability |
| kafka-go Reader with GroupID | a consumer group |
| reader.CommitMessages after the write | at-least-once |
| kafka.Message{Key: ...} | keyed records for ordering |
| map[windowKey]*Acc | an open-window state store |
