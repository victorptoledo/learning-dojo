---
from: go
status: draft
concept: "Moving data, not serving requests"
from_way: "Services that answer requests with small reads and writes and keep latency low."
native_way: "Jobs and streams that move large volumes between systems, where throughput and correctness on reruns matter most."
sensei_tip: "Go is common in data infrastructure (ingestion agents, Kafka tooling, CLI tools), but pipelines themselves are mostly SQL and Python; your concurrency instincts help with throughput and backpressure."
---

| Go | Data engineering |
| --- | --- |
| an HTTP handler | a pipeline task |
| database/sql queries per request | bulk extracts and set-based loads |
| goroutines and channels | parallel partitions and streams |
| a cron-triggered binary | an orchestrated job with retries |
| encoding/json | JSON lines, Avro or Parquet |
