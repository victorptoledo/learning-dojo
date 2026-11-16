---
from: go
status: draft
concept: "Distributed in-memory computing"
from_way: "A worker pool of goroutines over a slice that fits in memory."
native_way: "Spark partitions the data across executors, plans lazily and shuffles by key for joins and aggregations."
sensei_tip: "A goroutine fan-out over chunks is the narrow part; the shuffle is like re-sending every item to a channel chosen by hash(key) % n, which is exactly where skew hurts: one channel gets the hot key."
---

| Go | Apache Spark |
| --- | --- |
| worker pool over chunks | tasks over partitions |
| hash(key) % n channel routing | a shuffle |
| map built once, read by all workers | a broadcast table |
| one channel with most items | data skew |
| random suffix on a hot key | salting |
